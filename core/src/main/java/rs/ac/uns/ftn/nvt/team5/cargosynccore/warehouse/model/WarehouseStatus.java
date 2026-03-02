package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.AbstractPersistable;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "warehouse_status")
public class WarehouseStatus extends AbstractPersistable {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false, unique = true, updatable = false)
    private Warehouse warehouse;

    @Column(nullable = false)
    private boolean online = false;

    private Instant lastHeartbeat;

    @Version
    private Long version;

    public WarehouseStatus(Warehouse warehouse) {
        this.warehouse = warehouse;
    }
}
