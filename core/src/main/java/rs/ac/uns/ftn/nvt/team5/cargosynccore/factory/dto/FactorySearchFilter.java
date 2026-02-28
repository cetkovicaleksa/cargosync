package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto;

public record FactorySearchFilter(
        String name,
        String countryCode,
        String cityCode,
        Boolean isOnline
) {}