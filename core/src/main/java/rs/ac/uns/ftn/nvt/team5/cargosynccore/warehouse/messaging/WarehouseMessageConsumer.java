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
            status.setOnline(true);
            status.setLastHeartbeat(Instant.now());
            statuses.save(status);
        }, () -> log.warn("Heartbeat for unknown warehouse: {}", msg.warehouseId()));
    }

    @RabbitListener(queues = RabbitMQConfig.WAREHOUSE_TEMPERATURE_QUEUE)
    public void handleTemperature(WarehouseTemperatureMessage msg) {
        sectors.findByWarehouseIdAndName(msg.warehouseId(), msg.sectorName()).ifPresentOrElse(sector -> {
            sector.setLastTemperature(msg.temperature());
            sector.setLastTemperatureAt(msg.timestamp());
            sectors.save(sector);
            influx.writeWarehouseTemperature(msg.warehouseId(), msg.sectorName(), msg.temperature(), msg.timestamp());
        }, () -> log.warn("Temperature for unknown sector: {}/{}", msg.warehouseId(), msg.sectorName()));
    }
}
