package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto;

import lombok.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.GeoLocationDto;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FactoryDto {
    private UUID id;
    private String name;
    private GeoLocationDto location;
    private String street;
    private String cityCode;
    private String cityName;
    private String countryCode;
    private String countryName;
    private Set<String> imageUrls;
    private Long version;
}