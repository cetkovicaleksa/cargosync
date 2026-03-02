package rs.ac.uns.ftn.nvt.team5.cargosynccore.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID companyId,
        @NotEmpty List<@Valid OrderItemRequest> items
) {}
