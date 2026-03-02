package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.config.RabbitMQConfig;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactoryHeartbeatMessage;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactoryProductionMessage;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.repository.FactoryStatusRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.InfluxService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@RequiredArgsConstructor
@Component
public class FactoryMessageConsumer {

    private final FactoryStatusRepository statuses;
    private final InfluxService influx;

    @RabbitListener(queues = RabbitMQConfig.FACTORY_HEARTBEAT_QUEUE)
    public void handleHeartbeat(FactoryHeartbeatMessage msg) {
        if (msg == null || msg.factoryId() == null) {
            return;
        }

        Instant heartbeatTime = msg.timestamp() != null ? msg.timestamp() : Instant.now();
        LocalDateTime heartbeatDateTime = LocalDateTime.ofInstant(heartbeatTime, ZoneOffset.UTC);

        statuses.findByFactoryId(msg.factoryId()).ifPresentOrElse(status -> {
            boolean wasOffline = !status.isOnline();
            status.setOnline(true);
            status.setLastHeartbeat(heartbeatDateTime);
            statuses.save(status);

            if (wasOffline) {
                log.info("Factory {} is back ONLINE", msg.factoryId());
                influx.writeFactoryAvailabilityEvent(msg.factoryId(), "online", heartbeatTime);
            }
        }, () -> log.warn("Heartbeat for unknown factory: {}", msg.factoryId()));
    }

    @RabbitListener(queues = RabbitMQConfig.FACTORY_PRODUCTION_QUEUE)
    public void handleProduction(FactoryProductionMessage msg) {
        if (msg == null || msg.factoryId() == null || msg.readings() == null || msg.readings().isEmpty()) {
            return;
        }

        Instant eventTime = msg.timestamp() != null ? msg.timestamp() : Instant.now();
        for (var reading : msg.readings()) {
            if (reading == null || reading.productId() == null || reading.amount() < 0) {
                continue;
            }
            influx.writeFactoryProduction(msg.factoryId(), reading.productId(), reading.amount(), eventTime);
        }
    }
}
