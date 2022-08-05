package com.twelvet.websocket.netty.domain;

import com.twelvet.websocket.netty.support.impl.*;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import com.twelvet.websocket.netty.annotation.*;
import com.twelvet.websocket.netty.exception.WebSocketException;
import com.twelvet.websocket.netty.support.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author twelvet
 * @WebSite www.twelvet.cn
 * @Description: 断点方法注入参数
 */
public class WebSocketMethodMapping {

    private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private final Method beforeHandshake;
    private final Method onOpen;
    private final Method onClose;
    private final Method onError;
    private final Method onMessage;
    private final Method onBinary;
    private final Method onEvent;

    private final MethodParameter[] beforeHandshakeParameters;
    private final MethodParameter[] onOpenParameters;
    private final MethodParameter[] onCloseParameters;
    private final MethodParameter[] onErrorParameters;
    private final MethodParameter[] onMessageParameters;
    private final MethodParameter[] onBinaryParameters;
    private final MethodParameter[] onEventParameters;

    private final MethodArgumentResolver[] beforeHandshakeArgResolvers;
    private final MethodArgumentResolver[] onOpenArgResolvers;
    private final MethodArgumentResolver[] onCloseArgResolvers;
    private final MethodArgumentResolver[] onErrorArgResolvers;
    private final MethodArgumentResolver[] onMessageArgResolvers;
    private final MethodArgumentResolver[] onBinaryArgResolvers;
    private final MethodArgumentResolver[] onEventArgResolvers;

    private final Class<?> webSocketClazz;

    private final ApplicationContext applicationContext;

    private final AbstractBeanFactory beanFactory;

    public WebSocketMethodMapping(Class<?> webSocketClazz, ApplicationContext context, AbstractBeanFactory beanFactory) throws WebSocketException {
        this.applicationContext = context;
        this.webSocketClazz = webSocketClazz;
        this.beanFactory = beanFactory;
        Method handshake = null;
        Method open = null;
        Method close = null;
        Method error = null;
        Method message = null;
        Method binary = null;
        Method event = null;
        Method[] webSocketClazzMethods = null;
        Class<?> currentClazz = webSocketClazz;

        // Find parent class
        while (!currentClazz.equals(Object.class)) {
            Method[] currentClazzMethods = currentClazz.getDeclaredMethods();
            if (currentClazz == webSocketClazz) {
                webSocketClazzMethods = currentClazzMethods;
            }
            // Rewriting method does not allow multiple annotations
            for (Method method : currentClazzMethods) {
                if (method.getAnnotation(BeforeHandshake.class) != null) {
                    checkIsPublic(method);
                    if (handshake == null) {
                        handshake = method;
                    } else {
                        if (currentClazz == webSocketClazz ||
                                !isMethodOverride(handshake, method)) {
                            // Duplicate annotation
                            throw new WebSocketException(
                                    "duplicateAnnotation BeforeHandshake");
                        }
                    }
                } else if (method.getAnnotation(OnOpen.class) != null) {
                    checkIsPublic(method);
                    if (open == null) {
                        open = method;
                    } else {
                        if (currentClazz == webSocketClazz ||
                                !isMethodOverride(open, method)) {
                            // Duplicate annotation
                            throw new WebSocketException(
                                    "duplicateAnnotation OnOpen");
                        }
                    }
                } else if (method.getAnnotation(OnClose.class) != null) {
                    checkIsPublic(method);
                    if (close == null) {
                        close = method;
                    } else {
                        if (currentClazz == webSocketClazz ||
                                !isMethodOverride(close, method)) {
                            // Duplicate annotation
                            throw new WebSocketException(
                                    "duplicateAnnotation OnClose");
                        }
                    }
                } else if (method.getAnnotation(OnError.class) != null) {
                    checkIsPublic(method);
                    if (error == null) {
                        error = method;
                    } else {
                        if (currentClazz == webSocketClazz ||
                                !isMethodOverride(error, method)) {
                            // Duplicate annotation
                            throw new WebSocketException(
                                    "duplicateAnnotation OnError");
                        }
                    }
                } else if (method.getAnnotation(OnMessage.class) != null) {
                    checkIsPublic(method);
                    if (message == null) {
                        message = method;
                    } else {
                        if (currentClazz == webSocketClazz ||
                                !isMethodOverride(message, method)) {
                            // Duplicate annotation
                            throw new WebSocketException(
                                    "duplicateAnnotation onMessage");
                        }
                    }
                } else if (method.getAnnotation(OnBinary.class) != null) {
                    checkIsPublic(method);
                    if (binary == null) {
                        binary = method;
                    } else {
                        if (currentClazz == webSocketClazz ||
                                !isMethodOverride(binary, method)) {
                            // Duplicate annotation
                            throw new WebSocketException(
                                    "duplicateAnnotation OnBinary");
                        }
                    }
                } else if (method.getAnnotation(OnEvent.class) != null) {
                    checkIsPublic(method);
                    if (event == null) {
                        event = method;
                    } else {
                        if (currentClazz == webSocketClazz ||
                                !isMethodOverride(event, method)) {
                            // Duplicate annotation
                            throw new WebSocketException(
                                    "duplicateAnnotation OnEvent");
                        }
                    }
                } else {
                    // Method not annotated
                }
            }
            // Continue to get the previous level
            currentClazz = currentClazz.getSuperclass();
        }

        // When the method is rewritten and there is no annotation, the parent annotation function of this method will be cancelled
        if (handshake != null && handshake.getDeclaringClass() != webSocketClazz) {
            if (isOverrideWithoutAnnotation(webSocketClazzMethods, handshake, BeforeHandshake.class)) {
                handshake = null;
            }
        }
        if (open != null && open.getDeclaringClass() != webSocketClazz) {
            if (isOverrideWithoutAnnotation(webSocketClazzMethods, open, OnOpen.class)) {
                open = null;
            }
        }
        if (close != null && close.getDeclaringClass() != webSocketClazz) {
            if (isOverrideWithoutAnnotation(webSocketClazzMethods, close, OnClose.class)) {
                close = null;
            }
        }
        if (error != null && error.getDeclaringClass() != webSocketClazz) {
            if (isOverrideWithoutAnnotation(webSocketClazzMethods, error, OnError.class)) {
                error = null;
            }
        }
        if (message != null && message.getDeclaringClass() != webSocketClazz) {
            if (isOverrideWithoutAnnotation(webSocketClazzMethods, message, OnMessage.class)) {
                message = null;
            }
        }
        if (binary != null && binary.getDeclaringClass() != webSocketClazz) {
            if (isOverrideWithoutAnnotation(webSocketClazzMethods, binary, OnBinary.class)) {
                binary = null;
            }
        }
        if (event != null && event.getDeclaringClass() != webSocketClazz) {
            if (isOverrideWithoutAnnotation(webSocketClazzMethods, event, OnEvent.class)) {
                event = null;
            }
        }

        this.beforeHandshake = handshake;
        this.onOpen = open;
        this.onClose = close;
        this.onError = error;
        this.onMessage = message;
        this.onBinary = binary;
        this.onEvent = event;

        // Get parameters
        beforeHandshakeParameters = getParameters(beforeHandshake);
        onOpenParameters = getParameters(onOpen);
        onCloseParameters = getParameters(onClose);
        onMessageParameters = getParameters(onMessage);
        onErrorParameters = getParameters(onError);
        onBinaryParameters = getParameters(onBinary);
        onEventParameters = getParameters(onEvent);

        // Get parser
        beforeHandshakeArgResolvers = getResolvers(beforeHandshakeParameters);
        onOpenArgResolvers = getResolvers(onOpenParameters);
        onCloseArgResolvers = getResolvers(onCloseParameters);
        onMessageArgResolvers = getResolvers(onMessageParameters);
        onErrorArgResolvers = getResolvers(onErrorParameters);
        onBinaryArgResolvers = getResolvers(onBinaryParameters);
        onEventArgResolvers = getResolvers(onEventParameters);
    }

    /**
     * Check whether it is public
     *
     * @param m Method
     */
    private void checkIsPublic(Method m) throws WebSocketException {
        if (!Modifier.isPublic(m.getModifiers())) {
            throw new WebSocketException(
                    "methodNotPublic " + m.getName());
        }
    }

    /**
     * Whether the judgment method is consistent
     *
     * @param method1 子方法
     * @param method2 父方法
     * @return boolean
     */
    private boolean isMethodOverride(Method method1, Method method2) {
        // methodName
        return (method1.getName().equals(method2.getName())
                // return type
                && method1.getReturnType().equals(method2.getReturnType())
                // params
                && Arrays.equals(method1.getParameterTypes(), method2.getParameterTypes()));
    }

    /**
     * Whether the rewriting method changes the annotation
     *
     * @param methods          All methods of class
     * @param superClazzMethod superClazzMethod annotation
     * @param annotation       annotation
     * @return boolean
     */
    private boolean isOverrideWithoutAnnotation(Method[] methods, Method superClazzMethod, Class<? extends Annotation> annotation) {
        for (Method method : methods) {
            // Whether the method is overridden and whether the obtained annotation is empty
            if (isMethodOverride(method, superClazzMethod)
                    && (method.getAnnotation(annotation) == null)) {
                return true;
            }
        }
        return false;
    }

    Object getEndpointInstance() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Object implement = webSocketClazz.getDeclaredConstructor().newInstance();
        AutowiredAnnotationBeanPostProcessor postProcessor = applicationContext.getBean(AutowiredAnnotationBeanPostProcessor.class);
        postProcessor.postProcessProperties(null, implement, null);
        return implement;
    }

    Method getBeforeHandshake() {
        return beforeHandshake;
    }

    Object[] getBeforeHandshakeArgs(Channel channel, FullHttpRequest req) throws Exception {
        return getMethodArgumentValues(channel, req, beforeHandshakeParameters, beforeHandshakeArgResolvers);
    }

    Method getOnOpen() {
        return onOpen;
    }

    Object[] getOnOpenArgs(Channel channel, FullHttpRequest req) throws Exception {
        return getMethodArgumentValues(channel, req, onOpenParameters, onOpenArgResolvers);
    }

    MethodArgumentResolver[] getOnOpenArgResolvers() {
        return onOpenArgResolvers;
    }

    Method getOnClose() {
        return onClose;
    }

    Object[] getOnCloseArgs(Channel channel) throws Exception {
        return getMethodArgumentValues(channel, null, onCloseParameters, onCloseArgResolvers);
    }

    Method getOnError() {
        return onError;
    }

    Object[] getOnErrorArgs(Channel channel, Throwable throwable) throws Exception {
        return getMethodArgumentValues(channel, throwable, onErrorParameters, onErrorArgResolvers);
    }

    Method getOnMessage() {
        return onMessage;
    }

    Object[] getOnMessageArgs(Channel channel, TextWebSocketFrame textWebSocketFrame) throws Exception {
        return getMethodArgumentValues(channel, textWebSocketFrame, onMessageParameters, onMessageArgResolvers);
    }

    Method getOnBinary() {
        return onBinary;
    }

    Object[] getOnBinaryArgs(Channel channel, BinaryWebSocketFrame binaryWebSocketFrame) throws Exception {
        return getMethodArgumentValues(channel, binaryWebSocketFrame, onBinaryParameters, onBinaryArgResolvers);
    }

    Method getOnEvent() {
        return onEvent;
    }

    Object[] getOnEventArgs(Channel channel, Object evt) throws Exception {
        return getMethodArgumentValues(channel, evt, onEventParameters, onEventArgResolvers);
    }

    /**
     * Get all params
     *
     * @param channel    Channel
     * @param object     Object
     * @param parameters MethodParameter[]
     * @param resolvers  MethodArgumentResolver
     * @return Object[]
     */
    private Object[] getMethodArgumentValues(Channel channel, Object object, MethodParameter[] parameters, MethodArgumentResolver[] resolvers) throws Exception {
        Object[] objects = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter parameter = parameters[i];
            MethodArgumentResolver resolver = resolvers[i];
            Object arg = resolver.resolveArgument(parameter, channel, object);
            objects[i] = arg;
        }
        return objects;
    }

    /**
     * Get all parsers
     *
     * @param parameters MethodParameter[]
     * @return MethodArgumentResolver[]
     */
    private MethodArgumentResolver[] getResolvers(MethodParameter[] parameters) throws WebSocketException {
        MethodArgumentResolver[] methodArgumentResolvers = new MethodArgumentResolver[parameters.length];
        List<MethodArgumentResolver> resolvers = getDefaultResolvers();

        for (int i = 0; i < parameters.length; i++) {
            MethodParameter parameter = parameters[i];
            for (MethodArgumentResolver resolver : resolvers) {
                if (resolver.supportsParameter(parameter)) {
                    methodArgumentResolvers[i] = resolver;
                    break;
                }
            }
            if (methodArgumentResolvers[i] == null) {
                throw new WebSocketException("paramClassIncorrect parameter name : " + parameter.getParameterName());
            }
        }

        return methodArgumentResolvers;
    }

    /**
     * Add netty parser
     *
     * @return List<MethodArgumentResolver>
     */
    private List<MethodArgumentResolver> getDefaultResolvers() {
        List<MethodArgumentResolver> resolvers = new ArrayList<>();
        resolvers.add(new SessionMethodArgumentResolver());
        resolvers.add(new HttpHeadersMethodArgumentResolver());
        resolvers.add(new TextMethodArgumentResolver());
        resolvers.add(new ThrowableMethodArgumentResolver());
        resolvers.add(new ByteMethodArgumentResolver());
        resolvers.add(new RequestParamMapMethodArgumentResolver());
        resolvers.add(new RequestParamMethodArgumentResolver(beanFactory));
        resolvers.add(new PathVariableMapMethodArgumentResolver());
        resolvers.add(new PathVariableMethodArgumentResolver(beanFactory));
        resolvers.add(new EventMethodArgumentResolver(beanFactory));
        return resolvers;
    }

    /**
     * Get all parameters
     *
     * @param method Method
     * @return Method params
     */
    private static MethodParameter[] getParameters(Method method) {
        if (method == null) {
            return new MethodParameter[0];
        }
        int paramsCount = method.getParameterCount();
        MethodParameter[] result = new MethodParameter[paramsCount];
        for (int i = 0; i < paramsCount; i++) {
            MethodParameter methodParameter = new MethodParameter(method, i);
            methodParameter.initParameterNameDiscovery(parameterNameDiscoverer);
            result[i] = methodParameter;
        }
        return result;
    }
}