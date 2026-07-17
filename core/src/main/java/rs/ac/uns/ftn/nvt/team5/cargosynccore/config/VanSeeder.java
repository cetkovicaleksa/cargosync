package rs.ac.uns.ftn.nvt.team5.cargosynccore.config;

import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.GeoLocation;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.InfluxService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Model;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Van;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.VanStatus;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository.MakeRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository.VanRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository.VanStatusRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Profile("dev")
@RequiredArgsConstructor
@Component
@Order(4)
public class VanSeeder implements CommandLineRunner {

    private final MakeRepository makes;
    private final VanRepository vans;
    private final VanStatusRepository statuses;
    private final InfluxService influx;

    @Override
    @Transactional
    public void run(@Nullable String... args) {
        if (makes.count() == 0) {
            log.info("Skipping van seeding because makes are not loaded yet");
            return;
        }

        seedDemoVan("BNZ", "SPR906", "1", "BG-220-VN", 1400, "Mercedes-Benz Sprinter demo");
        seedDemoVan("VWG", "CRAFTR", "2", "NS-340-VN", 1750, "Volkswagen Crafter demo");
        seedDemoVan("FRD", "TRANS7", "3", "SU-180-VN", 1200, "Ford Transit demo");
    }

    private void seedDemoVan(String wmi, String vd, String vi, String reg, int maxPayload, String label) {
        var make = makes.findById(wmi).orElse(null);
        if (make == null) return;

        boolean exists = vans.findAll().stream().anyMatch(v -> v.getVin().equals(String.join("-", wmi, vd, vi)));
        if (!exists) {
            Model model = make.getModels().stream()
                    .filter(m -> vd.equals(m.getVd()))
                    .findFirst()
                    .orElseGet(() -> new Model(make, vd, label));

            Van van = new Van();
            van.setVi(vi);
            van.setModel(model);
            van.setRegistrationNumber(reg);
            van.setMaxPayload(maxPayload);
            van.setImages(List.of(
                    "https://picsum.photos/seed/van-" + wmi + "-" + vd + "/900/600",
                    "https://picsum.photos/seed/van-" + wmi + "-" + vi + "/900/600"
            ));
            vans.save(van);
        }

        statuses.findById(String.join("-", wmi, vd, vi)).orElseGet(() -> {
            VanStatus status = new VanStatus(String.join("-", wmi, vd, vi));
            status.setOnline(true);
            status.setLastHeartBeat(Instant.now().minus(45, ChronoUnit.SECONDS));
            status.setLastSeenAt(new GeoLocation("45.2671", "19.8335"));
            return statuses.save(status);
        });

        Instant now = Instant.now();
        for (int i = 0; i < 10; i++) {
            Instant pointTime = now.minus(i * 2L, ChronoUnit.DAYS);
            influx.writeVanAvailabilityEvent(String.join("-", wmi, vd, vi), i % 4 == 0 ? "offline" : "online", pointTime);
            influx.writeVanDistance(String.join("-", wmi, vd, vi), 35 + (i * 7.5), pointTime);
        }
    }
}