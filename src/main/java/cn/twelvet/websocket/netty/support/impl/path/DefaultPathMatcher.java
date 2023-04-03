package cn.twelvet.websocket.netty.support.impl.path;

import cn.twelvet.websocket.netty.support.WsPathMatcher;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.QueryStringDecoder;

/**
 * @author twelvet
 * @WebSite www.twelvet.cn
 * @Description: Default path matcher
 */
public class DefaultPathMatcher implements WsPathMatcher {

    private final String pattern;

    public DefaultPathMatcher(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String getPattern() {
        return this.pattern;
    }

    @Override
    public boolean matchAndExtract(QueryStringDecoder decoder, Channel channel) {
        return pattern.equals(decoder.path());
    }
}
