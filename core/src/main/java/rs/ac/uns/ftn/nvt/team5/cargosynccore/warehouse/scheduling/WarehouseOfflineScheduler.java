package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.WarehouseRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.InfluxService;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Component
public class WarehouseOfflineScheduler {

    private final WarehouseRepository warehouses;
    private final InfluxService influx;

    @Scheduled(fixedDelay = 30_000)
    public void markOfflineWarehouses() {
        var threshold = Instant.now().minusSeconds(60);
        var stale = warehouses.findByOnlineTrueAndLastHeartbeatBefore(threshold);
        for (var warehouse : stale) {
            log.info("Marking warehouse {} offline (last heartbeat: {})", warehouse.getId(), warehouse.getLastHeartbeat());
            warehouse.setOnline(false);
            warehouses.save(warehouse);
            influx.writeAvailabilityEvent(warehouse.getId(), false, Instant.now());
        }
    }
}
