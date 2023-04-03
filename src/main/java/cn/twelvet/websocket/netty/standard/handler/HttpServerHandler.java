package cn.twelvet.websocket.netty.standard.handler;

import cn.twelvet.websocket.netty.standard.WebSocketEndpointConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.util.ObjectUtils;
import cn.twelvet.websocket.netty.domain.WebSocketEndpointServer;
import cn.twelvet.websocket.netty.support.WsPathMatcher;

import java.io.InputStream;
import java.util.Set;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author twelvet
 * @WebSite www.twelvet.cn
 * @Description: Process request information
 */
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(HttpServerHandler.class);

    private final WebSocketEndpointServer webSocketEndpointServer;
    private final WebSocketEndpointConfig webSocketEndpointConfig;
    private final EventExecutorGroup eventExecutorGroup;
    private final boolean isCors;

    private static ByteBuf faviconByteBuf = null;
    private static ByteBuf notFoundByteBuf = null;
    private static ByteBuf badRequestByteBuf = null;
    private static ByteBuf forbiddenByteBuf = null;
    private static ByteBuf internalServerErrorByteBuf = null;

    static {
        faviconByteBuf = buildStaticRes("/favicon.ico");
        notFoundByteBuf = buildStaticRes("/public/error/404.html");
        badRequestByteBuf = buildStaticRes("/public/error/400.html");
        forbiddenByteBuf = buildStaticRes("/public/error/403.html");
        internalServerErrorByteBuf = buildStaticRes("/public/error/500.html");
        if (notFoundByteBuf == null) {
            notFoundByteBuf = buildStaticRes("/public/error/4xx.html");
        }
        if (badRequestByteBuf == null) {
            badRequestByteBuf = buildStaticRes("/public/error/4xx.html");
        }
        if (forbiddenByteBuf == null) {
            forbiddenByteBuf = buildStaticRes("/public/error/4xx.html");
        }
        if (internalServerErrorByteBuf == null) {
            internalServerErrorByteBuf = buildStaticRes("/public/error/5xx.html");
        }
    }


    private static ByteBuf buildStaticRes(String resPath) {
        try {
            InputStream inputStream = HttpServerHandler.class.getResourceAsStream(resPath);
            if (inputStream != null) {
                int available = inputStream.available();
                if (available != 0) {
                    byte[] bytes = new byte[available];
                    inputStream.read(bytes);
                    return ByteBufAllocator.DEFAULT.buffer(bytes.length).writeBytes(bytes);
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public HttpServerHandler(WebSocketEndpointServer webSocketEndpointServer, WebSocketEndpointConfig webSocketEndpointConfig, EventExecutorGroup eventExecutorGroup, boolean isCors) {
        this.webSocketEndpointServer = webSocketEndpointServer;
        this.webSocketEndpointConfig = webSocketEndpointConfig;
        this.eventExecutorGroup = eventExecutorGroup;
        this.isCors = isCors;
    }

    /**
     * Receive message
     *
     * @param ctx ChannelHandlerContext
     * @param msg FullHttpRequest
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        try {
            handleHttpRequest(ctx, msg);
        } catch (TypeMismatchException e) {
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
            sendHttpResponse(ctx, msg, res);
            log.error(e);
        } catch (Exception e) {
            FullHttpResponse res;
            if (internalServerErrorByteBuf != null) {
                res = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR, internalServerErrorByteBuf.retainedDuplicate());
            } else {
                res = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
            }
            sendHttpResponse(ctx, msg, res);
            log.error(e);
        }
    }

    /**
     * Processing error
     *
     * @param ctx   ChannelHandlerContext
     * @param cause Throwable
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        webSocketEndpointServer.doOnError(ctx.channel(), cause);
    }

    /**
     * Close inactive connections
     *
     * @param ctx ChannelHandlerContext
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        webSocketEndpointServer.doOnClose(ctx.channel());
        super.channelInactive(ctx);
    }

    /**
     * Handle messages
     *
     * @param ctx ChannelHandlerContext
     * @param req FullHttpRequest
     */
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        FullHttpResponse res;
        // Handle a bad request.
        if (!req.decoderResult().isSuccess()) {
            if (badRequestByteBuf != null) {
                res = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, badRequestByteBuf.retainedDuplicate());
            } else {
                res = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
            }
            sendHttpResponse(ctx, req, res);
            return;
        }

        // Allow only GET methods.
        if (req.method() != GET) {
            if (forbiddenByteBuf != null) {
                res = new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN, forbiddenByteBuf.retainedDuplicate());
            } else {
                res = new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN);
            }
            sendHttpResponse(ctx, req, res);
            return;
        }

        HttpHeaders headers = req.headers();
        String host = headers.get(HttpHeaderNames.HOST);
        if (ObjectUtils.isEmpty(host)) {
            if (forbiddenByteBuf != null) {
                res = new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN, forbiddenByteBuf.retainedDuplicate());
            } else {
                res = new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN);
            }
            sendHttpResponse(ctx, req, res);
            return;
        }

        if (!ObjectUtils.isEmpty(webSocketEndpointServer.getHost()) && !webSocketEndpointServer.getHost().equals("0.0.0.0") && !webSocketEndpointServer.getHost().equals(host.split(":")[0])) {
            if (forbiddenByteBuf != null) {
                res = new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN, forbiddenByteBuf.retainedDuplicate());
            } else {
                res = new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN);
            }
            sendHttpResponse(ctx, req, res);
            return;
        }

        QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
        String path = decoder.path();
        if ("/favicon.ico".equals(path)) {
            if (faviconByteBuf != null) {
                res = new DefaultFullHttpResponse(HTTP_1_1, OK, faviconByteBuf.retainedDuplicate());
            } else {
                res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
            }
            sendHttpResponse(ctx, req, res);
            return;
        }

        Channel channel = ctx.channel();

        //path match
        String pattern = null;
        Set<WsPathMatcher> pathMatcherSet = webSocketEndpointServer.getPathMatcherSet();
        for (WsPathMatcher pathMatcher : pathMatcherSet) {
            if (pathMatcher.matchAndExtract(decoder, channel)) {
                pattern = pathMatcher.getPattern();
                break;
            }
        }

        if (pattern == null) {
            if (notFoundByteBuf != null) {
                res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND, notFoundByteBuf.retainedDuplicate());
            } else {
                res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
            }
            sendHttpResponse(ctx, req, res);
            return;
        }

        if (!req.headers().contains(UPGRADE) || !req.headers().contains(SEC_WEBSOCKET_KEY) || !req.headers().contains(SEC_WEBSOCKET_VERSION)) {
            if (forbiddenByteBuf != null) {
                res = new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN, forbiddenByteBuf.retainedDuplicate());
            } else {
                res = new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN);
            }
            sendHttpResponse(ctx, req, res);
            return;
        }

        String subprotocols = null;

        if (webSocketEndpointServer.hasBeforeHandshake(channel, pattern)) {
            webSocketEndpointServer.doBeforeHandshake(channel, req, pattern);
            if (!channel.isActive()) {
                return;
            }

            AttributeKey<String> subprotocolsAttrKey = AttributeKey.valueOf("subprotocols");
            if (channel.hasAttr(subprotocolsAttrKey)) {
                subprotocols = ctx.channel().attr(subprotocolsAttrKey).get();
            }
        }

        // Handshake
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), subprotocols, true, webSocketEndpointConfig.getMaxFramePayloadLength());
        WebSocketServerHandshaker webSocketServerHandshaker = wsFactory.newHandshaker(req);
        if (webSocketServerHandshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(channel);
        } else {
            ChannelPipeline pipeline = ctx.pipeline();
            pipeline.remove(ctx.name());
            if (webSocketEndpointConfig.getReaderIdleTimeSeconds() != 0 || webSocketEndpointConfig.getWriterIdleTimeSeconds() != 0 || webSocketEndpointConfig.getAllIdleTimeSeconds() != 0) {
                pipeline.addLast(new IdleStateHandler(webSocketEndpointConfig.getReaderIdleTimeSeconds(), webSocketEndpointConfig.getWriterIdleTimeSeconds(), webSocketEndpointConfig.getAllIdleTimeSeconds()));
            }
            if (webSocketEndpointConfig.isUseCompressionHandler()) {
                pipeline.addLast(new WebSocketServerCompressionHandler());
            }
            pipeline.addLast(new WebSocketFrameAggregator(Integer.MAX_VALUE));
            if (webSocketEndpointConfig.isUseEventExecutorGroup()) {
                pipeline.addLast(eventExecutorGroup, new WebSocketServerHandler(webSocketEndpointServer));
            } else {
                pipeline.addLast(new WebSocketServerHandler(webSocketEndpointServer));
            }
            String finalPattern = pattern;
            webSocketServerHandshaker.handshake(channel, req).addListener(future -> {
                if (future.isSuccess()) {
                    if (isCors) {
                        pipeline.remove(CorsHandler.class);
                    }
                    webSocketEndpointServer.doOnOpen(channel, req, finalPattern);
                } else {
                    webSocketServerHandshaker.close(channel, new CloseWebSocketFrame());
                }
            });
        }

    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        // Generate an error page if response getStatus code is not OK (200).
        int statusCode = res.status().code();
        if (statusCode != OK.code() && res.content().readableBytes() == 0) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        HttpUtil.setContentLength(res, res.content().readableBytes());

        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpUtil.isKeepAlive(req) || statusCode != OK.code()) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static String getWebSocketLocation(FullHttpRequest req) {
        String location = req.headers().get(HttpHeaderNames.HOST) + req.uri();
        return "ws://" + location;
    }
}