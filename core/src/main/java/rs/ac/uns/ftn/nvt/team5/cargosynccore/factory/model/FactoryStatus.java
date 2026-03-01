package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "factory_statuses")
public class FactoryStatus {

    @Id
    @Column(name = "factory_id")
    private java.util.UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "factory_id")
    private Factory factory;

    @Column(name = "is_online", nullable = false)
    private boolean isOnline = false;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    public FactoryStatus(Factory factory) {
        this.factory = factory;
        this.isOnline = false;
        this.lastHeartbeat = LocalDateTime.now();
    }
}