package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.InfluxService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository.WarehouseStatusRepository;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Component
public class WarehouseOfflineScheduler {

    private final WarehouseStatusRepository statuses;
    private final InfluxService influx;

    @Scheduled(fixedDelay = 30_000)
    public void markOfflineWarehouses() {
        var threshold = Instant.now().minusSeconds(60);
        var stale = statuses.findByOnlineTrueAndLastHeartbeatBefore(threshold);
        for (var status : stale) {
            log.info("Marking warehouse {} offline (last heartbeat: {})",
                    status.getWarehouse().getId(), status.getLastHeartbeat());
            status.setOnline(false);
            statuses.save(status);
            influx.writeWarehouseAvailabilityEvent(status.getWarehouse().getId(), false, Instant.now());
        }
    }
}
