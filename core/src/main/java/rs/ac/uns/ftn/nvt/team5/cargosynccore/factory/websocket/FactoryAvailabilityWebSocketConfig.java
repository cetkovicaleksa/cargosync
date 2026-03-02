package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.config.properties.CorsProperties;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class FactoryAvailabilityWebSocketConfig implements WebSocketConfigurer {

    private final FactoryAvailabilityWebSocketHandler handler;
    private final CorsProperties corsProperties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
            .addHandler(
                handler,
                "/factories/*/availability/ws",
                "/api/factories/*/availability/ws"
            )
                .setAllowedOriginPatterns(corsProperties.allowedOrigins().toArray(String[]::new));
    }
}
