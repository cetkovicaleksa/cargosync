package rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto;

import java.util.UUID;

public record ProductSummaryDto(
        UUID id,
        String name,
        String categoryName,
        String imageUrl
) {}
