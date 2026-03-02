package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.repository.FactoryStatusRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.InfluxService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@RequiredArgsConstructor
@Component
public class FactoryOfflineScheduler {

    private final FactoryStatusRepository statuses;
    private final InfluxService influx;

    @Scheduled(fixedDelay = 30_000)
    public void markOfflineFactories() {
        LocalDateTime threshold = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(60);
        var stale = statuses.findByOnlineTrueAndLastHeartbeatBefore(threshold);

        for (var status : stale) {
            log.info("Marking factory {} offline (last heartbeat: {})",
                    status.getFactory().getId(), status.getLastHeartbeat());

            status.setOnline(false);
            statuses.save(status);
            influx.writeFactoryAvailabilityEvent(status.getFactory().getId(), "offline", Instant.now());
        }
    }
}
