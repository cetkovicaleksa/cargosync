package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model.WarehouseStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WarehouseStatusRepository extends JpaRepository<WarehouseStatus, UUID> {

    Optional<WarehouseStatus> findByWarehouseId(UUID warehouseId);

    List<WarehouseStatus> findByOnlineTrueAndLastHeartbeatBefore(Instant threshold);
}
