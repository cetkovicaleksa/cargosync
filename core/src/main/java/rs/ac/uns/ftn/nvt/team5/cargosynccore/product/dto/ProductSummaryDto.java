package rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public record ProductSummaryDto(
        UUID id,
        String name,
        String categoryName,
        String imageUrl
) {}
