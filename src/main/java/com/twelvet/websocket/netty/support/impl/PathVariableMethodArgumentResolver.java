package com.twelvet.websocket.netty.support.impl;

import com.twelvet.websocket.netty.support.MethodArgumentResolver;
import io.netty.channel.Channel;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.core.MethodParameter;
import com.twelvet.websocket.netty.annotation.BindPathVariable;

import java.util.Map;

import static com.twelvet.websocket.netty.domain.WebSocketEndpointServer.URI_TEMPLATE;

public class PathVariableMethodArgumentResolver implements MethodArgumentResolver {

    private final AbstractBeanFactory beanFactory;

    public PathVariableMethodArgumentResolver(AbstractBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(BindPathVariable.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, Channel channel, Object object) throws Exception {
        BindPathVariable ann = parameter.getParameterAnnotation(BindPathVariable.class);
        String name = ann.name();
        if (name.isEmpty()) {
            name = parameter.getParameterName();
            if (name == null) {
                throw new IllegalArgumentException(
                        "Name for argument type [" + parameter.getNestedParameterType().getName() +
                                "] not available, and parameter name information not found in class file either.");
            }
        }
        Map<String, String> uriTemplateVars = channel.attr(URI_TEMPLATE).get();
        Object arg = (uriTemplateVars != null ? uriTemplateVars.get(name) : null);
        TypeConverter typeConverter = beanFactory.getTypeConverter();
        return typeConverter.convertIfNecessary(arg, parameter.getParameterType());
    }
}
