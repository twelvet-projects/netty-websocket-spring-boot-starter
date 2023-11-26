[中文](https://github.com/twelvet-projects/netty-websocket-spring-boot-starter/blob/master/README_ZH.md) | [English](https://github.com/twelvet-projects/netty-websocket-spring-boot-starter/blob/master/README.md)
# netty-websocket-spring-boot-starter

It is a code refactoring and feature enhancement for the original netty-websocket-spring-boot-starter.
Thank you very much for the author's sharing of `netty-websocket-spring-boot-starter`.

[![AUR](https://img.shields.io/github/license/twelvet-projects/twelvet)](https://github.com/twelvet-projects/netty-websocket-spring-boot-starter/blob/master/LICENSE)
[![](https://img.shields.io/badge/Author-TwelveT-orange.svg)](https://twelvet.cn)
[![](https://img.shields.io/badge/version-1.0.0-success)](https://github.com/twelvet-projects/netty-websocket-spring-boot-starter)
[![GitHub stars](https://img.shields.io/github/stars/twelvet-projects/netty-websocket-spring-boot-starter.svg?style=social&label=Stars)](https://github.com/twelvet-projects/netty-websocket-spring-boot-starter/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/twelvet-projects/netty-websocket-spring-boot-starter.svg?style=social&label=Fork)](https://github.com/twelvet-projects/netty-websocket-spring-boot-starter/network/members)
[![star](https://gitee.com/twelvet/netty-websocket-spring-boot-starter/badge/star.svg?theme=white)](https://gitee.com/twelvet/netty-websocket-spring-boot-starter/stargazers)
[![fork](https://gitee.com/twelvet/netty-websocket-spring-boot-starter/badge/fork.svg?theme=white)](https://gitee.com/twelvet/netty-websocket-spring-boot-starter/members)

### Introduction
Developing a WebSocket server using Netty in Spring Boot, with the simplicity and high performance of spring-websocket annotations.

### Requirements
- jdk >= 1.8 (compatible with jdk 17、21)

### Quick Start

- Add dependencies:

```xml
<dependency>
    <groupId>cn.twelvet</groupId>
    <artifactId>netty-websocket-spring-boot-starter</artifactId>
    <version>${version}</version>
</dependency>
```

- Add the `@WebSocketEndpoint` annotation to the endpoint class, and add the `@BeforeHandshake`、`@OnOpen`、`@OnClose`、`@OnError`、`@OnMessage`、`@OnBinary` and `@OnEvent` annotations to the respective methods. Here's an example:
- Use `@PathVariable` to retrieve path parameters and `@RequestParam` to retrieve query parameters, both of which have the same effect as the corresponding Spring annotations (Note: Use the annotations provided by this framework, not Spring's annotations).

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

- Open the WebSocket client and connect to `ws://127.0.0.1:80/ws/xxx`

### Multi Endpoint
- base on [Quick-Start](#quick-start),use annotation `@WebSocketEndpoint` in classes which hope to become a endpoint.
- you can get all socket addresses in `WebSocketEndpointExporter.getAddressWebsocketServerMap()`.
- when there are different addresses(different host or different port) in WebSocket,they will use different `ServerBootstrap` instance.
- when the addresses are the same,but path is different,they will use the same `ServerBootstrap` instance.
- when multiple port of endpoint is 0 ,they will use the same random port
- when multiple port of endpoint is the same as the path,host can't be set as "0.0.0.0",because it means it binds all of the addresses

### Configure using application.properties.

> All parameters can be obtained from the configuration in `application.yml` using `${...}` placeholders. Here's an example:：

- First, use `${...}` placeholders in the attributes of the `@WebSocketEndpoint` annotation.
```java
@WebSocketEndpoint(host = "${ws.host}", port = "${ws.port}")
public class MyWebSocket {
    ...
}
```
- Next, you can configure it in the `application.yml` file.
```
ws:
  host: 0.0.0.0
  port: 80
```
