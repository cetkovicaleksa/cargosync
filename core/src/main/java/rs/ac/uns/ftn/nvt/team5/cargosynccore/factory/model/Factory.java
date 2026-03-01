package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "factories")
public class Factory extends AbstractPersistable {

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Embedded
    private GeoLocation location;

    @NotBlank
    @Column(nullable = false)
    private String street;

    @Column(name="country_code", nullable = false)
    private String countryCode;

    @Column(name = "city_code", nullable = false)
    private String cityCode;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "factory_images", joinColumns = @JoinColumn(name = "factory_id"))
    @Column(name = "image_url", nullable = false)
    @BatchSize(size = 24)
    private Set<String> imageUrls = new HashSet<>();

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToMany(mappedBy = "factories")
    @BatchSize(size = 24)
    private Set<Product> products = new HashSet<>();
}