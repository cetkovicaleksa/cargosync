package rs.ac.uns.ftn.nvt.team5.cargosynccore.order.dto;

import rs.ac.uns.ftn.nvt.team5.cargosynccore.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDto(
        UUID id,
        UUID companyId,
        String companyName,
        String companyAddress,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemDto> items,
        LocalDateTime createdDate
) {}
