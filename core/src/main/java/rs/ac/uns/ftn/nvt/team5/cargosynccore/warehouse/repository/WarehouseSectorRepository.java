package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model.WarehouseSector;

import java.util.Optional;
import java.util.UUID;

public interface WarehouseSectorRepository extends JpaRepository<WarehouseSector, UUID> {

    Optional<WarehouseSector> findByWarehouseIdAndName(UUID warehouseId, String name);

    Optional<WarehouseSector> findByIdAndWarehouseId(UUID id, UUID warehouseId);
}
