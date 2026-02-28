package rs.ac.uns.ftn.nvt.team5.cargosynccore.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(
        UUID id,
        UUID productId,
        String productName,
        String productImageUrl,
        Long quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
