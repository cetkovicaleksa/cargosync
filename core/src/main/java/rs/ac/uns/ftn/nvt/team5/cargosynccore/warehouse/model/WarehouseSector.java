package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "warehouse_sectors")
public class WarehouseSector extends AbstractPersistable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false, updatable = false)
    private Warehouse warehouse;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column
    private String climateZone;

    @Column
    private Double lastTemperature;

    @Column
    private Instant lastTemperatureAt;

}
