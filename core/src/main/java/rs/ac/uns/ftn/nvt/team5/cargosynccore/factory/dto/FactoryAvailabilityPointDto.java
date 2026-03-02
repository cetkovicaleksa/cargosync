package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto;

import java.time.Instant;

public record FactoryAvailabilityPointDto(
        Instant periodStart,
        Instant periodEnd,
        long onlineSeconds,
        long offlineSeconds,
        double onlinePercent,
        double offlinePercent
) {}
