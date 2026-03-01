package rs.ac.uns.ftn.nvt.team5.cargosynccore.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderItemRequest(
        @NotNull UUID productId,
        @NotNull @Min(1) Long quantity
) {}
