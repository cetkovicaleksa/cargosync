package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.AbstractPersistable;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Product;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "warehouse_inventory",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_inventory_warehouse_product",
        columnNames = {"warehouse_id", "product_id"}
    )
)
public class WarehouseInventory extends AbstractPersistable {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false, updatable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Column(nullable = false)
    private Long quantity = 0L;

    @Version
    private Long version;

    public WarehouseInventory(Warehouse warehouse, Product product, long quantity) {
        this.warehouse = warehouse;
        this.product = product;
        this.quantity = quantity;
    }
}
