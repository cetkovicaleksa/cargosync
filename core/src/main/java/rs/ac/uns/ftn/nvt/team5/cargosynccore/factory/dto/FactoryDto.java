package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto;

import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.GeoLocationDto;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record FactoryDto(
        UUID id,
        String name,
        GeoLocationDto location,
        String street,
        String cityCode,
        String cityName,
        String countryCode,
        String countryName,
        Set<String> imageUrls,
        boolean isOnline,
        LocalDateTime lastHeartbeat,
        Long version
) {}