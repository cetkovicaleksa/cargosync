package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.NotFoundException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model.Warehouse;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository.WarehouseRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.ContentStore;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class WarehouseService {

    private final WarehouseRepository warehouses;
    private final ContentStore content;

    @Transactional(readOnly = true)
    public Warehouse getWarehouse(UUID id) {
        return warehouses.findByIdWithSectors(id)
                .orElseThrow(() -> new NotFoundException("Warehouse not found"));
    }

    List<String> saveWarehouseImages(Warehouse warehouse, List<MultipartFile> images) {
        return images.stream()
                .map(img -> {
                    String path = "warehouses/" + warehouse.getId() + "-" + img.getOriginalFilename();
                    content.save(path, img);
                    return path;
                })
                .toList();
    }

    void deleteWarehouseImage(String path) {
        content.delete(path);
    }
}
