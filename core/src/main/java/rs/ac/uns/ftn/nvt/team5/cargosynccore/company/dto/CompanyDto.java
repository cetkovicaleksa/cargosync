package rs.ac.uns.ftn.nvt.team5.cargosynccore.company.dto;

import lombok.Data;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.*;

import java.util.List;

@Data
public class CompanyDto {
    private UserDto owner;
    private String name, address;
    private CityDto city;
    private GeoLocationDto location;
    private List<String> images;
}
