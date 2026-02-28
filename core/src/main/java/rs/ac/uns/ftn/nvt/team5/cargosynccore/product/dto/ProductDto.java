package rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String name,
        String imageUrl,
        BigDecimal price,
        Double weight,
        boolean isForSale,
        UUID categoryId,
        String categoryName,
        Set<UUID> factoryIds,
        Long version // optimistic locking
) {}