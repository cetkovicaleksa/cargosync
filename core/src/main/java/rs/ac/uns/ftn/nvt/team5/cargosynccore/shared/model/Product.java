package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.Factory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_products_name", columnList = "name"),
        @Index(name = "idx_products_category_id", columnList = "category_id"),
        @Index(name = "idx_products_price", columnList = "price"),
        @Index(name = "idx_products_is_for_sale_price", columnList = "is_for_sale,price")
    }
)
public class Product extends AbstractPersistable implements Serializable {

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
            inverseJoinColumns = @JoinColumn(name = "factory_id"),
            indexes = {
                @Index(name = "idx_product_factories_product_id", columnList = "product_id"),
                @Index(name = "idx_product_factories_factory_id", columnList = "factory_id")
            }
    )
    private Set<Factory> factories = new HashSet<>();
}