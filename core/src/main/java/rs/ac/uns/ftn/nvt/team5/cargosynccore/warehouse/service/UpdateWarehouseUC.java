package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.NotFoundException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.GeoLocation;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.CountryRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model.Warehouse;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model.WarehouseSector;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository.WarehouseRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto.UpdateWarehouseForm;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto.WarehouseForm;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UpdateWarehouseUC {

    private final WarehouseRepository warehouses;
    private final CountryRepository countries;
    private final WarehouseService warehouseService;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Transactional
    public Warehouse updateWarehouse(UUID id, UpdateWarehouseForm form) {
        var warehouse = warehouseService.getWarehouse(id);

        Optional.ofNullable(form.getName()).ifPresent(warehouse::setName);
        Optional.ofNullable(form.getAddress()).ifPresent(warehouse::setAddress);

        if (form.getCountryCode() != null && form.getCityCode() != null) {
            var country = countries.findById(form.getCountryCode())
                    .orElseThrow(() -> new NotFoundException("Country not found: " + form.getCountryCode()));
            var city = country.getCities().get(form.getCityCode());
            if (city == null) throw new NotFoundException("City not found: " + form.getCityCode());
            warehouse.setCity(city);
        }

        if (form.getLatitude() != null && form.getLongitude() != null) {
            warehouse.setLocation(new GeoLocation(form.getLatitude(), form.getLongitude()));
        }

        if (form.getRemoveImages() != null) {
            form.getRemoveImages().forEach(path -> {
                warehouse.getImages().remove(path);
                warehouseService.deleteWarehouseImage(path);
            });
        }

        if (form.getImages() != null && !form.getImages().isEmpty()) {
            var paths = warehouseService.saveWarehouseImages(warehouse, form.getImages());
            warehouse.getImages().addAll(paths);
        }

        if (form.getSectors() != null) {
            List<WarehouseForm.SectorForm> sectorForms =
                    objectMapper.readValue(form.getSectors(), new TypeReference<>() {});
            warehouse.getSectors().clear();
            for (var sf : sectorForms) {
                var sector = new WarehouseSector();
                sector.setWarehouse(warehouse);
                sector.setName(sf.getName());
                sector.setClimateZone(sf.getClimateZone());
                warehouse.getSectors().add(sector);
            }
        }

        return warehouses.save(warehouse);
    }
}
