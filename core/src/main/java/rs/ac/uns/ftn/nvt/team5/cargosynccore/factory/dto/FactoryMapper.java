package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto;

import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.GeoLocationDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.Factory;

public class FactoryMapper {
    private FactoryMapper() {}

    public static FactoryDto mapToDto(Factory factory) {
        GeoLocationDto geoDto = new GeoLocationDto();
        geoDto.setLatitude(factory.getLocation().getLatitude());
        geoDto.setLongitude(factory.getLocation().getLongitude());

        return new FactoryDto(
                factory.getId(),
                factory.getName(),
                geoDto,
                factory.getStreet(),
                factory.getCity().getCode(),
                factory.getCity().getName(),
                factory.getCity().getCountry().getCode(),
                factory.getCity().getCountry().getName(),
                factory.getImageUrls(),
                factory.isOnline(),
                factory.getLastHeartbeat(),
                factory.getVersion()
        );
    }
}
