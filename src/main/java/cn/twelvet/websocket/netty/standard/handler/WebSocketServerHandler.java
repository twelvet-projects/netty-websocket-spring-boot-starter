package cn.twelvet.websocket.netty.standard.handler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import cn.twelvet.websocket.netty.domain.WebSocketEndpointServer;

/**
 * @author twelvet
 * @WebSite www.twelvet.cn
 * @Description: Handler WebSocketFrame
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final WebSocketEndpointServer webSocketEndpointServer;

    public WebSocketServerHandler(WebSocketEndpointServer webSocketEndpointServer) {
        this.webSocketEndpointServer = webSocketEndpointServer;
    }

    /**
     * Receive message
     *
     * @param ctx ChannelHandlerContext
     * @param msg FullHttpRequest
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception {
        handleWebSocketFrame(ctx, msg);
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
    }

    /**
     * heartbeat
     *
     * @param ctx ChannelHandlerContext
     * @param evt Object
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        webSocketEndpointServer.doOnEvent(ctx.channel(), evt);
    }

    /**
     * Processing different types of messages
     *
     * @param ctx   ChannelHandlerContext
     * @param frame WebSocketFrame
     */
    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            webSocketEndpointServer.doOnMessage(ctx.channel(), frame);
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if (frame instanceof CloseWebSocketFrame) {
            ctx.writeAndFlush(frame.retainedDuplicate()).addListener(ChannelFutureListener.CLOSE);
            return;
        }
        if (frame instanceof BinaryWebSocketFrame) {
            webSocketEndpointServer.doOnBinary(ctx.channel(), frame);
            return;
        }
        if (frame instanceof PongWebSocketFrame) {
            return;
        }
    }

}
