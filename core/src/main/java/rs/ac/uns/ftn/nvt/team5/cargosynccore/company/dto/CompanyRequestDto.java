package rs.ac.uns.ftn.nvt.team5.cargosynccore.company.dto;

import lombok.Data;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.*;

import java.util.List;

import static rs.ac.uns.ftn.nvt.team5.cargosynccore.company.model.CompanyRequest.Status;

@Data
public class CompanyRequestDto {

    private String name, address;
    private CityDto city;
    private GeoLocationDto location;
    private Status status;
    private List<String> images, proofs;

}
