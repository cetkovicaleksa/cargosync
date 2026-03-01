package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.ConflictException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository.WarehouseInventoryRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository.WarehouseRepository;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class DeleteWarehouseUC {

    private final WarehouseRepository warehouses;
    private final WarehouseInventoryRepository inventory;
    private final WarehouseService warehouseService;

    @Transactional
    public void deleteWarehouse(UUID id) {
        var warehouse = warehouseService.getWarehouse(id);

        if (inventory.existsByWarehouseIdAndQuantityGreaterThan(id, 0L)) {
            throw new ConflictException("Warehouse has active inventory");
        }

        var zeroInventory = inventory.findByWarehouseId(id);
        inventory.deleteAll(zeroInventory);

        warehouse.getImages().forEach(warehouseService::deleteWarehouseImage);

        warehouses.delete(warehouse);
    }
}
