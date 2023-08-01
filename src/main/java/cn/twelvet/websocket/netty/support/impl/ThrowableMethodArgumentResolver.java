package cn.twelvet.websocket.netty.support.impl;

import cn.twelvet.websocket.netty.support.MethodArgumentResolver;
import io.netty.channel.Channel;
import org.springframework.core.MethodParameter;
import cn.twelvet.websocket.netty.annotation.OnError;

public class ThrowableMethodArgumentResolver implements MethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.getMethod().isAnnotationPresent(OnError.class)
				&& Throwable.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Channel channel, Object object) throws Exception {
		if (object instanceof Throwable) {
			return object;
		}
		return null;
	}

}
