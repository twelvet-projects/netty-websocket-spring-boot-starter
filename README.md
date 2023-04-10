# netty-websocket-spring-boot-starter

[![AUR](https://img.shields.io/github/license/twelvet-s/twelvet)](https://gitee.com/twelvet/netty-websocket-spring-boot-starter/blob/master/LICENSE)
[![](https://img.shields.io/badge/Author-TwelveT-orange.svg)](https://www.twelvet.cn)
[![](https://img.shields.io/badge/version-1.0.0-success)](https://gitee.com/twelvet/netty-websocket-spring-boot-starter)
[![star](https://gitee.com/twelvet/netty-websocket-spring-boot-starter/badge/star.svg?theme=white)](https://gitee.com/twelvet/netty-websocket-spring-boot-starter/stargazers)
[![fork](https://gitee.com/twelvet/netty-websocket-spring-boot-starter/badge/fork.svg?theme=white)](https://gitee.com/twelvet/netty-websocket-spring-boot-starter/members)

### 简介
在Spring Boot中使用Netty来开发WebSocket服务器，并像spring-websocket注解一样简单且高性能

### 要求
- jdk >= 1.8 (兼容jdk 17)

### 快速开始

- 添加依赖:

```xml
<dependency>
    <groupId>cn.twelvet.websocket</groupId>
    <artifactId>netty-websocket-spring-boot-starter</artifactId>
    <version>${last.version}</version>
</dependency>
```

- 在端点类上加上`@WebSocketEndpoint`注解，并在相应的方法上加上`@BeforeHandshake`、`@OnOpen`、`@OnClose`、`@OnError`、`@OnMessage`、`@OnBinary`、`@OnEvent`注解，样例如下：
- @PathVariable获取路径参数 @RequestParam获取query参数，二者皆与Spring的注解效果相同（注意：引入本框架实现的注解，不是Spring的）

```java

import cn.twelvet.websocket.netty.annotation.*;
import cn.twelvet.websocket.netty.domain.NettySession;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.timeout.IdleStateEvent;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@WebSocketEndpoint(path = "/ws")
public class MyWebSocket {

    @BeforeHandshake
    public void handshake(NettySession nettySession, HttpHeaders headers, @RequestParam String req, @RequestParam MultiValueMap reqMap, @PathVariable String arg, @PathVariable Map pathMap) {
        nettySession.setSubprotocols("stomp");
        if (!"ok".equals(req)) {
            System.out.println("Authentication failed!");
            // nettySession.close();
        }
    }

    @OnOpen
    public void onOpen(NettySession nettySession, HttpHeaders headers, @RequestParam String req, @RequestParam MultiValueMap reqMap, @PathVariable String arg, @PathVariable Map pathMap) {
        System.out.println("new connection");
        System.out.println(req);
    }

    @OnClose
    public void onClose(NettySession nettySession) {
        System.out.println("one connection closed");
    }

    @OnError
    public void onError(NettySession nettySession, Throwable throwable) {
        throwable.printStackTrace();
    }

    @OnMessage
    public void onMessage(NettySession nettySession, String message) {
        System.out.println(message);
        nettySession.sendText("Hello Netty!");
    }

    @OnBinary
    public void onBinary(NettySession nettySession, byte[] bytes) {
        for (byte b : bytes) {
            System.out.println(b);
        }
        nettySession.sendBinary(bytes);
    }

    @OnEvent
    public void onEvent(NettySession nettySession, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            switch (idleStateEvent.state()) {
                case READER_IDLE:
                    System.out.println("read idle");
                    break;
                case WRITER_IDLE:
                    System.out.println("write idle");
                    break;
                case ALL_IDLE:
                    System.out.println("all idle");
                    break;
                default:
                    break;
            }
        }
    }

}
```

- 打开WebSocket客户端，连接到`ws://127.0.0.1:80/ws/xxx`

### 通过application.properties进行配置
> 所有参数皆可使用`${...}`占位符获取`application.yml`中的配置。如下：

- 首先在`@WebSocketEndpoint`注解的属性中使用`${...}`占位符
```java
@WebSocketEndpoint(host = "${ws.host}", port = "${ws.port}")
public class MyWebSocket {
    ...
}
```
- 接下来即可在`application.yml`中配置
```
ws:
  host: 0.0.0.0
  port: 80
```
