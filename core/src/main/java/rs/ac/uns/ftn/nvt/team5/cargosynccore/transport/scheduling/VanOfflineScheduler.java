package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.InfluxService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository.VanStatusRepository;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Component
public class VanOfflineScheduler {

    private final VanStatusRepository statuses;
    private final InfluxService influx;

    @Scheduled(fixedDelay = 30_000)
    public void markOfflineVans() {
        var threshold = Instant.now().minusSeconds(60);
        var stale = statuses.findByOnlineTrueAndLastHeartBeatBefore(threshold);

        for (var status : stale) {
            log.info("Marking van {} offline (last heartbeat: {})", status.getVin(), status.getLastHeartBeat());
            status.setOnline(false);
            statuses.save(status);
            influx.writeVanAvailabilityEvent(status.getVin(), "offline", Instant.now());
        }
    }
}