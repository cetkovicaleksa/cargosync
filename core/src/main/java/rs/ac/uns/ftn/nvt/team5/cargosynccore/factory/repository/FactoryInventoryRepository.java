package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.FactoryInventory;

import java.util.Optional;
import java.util.UUID;

public interface FactoryInventoryRepository extends JpaRepository<FactoryInventory, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT fi
            FROM FactoryInventory fi
            WHERE fi.factory.id = :factoryId
              AND fi.product.id = :productId
            """)
    Optional<FactoryInventory> findByFactoryIdAndProductIdForUpdate(
            @Param("factoryId") UUID factoryId,
            @Param("productId") UUID productId
    );
}
