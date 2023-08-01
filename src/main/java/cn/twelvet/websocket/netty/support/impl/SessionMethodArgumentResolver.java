package cn.twelvet.websocket.netty.support.impl;

import cn.twelvet.websocket.netty.domain.NettySession;
import cn.twelvet.websocket.netty.support.MethodArgumentResolver;
import io.netty.channel.Channel;
import org.springframework.core.MethodParameter;

import static cn.twelvet.websocket.netty.domain.WebSocketEndpointServer.SESSION_KEY;

public class SessionMethodArgumentResolver implements MethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return NettySession.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Channel channel, Object object) throws Exception {
		return channel.attr(SESSION_KEY).get();
	}

}
