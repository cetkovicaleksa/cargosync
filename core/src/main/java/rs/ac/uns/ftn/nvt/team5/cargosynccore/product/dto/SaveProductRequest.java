package rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record SaveProductRequest(
        @NotBlank String name,
        String imageUrl,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        @NotNull @DecimalMin("0.0") Double weight,
        boolean isForSale,
        @NotBlank String categoryName,
        Set<UUID> factoryIds,
        Long version // optimistic locking
) {}