package cn.twelvet.websocket.netty.autoconfigure.annotation;

import cn.twelvet.websocket.netty.autoconfigure.NettyWebSocketSelector;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author twelvet 开启WebSocket
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(NettyWebSocketSelector.class)
public @interface EnableWebSocket {

	String[] scanBasePackages() default {};

}
