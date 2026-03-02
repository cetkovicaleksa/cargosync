package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto;

import java.time.LocalDateTime;

public record FactoryInventoryDto(
    Long totalProduced,
    LocalDateTime lastProductionTime
) {}
