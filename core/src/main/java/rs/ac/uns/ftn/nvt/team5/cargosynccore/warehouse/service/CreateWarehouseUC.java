package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.NotFoundException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.GeoLocation;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.CountryRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model.Warehouse;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model.WarehouseSector;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository.WarehouseRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto.WarehouseForm;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CreateWarehouseUC {

    private final WarehouseRepository warehouses;
    private final CountryRepository countries;
    private final WarehouseService warehouseService;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    public Warehouse createWarehouse(WarehouseForm form) {
        var country = countries.findById(form.getCountryCode())
                .orElseThrow(() -> new NotFoundException("Country not found: " + form.getCountryCode()));
        var city = country.getCities().get(form.getCityCode());
        if (city == null) throw new NotFoundException("City not found: " + form.getCityCode());

        var warehouse = new Warehouse();
        warehouse.setName(form.getName());
        warehouse.setAddress(form.getAddress());
        warehouse.setCity(city);
        if (form.getLatitude() != null && form.getLongitude() != null) {
            warehouse.setLocation(new GeoLocation(form.getLatitude(), form.getLongitude()));
        }

        var saved = warehouses.saveAndFlush(warehouse);

        List<WarehouseForm.SectorForm> sectorForms = form.getSectors() != null
                ? objectMapper.readValue(form.getSectors(), new TypeReference<>() {})
                : Collections.emptyList();

        for (var sf : sectorForms) {
            var sector = new WarehouseSector();
            sector.setWarehouse(saved);
            sector.setName(sf.getName());
            sector.setClimateZone(sf.getClimateZone());
            saved.getSectors().add(sector);
        }

        if (form.getImages() != null && !form.getImages().isEmpty()) {
            var paths = warehouseService.saveWarehouseImages(saved, form.getImages());
            saved.getImages().addAll(paths);
        }

        return warehouses.save(saved);
    }
}
