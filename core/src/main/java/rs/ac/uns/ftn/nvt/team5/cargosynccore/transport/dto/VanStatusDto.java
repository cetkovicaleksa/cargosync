package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto;

import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.GeoLocationDto;

import java.time.Instant;

public record VanStatusDto(
        String vin,
        boolean online,
        Instant lastHeartbeat,
        GeoLocationDto lastSeenAt
) {}