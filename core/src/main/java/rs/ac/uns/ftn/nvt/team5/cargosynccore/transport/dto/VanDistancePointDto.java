package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto;

import java.time.Instant;

public record VanDistancePointDto(
        Instant periodStart,
        Instant periodEnd,
        double distance
) {}