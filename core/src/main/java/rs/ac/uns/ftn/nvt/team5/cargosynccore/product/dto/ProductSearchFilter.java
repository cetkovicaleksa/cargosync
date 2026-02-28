package rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSearchFilter(
        String name,
        String categoryName,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean isForSale
) {}