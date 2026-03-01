package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "warehouses")
public class Warehouse extends AbstractAuditable {

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String address;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumns({
        @JoinColumn(name = "city_country_code", referencedColumnName = "country_code"),
        @JoinColumn(name = "city_code", referencedColumnName = "code")
    })
    private City city;

    @Embedded
    private GeoLocation location;

    @ElementCollection
    private List<String> images = new ArrayList<>();

    @Column(nullable = false)
    private boolean online = false;

    private Instant lastHeartbeat;

    @OneToMany(mappedBy = "warehouse", cascade = ALL, orphanRemoval = true)
    private List<WarehouseSector> sectors = new ArrayList<>();

    @Version
    private Long version;
}
