package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.service;

import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model.Warehouse;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository.WarehouseRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto.WarehouseQueryParams;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class QueryWarehousesUC {

    private final WarehouseRepository warehouses;
    private final WarehouseService warehouseService;

    @Transactional(readOnly = true)
    public Page<Warehouse> getWarehouses(WarehouseQueryParams params, Pageable pageable) {
        Specification<Warehouse> spec = (root, query, cb) -> cb.conjunction();

        if (StringUtils.hasText(params.getName())) {
            String pattern = "%" + params.getName().toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), pattern));
        }
        if (StringUtils.hasText(params.getCountryCode())) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("city").get("country").get("code"), params.getCountryCode()));
        }
        if (StringUtils.hasText(params.getCityCode())) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("city").get("code"), params.getCityCode()));
        }
        if (params.getOnline() != null) {
            spec = spec.and((root, query, cb) -> {
                var statusJoin = root.join("status", JoinType.LEFT);
                return cb.equal(statusJoin.get("online"), params.getOnline());
            });
        }

        return warehouses.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Warehouse getWarehouse(UUID id) {
        return warehouseService.getWarehouse(id);
    }
}
