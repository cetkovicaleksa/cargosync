package rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
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
        Long version, // optimistic locking
        Long totalStock  // null for manager CRUD responses, populated for customer listing
) {}