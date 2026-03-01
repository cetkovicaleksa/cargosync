package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product extends AbstractAuditable {

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(name = "image_url")
    private String imageUrl;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false)
    private BigDecimal price;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false)
    private Double weight;

    @Column(name = "is_for_sale", nullable = false)
    private boolean isForSale = false;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @BatchSize(size = 24)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_factories",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "factory_id")
    )
    private Set<Factory> factories = new HashSet<>();
}