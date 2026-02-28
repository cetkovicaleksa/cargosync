package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.GeoLocationDto;

import java.util.Set;

public record SaveFactoryRequest(
        @NotBlank String name,
        @NotNull @Valid GeoLocationDto location,
        @NotBlank String street,
        @NotBlank String cityCode,
        @NotBlank String countryCode,
        Set<String> imageUrls,
        Long version
) {}
