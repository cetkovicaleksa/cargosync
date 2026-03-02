package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactoryAvailabilityRealtimeEventDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactoryStatusDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.service.FactoryService;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class FactoryAvailabilityWebSocketHandler extends TextWebSocketHandler {

    private final FactoryService factoryService;
    private final ObjectMapper objectMapper;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ConcurrentMap<String, ScheduledFuture<?>> sessionTasks = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID factoryId = extractFactoryId(session.getUri());
        if (factoryId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        Runnable pushTask = () -> pushStatus(session, factoryId);

        pushTask.run();
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(pushTask, 5, 5, TimeUnit.SECONDS);
        sessionTasks.put(session.getId(), future);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cleanup(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.debug("Factory WS transport error for session {}", session.getId(), exception);
        cleanup(session.getId());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @PreDestroy
    public void shutdownScheduler() {
        for (ScheduledFuture<?> task : sessionTasks.values()) {
            task.cancel(true);
        }
        sessionTasks.clear();
        scheduler.shutdownNow();
    }

    private void pushStatus(WebSocketSession session, UUID factoryId) {
        if (!session.isOpen()) {
            cleanup(session.getId());
            return;
        }

        try {
            FactoryStatusDto status = factoryService.getFactoryStatus(factoryId);
            FactoryAvailabilityRealtimeEventDto payload = new FactoryAvailabilityRealtimeEventDto(
                    Instant.now().toString(),
                    status.isOnline()
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception ex) {
            log.debug("Failed to send realtime availability update for factory {}", factoryId, ex);
            cleanup(session.getId());
            try {
                if (session.isOpen()) {
                    session.close(CloseStatus.SERVER_ERROR);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void cleanup(String sessionId) {
        ScheduledFuture<?> task = sessionTasks.remove(sessionId);
        if (task != null) {
            task.cancel(true);
        }
    }

    private UUID extractFactoryId(URI uri) {
        if (uri == null || uri.getPath() == null) {
            return null;
        }

        String[] parts = uri.getPath().split("/");
        for (int i = 0; i < parts.length; i++) {
            if (!"factories".equals(parts[i])) {
                continue;
            }

            if (i + 3 >= parts.length) {
                return null;
            }

            String factoryIdRaw = parts[i + 1];
            String availabilityPart = parts[i + 2];
            String wsPart = parts[i + 3];

            if (!"availability".equals(availabilityPart) || !"ws".equals(wsPart)) {
                return null;
            }

            try {
                return UUID.fromString(factoryIdRaw);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        if (!uri.getPath().contains("/factories/")) {
            return null;
        }

        return null;
    }
}
