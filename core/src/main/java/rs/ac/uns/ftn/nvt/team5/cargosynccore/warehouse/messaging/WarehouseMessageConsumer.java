package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.config.RabbitMQConfig;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.InfluxService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto.WarehouseHeartbeatMessage;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto.WarehouseTemperatureMessage;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository.WarehouseSectorRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository.WarehouseStatusRepository;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Component
public class WarehouseMessageConsumer {

    private final WarehouseStatusRepository statuses;
    private final WarehouseSectorRepository sectors;
    private final InfluxService influx;

    @RabbitListener(queues = RabbitMQConfig.WAREHOUSE_HEARTBEAT_QUEUE)
    public void handleHeartbeat(WarehouseHeartbeatMessage msg) {
        statuses.findByWarehouseId(msg.warehouseId()).ifPresentOrElse(status -> {
            boolean wasOffline = !status.isOnline();
            status.setOnline(true);
            status.setLastHeartbeat(Instant.now());
            statuses.save(status);
            if (wasOffline) {
                log.info("Warehouse {} is back ONLINE", msg.warehouseId());
                influx.writeWarehouseAvailabilityEvent(status.getWarehouse().getId(), "online", Instant.now());
            }
        }, () -> log.warn("Heartbeat for unknown warehouse: {}", msg.warehouseId()));
    }

    @RabbitListener(queues = RabbitMQConfig.WAREHOUSE_TEMPERATURE_QUEUE)
    public void handleTemperature(WarehouseTemperatureMessage msg) {
        if (msg.readings() == null) return;
        for (var reading : msg.readings()) {
            sectors.findByWarehouseIdAndName(msg.warehouseId(), reading.sectorName()).ifPresentOrElse(sector -> {
                sector.setLastTemperature(reading.temperature());
                sector.setLastTemperatureAt(msg.timestamp() != null ? msg.timestamp() : Instant.now());
                sectors.save(sector);
                influx.writeWarehouseTemperature(
                        msg.warehouseId(), reading.sectorId(), reading.sectorName(),
                        reading.temperature(), sector.getLastTemperatureAt());
            }, () -> log.warn("Temperature for unknown sector: {}/{}", msg.warehouseId(), reading.sectorName()));
        }
    }
}
