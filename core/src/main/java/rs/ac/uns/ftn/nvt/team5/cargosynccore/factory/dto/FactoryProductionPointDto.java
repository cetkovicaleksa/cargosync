package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto;

import java.time.Instant;

public record FactoryProductionPointDto(
        Instant periodStart,
        Instant periodEnd,
        long units
) {}
