package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto;

import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.Factory;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.GeoLocationDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.City;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.CityRepository;

import java.util.HashSet;
import java.util.Set;

public class FactoryMapper {
    private FactoryMapper() {}
    public static FactoryDto mapToDto(Factory factory, CityRepository cityRepository) {
        City city = cityRepository.findByCountryCodeAndCode(factory.getCountryCode(), factory.getCityCode())
                .orElseThrow(() -> new IllegalStateException("City missing"));
        GeoLocationDto geoDto = new GeoLocationDto();
        geoDto.setLatitude(factory.getLocation().getLatitude());
        geoDto.setLongitude(factory.getLocation().getLongitude());

        Set<String> images = (factory.getImageUrls() != null)
                ? new HashSet<>(factory.getImageUrls())
                : new HashSet<>();

        return new FactoryDto(
                factory.getId(),
                factory.getName(),
                geoDto,
                factory.getStreet(),
                city.getCode(),
                city.getName(),
                city.getCountry().getCode(),
                city.getCountry().getName(),
                images,
                factory.getVersion()
        );
    }


    public static FactoryLookupDto mapToLookupDto(Factory factory, CityRepository cityRepository) {
        City city = cityRepository.findByCountryCodeAndCode(factory.getCountryCode(), factory.getCityCode())
                .orElseThrow(() -> new IllegalStateException("City missing"));

        return new FactoryLookupDto(
                factory.getId(),
                factory.getName(),
                factory.getStreet(),
                city.getName(),
                city.getCountry().getName(),
                city.getCountry().getCode()
        );
    }
}
