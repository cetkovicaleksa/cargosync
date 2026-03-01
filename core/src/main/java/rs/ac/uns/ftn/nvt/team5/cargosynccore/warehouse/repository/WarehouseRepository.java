package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Warehouse;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID>, JpaSpecificationExecutor<Warehouse> {

    @Query("SELECT w FROM Warehouse w LEFT JOIN FETCH w.sectors WHERE w.id = :id")
    Optional<Warehouse> findByIdWithSectors(@Param("id") UUID id);

    List<Warehouse> findByOnlineTrueAndLastHeartbeatBefore(Instant threshold);
}
