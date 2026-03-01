package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.WarehouseInventory;

import java.util.List;
import java.util.UUID;

public interface WarehouseInventoryRepository extends JpaRepository<WarehouseInventory, UUID> {

    /**
     * Fetches all inventory rows for a product with a PESSIMISTIC_WRITE lock.
     * Used during order placement to prevent race conditions (overselling).
     * Rows with zero stock are excluded.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT wi FROM WarehouseInventory wi " +
            "WHERE wi.product.id = :productId " +
            "AND wi.quantity > 0 " +
            "ORDER BY wi.quantity " +
            "DESC")
    List<WarehouseInventory> findByProductIdForUpdate(@Param("productId") UUID productId);

    /**
     * Batch-fetches total stock per product for product listing enrichment.
     * Returns pairs [productId, totalStock].
     */
    @Query("SELECT wi.product.id, COALESCE(SUM(wi.quantity), 0) " +
            "FROM WarehouseInventory wi " +
            "WHERE wi.product.id IN :productIds " +
            "GROUP BY wi.product.id")
    List<Object[]> findTotalStockByProductIds(@Param("productIds") List<UUID> productIds);

    @Query("SELECT COALESCE(SUM(wi.quantity), 0) " +
            "FROM WarehouseInventory wi " +
            "WHERE wi.product.id = :productId")
    Long findTotalStockByProductId(@Param("productId") UUID productId);
}
