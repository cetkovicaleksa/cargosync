package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.GeoLocation;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "van_statuses")
public class VanStatus {

    private static final Duration ONLINE_THRESHOLD = Duration.ofMinutes(2);

    @Id
    @Column(name = "vin", nullable = false, length = 64)
    private String vin;

    @Valid
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude", column = @Column(name = "last_seen_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "last_seen_longitude"))
    })
    private GeoLocation lastSeenAt;

    @Column(name = "last_heartbeat")
    private Instant lastHeartBeat;

    @Column(name = "is_online", nullable = false)
    private boolean online;

    @Version
    private Long version;

    public VanStatus(String vin) {
        this.vin = vin;
    }

    @PostLoad
    @PostPersist
    @PostUpdate
    @PrePersist
    private void computeOnline() {
        online = Optional.ofNullable(lastHeartBeat)
                .map(Instant.now().minus(ONLINE_THRESHOLD)::isBefore)
                .orElse(false);
    }

}
