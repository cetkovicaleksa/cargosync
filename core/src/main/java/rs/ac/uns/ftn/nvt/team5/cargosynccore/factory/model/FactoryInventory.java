package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.AbstractAuditable;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.AbstractPersistable;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Product;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "factory_inventory",
    uniqueConstraints = @UniqueConstraint(name = "uk_factory_inventory_factory_product", columnNames = {"factory_id", "product_id"})
)
public class FactoryInventory extends AbstractPersistable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factory_id", nullable = false)
    private Factory factory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "total_produced", nullable = false)
    private Long totalProduced = 0L;

    @Column(name = "last_production_time")
    private LocalDateTime lastProductionTime;
}