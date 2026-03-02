package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto;

import java.util.UUID;

public record FactoryLookupDto (
        UUID id,
        String name,
        String street,
        String cityName,
        String countryName,
        String countryCode
) {}