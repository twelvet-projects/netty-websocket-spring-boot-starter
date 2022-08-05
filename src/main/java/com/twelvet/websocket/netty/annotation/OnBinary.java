package com.twelvet.websocket.netty.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author twelvet
 * @WebSite www.twelvet.cn
 * @Description: 当接收到二进制消息时，对该方法进行回调 注入参数的类型:Session、byte[]
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnBinary {
}