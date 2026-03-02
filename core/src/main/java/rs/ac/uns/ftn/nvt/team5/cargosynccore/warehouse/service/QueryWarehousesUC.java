package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.service;

import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.PageResult;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto.WarehouseDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto.WarehouseQueryParams;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model.Warehouse;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository.WarehouseRepository;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class QueryWarehousesUC {

    private final WarehouseRepository warehouses;
    private final WarehouseService warehouseService;
    private final ModelMapper modelMapper;

    @Cacheable(
            cacheNames = "warehouseList",
            key = "#params.name + ':' + #params.countryCode + ':' + #params.cityCode + ':' + #params.online + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort"
    )
    @Transactional(readOnly = true)
    public PageResult<WarehouseDto> getWarehouses(WarehouseQueryParams params, Pageable pageable) {
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

        // Eagerly fetch city (ManyToOne — no row multiplication, safe with pagination).
        // Guard against the count query Spring Data runs internally for Page.
        spec = spec.and((root, query, cb) -> {
            if (!Long.class.isAssignableFrom(query.getResultType())) {
                root.fetch("city", JoinType.LEFT);
            }
            return cb.conjunction();
        });

        Page<WarehouseDto> page = warehouses.findAll(spec, pageable)
                .map(w -> modelMapper.map(w, WarehouseDto.class));
        return PageResult.of(page);
    }

    @Cacheable(cacheNames = "warehouseDetail", key = "#id")
    @Transactional(readOnly = true)
    public WarehouseDto getWarehouse(UUID id) {
        return modelMapper.map(warehouseService.getWarehouse(id), WarehouseDto.class);
    }
}
