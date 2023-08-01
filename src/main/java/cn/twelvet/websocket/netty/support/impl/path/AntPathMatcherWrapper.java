package cn.twelvet.websocket.netty.support.impl.path;

import cn.twelvet.websocket.netty.support.WsPathMatcher;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.springframework.util.AntPathMatcher;

import java.util.LinkedHashMap;
import java.util.Map;

import static cn.twelvet.websocket.netty.domain.WebSocketEndpointServer.URI_TEMPLATE;

/**
 * @author twelvet Ant path matcher
 */
public class AntPathMatcherWrapper extends AntPathMatcher implements WsPathMatcher {

	private final String pattern;

	public AntPathMatcherWrapper(String pattern) {
		this.pattern = pattern;
	}

	@Override
	public String getPattern() {
		return this.pattern;
	}

	@Override
	public boolean matchAndExtract(QueryStringDecoder decoder, Channel channel) {
		Map<String, String> variables = new LinkedHashMap<>();
		boolean result = doMatch(pattern, decoder.path(), true, variables);
		if (result) {
			channel.attr(URI_TEMPLATE).set(variables);
			return true;
		}
		return false;
	}

}
