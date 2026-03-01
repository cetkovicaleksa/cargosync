package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository.WarehouseRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.InfluxService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto.WarehouseHeartbeatMessage;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto.WarehouseTemperatureMessage;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository.WarehouseSectorRepository;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Component
public class WarehouseMessageConsumer {

    private final WarehouseRepository warehouses;
    private final WarehouseSectorRepository sectors;
    private final InfluxService influx;

    @RabbitListener(queues = "warehouse-heartbeat")
    public void handleHeartbeat(WarehouseHeartbeatMessage msg) {
        warehouses.findById(msg.warehouseId()).ifPresentOrElse(warehouse -> {
            warehouse.setOnline(true);
            warehouse.setLastHeartbeat(Instant.now());
            warehouses.save(warehouse);
        }, () -> log.warn("Heartbeat for unknown warehouse: {}", msg.warehouseId()));
    }

    @RabbitListener(queues = "warehouse-temperature")
    public void handleTemperature(WarehouseTemperatureMessage msg) {
        sectors.findByWarehouseIdAndName(msg.warehouseId(), msg.sectorName()).ifPresentOrElse(sector -> {
            sector.setLastTemperature(msg.temperature());
            sector.setLastTemperatureAt(msg.timestamp());
            sectors.save(sector);
            influx.writeTemperature(msg.warehouseId(), msg.sectorName(), msg.temperature(), msg.timestamp());
        }, () -> log.warn("Temperature for unknown sector: {}/{}", msg.warehouseId(), msg.sectorName()));
    }
}
