package cn.twelvet.websocket.netty.domain;

import cn.twelvet.websocket.netty.support.impl.path.AntPathMatcherWrapper;
import cn.twelvet.websocket.netty.support.impl.path.DefaultPathMatcher;
import cn.twelvet.websocket.netty.support.impl.PathVariableMapMethodArgumentResolver;
import cn.twelvet.websocket.netty.support.impl.PathVariableMethodArgumentResolver;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.TypeMismatchException;
import cn.twelvet.websocket.netty.standard.WebSocketEndpointConfig;
import cn.twelvet.websocket.netty.support.*;

import java.lang.reflect.Method;
import java.util.*;

/**
 * @author twelvet
 * Event handling service
 */
public class WebSocketEndpointServer {

    private static final AttributeKey<Object> WEB_SOCKET_KEY = AttributeKey.valueOf("WEBSOCKET_IMPLEMENT");

    public static final AttributeKey<NettySession> SESSION_KEY = AttributeKey.valueOf("WEBSOCKET_SESSION");

    private static final AttributeKey<String> PATH_KEY = AttributeKey.valueOf("WEBSOCKET_PATH");

    public static final AttributeKey<Map<String, String>> URI_TEMPLATE = AttributeKey.valueOf("WEBSOCKET_URI_TEMPLATE");

    public static final AttributeKey<Map<String, List<String>>> REQUEST_PARAM = AttributeKey.valueOf("WEBSOCKET_REQUEST_PARAM");

    private final Map<String, WebSocketMethodMapping> pathMethodMappingMap = new HashMap<>();

    private final WebSocketEndpointConfig config;

    private final Set<WsPathMatcher> pathMatchers = new HashSet<>();

    private static final InternalLogger log = InternalLoggerFactory.getInstance(WebSocketEndpointServer.class);

    public WebSocketEndpointServer(WebSocketMethodMapping methodMapping, WebSocketEndpointConfig config, String path) {
        addPathMethodMapping(path, methodMapping);
        this.config = config;
    }

    public boolean hasBeforeHandshake(Channel channel, String path) {
        WebSocketMethodMapping methodMapping = getWebSocketMethodMapping(path, channel);
        return methodMapping.getBeforeHandshake() != null;
    }

    public void doBeforeHandshake(Channel channel, FullHttpRequest req, String path) {
        WebSocketMethodMapping methodMapping = getWebSocketMethodMapping(path, channel);

        Object implement = null;
        try {
            implement = methodMapping.getEndpointInstance();
        } catch (Exception e) {
            log.error(e);
            return;
        }
        channel.attr(WEB_SOCKET_KEY).set(implement);
        NettySession nettySession = new NettySession(channel);
        channel.attr(SESSION_KEY).set(nettySession);
        Method beforeHandshake = methodMapping.getBeforeHandshake();
        if (beforeHandshake != null) {
            try {
                beforeHandshake.invoke(implement, methodMapping.getBeforeHandshakeArgs(channel, req));
            } catch (TypeMismatchException e) {
                throw e;
            } catch (Throwable t) {
                log.error(t);
            }
        }
    }

    public void doOnOpen(Channel channel, FullHttpRequest req, String path) {
        WebSocketMethodMapping methodMapping = getWebSocketMethodMapping(path, channel);

        Object implement = channel.attr(WEB_SOCKET_KEY).get();
        if (implement == null) {
            try {
                implement = methodMapping.getEndpointInstance();
                channel.attr(WEB_SOCKET_KEY).set(implement);
            } catch (Exception e) {
                log.error(e);
                return;
            }
            NettySession nettySession = new NettySession(channel);
            channel.attr(SESSION_KEY).set(nettySession);
        }

        Method onOpenMethod = methodMapping.getOnOpen();
        if (onOpenMethod != null) {
            try {
                onOpenMethod.invoke(implement, methodMapping.getOnOpenArgs(channel, req));
            } catch (TypeMismatchException e) {
                throw e;
            } catch (Throwable t) {
                log.error(t);
            }
        }
    }

    public void doOnClose(Channel channel) {
        Attribute<String> attrPath = channel.attr(PATH_KEY);
        WebSocketMethodMapping methodMapping = null;
        if (pathMethodMappingMap.size() == 1) {
            methodMapping = pathMethodMappingMap.values().iterator().next();
        } else {
            String path = attrPath.get();
            methodMapping = pathMethodMappingMap.get(path);
            if (methodMapping == null) {
                return;
            }
        }
        if (methodMapping.getOnClose() != null) {
            if (!channel.hasAttr(SESSION_KEY)) {
                return;
            }
            Object implement = channel.attr(WEB_SOCKET_KEY).get();
            try {
                methodMapping.getOnClose().invoke(implement,
                        methodMapping.getOnCloseArgs(channel));
            } catch (Throwable t) {
                log.error(t);
            }
        }
    }


    public void doOnError(Channel channel, Throwable throwable) {
        WebSocketMethodMapping webSocketMethodMapping = getWebSocketMethodMapping(channel);
        if (webSocketMethodMapping.getOnError() != null) {
            if (!channel.hasAttr(SESSION_KEY)) {
                return;
            }
            Object implement = channel.attr(WEB_SOCKET_KEY).get();
            try {
                Method method = webSocketMethodMapping.getOnError();
                Object[] args = webSocketMethodMapping.getOnErrorArgs(channel, throwable);
                method.invoke(implement, args);
            } catch (Throwable t) {
                log.error(t);
            }
        }
    }

    public void doOnMessage(Channel channel, WebSocketFrame frame) {
        WebSocketMethodMapping webSocketMethodMapping = getWebSocketMethodMapping(channel);
        if (webSocketMethodMapping.getOnMessage() != null) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            Object implement = channel.attr(WEB_SOCKET_KEY).get();
            try {
                webSocketMethodMapping.getOnMessage().invoke(implement, webSocketMethodMapping.getOnMessageArgs(channel, textFrame));
            } catch (Throwable t) {
                log.error(t);
            }
        }
    }

    public void doOnBinary(Channel channel, WebSocketFrame frame) {
        WebSocketMethodMapping webSocketMethodMapping = getWebSocketMethodMapping(channel);
        if (webSocketMethodMapping.getOnBinary() != null) {
            BinaryWebSocketFrame binaryWebSocketFrame = (BinaryWebSocketFrame) frame;
            Object implement = channel.attr(WEB_SOCKET_KEY).get();
            try {
                webSocketMethodMapping.getOnBinary().invoke(implement, webSocketMethodMapping.getOnBinaryArgs(channel, binaryWebSocketFrame));
            } catch (Throwable t) {
                log.error(t);
            }
        }
    }

    public void doOnEvent(Channel channel, Object evt) {
        WebSocketMethodMapping webSocketMethodMapping = getWebSocketMethodMapping(channel);
        if (webSocketMethodMapping.getOnEvent() != null) {
            if (!channel.hasAttr(SESSION_KEY)) {
                return;
            }
            Object implement = channel.attr(WEB_SOCKET_KEY).get();
            try {
                webSocketMethodMapping.getOnEvent().invoke(implement, webSocketMethodMapping.getOnEventArgs(channel, evt));
            } catch (Throwable t) {
                log.error(t);
            }
        }
    }

    public String getHost() {
        return config.getHost();
    }

    public int getPort() {
        return config.getPort();
    }

    public Set<WsPathMatcher> getPathMatcherSet() {
        return pathMatchers;
    }

    public void addPathMethodMapping(String path, WebSocketMethodMapping webSocketMethodMapping) {
        pathMethodMappingMap.put(path, webSocketMethodMapping);
        for (MethodArgumentResolver onOpenArgResolver : webSocketMethodMapping.getOnOpenArgResolvers()) {
            if (onOpenArgResolver instanceof PathVariableMethodArgumentResolver || onOpenArgResolver instanceof PathVariableMapMethodArgumentResolver) {
                pathMatchers.add(new AntPathMatcherWrapper(path));
                return;
            }
        }
        pathMatchers.add(new DefaultPathMatcher(path));
    }

    private WebSocketMethodMapping getWebSocketMethodMapping(Channel channel) {
        Attribute<String> attrPath = channel.attr(PATH_KEY);
        if (pathMethodMappingMap.size() == 1) {
            return pathMethodMappingMap.values().iterator().next();
        } else {
            String path = attrPath.get();
            return pathMethodMappingMap.get(path);
        }
    }

    private WebSocketMethodMapping getWebSocketMethodMapping(String path, Channel channel) {
        WebSocketMethodMapping methodMapping;
        if (pathMethodMappingMap.size() == 1) {
            methodMapping = pathMethodMappingMap.values().iterator().next();
        } else {
            Attribute<String> attrPath = channel.attr(PATH_KEY);
            attrPath.set(path);
            methodMapping = pathMethodMappingMap.get(path);
            if (methodMapping == null) {
                throw new RuntimeException("path " + path + " is not in pathMethodMappingMap ");
            }
        }
        return methodMapping;
    }
}
