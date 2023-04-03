package cn.twelvet.websocket.netty.support;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.QueryStringDecoder;

/**
 * @author twelvet
 * @WebSite www.twelvet.cn
 * @Description: WebSocket path matcher
 */
public interface WsPathMatcher {

    String getPattern();

    boolean matchAndExtract(QueryStringDecoder decoder, Channel channel);
}
