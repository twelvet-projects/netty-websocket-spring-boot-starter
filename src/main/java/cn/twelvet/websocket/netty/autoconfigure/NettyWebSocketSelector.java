package cn.twelvet.websocket.netty.autoconfigure;

import cn.twelvet.websocket.netty.standard.WebSocketEndpointExporter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

/**
 * @author twelvet 开启WebSocket
 */
@ConditionalOnMissingBean({ WebSocketEndpointExporter.class })
@AutoConfiguration
public class NettyWebSocketSelector {

	@Bean
	public WebSocketEndpointExporter webSocketEndpointExporter(ResourceLoader resourceLoader) {
		return new WebSocketEndpointExporter(resourceLoader);
	}

}
