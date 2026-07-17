package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.config.RabbitMQConfig;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.NotFoundException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.GeoLocation;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.InfluxService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto.VanDistanceMessage;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto.VanHeartbeatMessage;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.VanStatus;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository.VanStatusRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.service.VanService;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Component
public class VanMessageConsumer {

    private final VanService vanService;
    private final VanStatusRepository statuses;
    private final InfluxService influx;

    @RabbitListener(queues = RabbitMQConfig.VAN_HARTBEAT_QUEUE)
    public void handleHeartbeat(VanHeartbeatMessage msg) {
        if (!isKnownVan(msg == null ? null : msg.vanId())) {
            return;
        }

        Instant heartbeatTime = msg.timestamp() != null ? msg.timestamp() : Instant.now();
        upsertStatus(msg.vanId(), heartbeatTime, null, true);
    }

    @RabbitListener(queues = RabbitMQConfig.VAN_DATA_QUEUE)
    public void handleDistance(VanDistanceMessage msg) {
        if (!isKnownVan(msg == null ? null : msg.vanId())) {
            return;
        }

        Instant eventTime = msg.timestamp() != null ? msg.timestamp() : Instant.now();
        GeoLocation location = msg.location() != null
                ? new GeoLocation(msg.location().getLatitude(), msg.location().getLongitude())
                : null;

        upsertStatus(msg.vanId(), eventTime, location, true);

        if (msg.distance() != null && msg.distance() >= 0) {
            influx.writeVanDistance(msg.vanId(), msg.distance(), eventTime);
        }
    }

    private boolean isKnownVan(String vin) {
        if (vin == null || vin.isBlank()) {
            return false;
        }

        try {
            vanService.getVan(vin);
            return true;
        } catch (RuntimeException ex) {
            if (ex instanceof NotFoundException) {
                log.warn("Telemetry received for unknown van: {}", vin);
            } else {
                log.warn("Invalid van telemetry identifier {}", vin, ex);
            }
            return false;
        }
    }

    private void upsertStatus(String vin, Instant heartbeatTime, GeoLocation location, boolean online) {
        statuses.findById(vin).ifPresentOrElse(status -> {
            boolean wasOffline = !status.isOnline();
            status.setOnline(online);
            status.setLastHeartBeat(heartbeatTime);
            if (location != null) {
                status.setLastSeenAt(location);
            }
            statuses.save(status);

            if (wasOffline && online) {
                influx.writeVanAvailabilityEvent(vin, "online", heartbeatTime);
            }
        }, () -> {
            var status = new VanStatus(vin);
            status.setOnline(online);
            status.setLastHeartBeat(heartbeatTime);
            status.setLastSeenAt(location);
            statuses.save(status);

            if (online) {
                influx.writeVanAvailabilityEvent(vin, "online", heartbeatTime);
            }
        });
    }
}