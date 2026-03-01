package rs.ac.uns.ftn.nvt.team5.cargosynccore.company.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.CityDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.GeoLocationDto;

import java.util.List;

@Data
public class CompanyRequestForm {

    @NotBlank
    private String name;

    @Valid
    @NotNull
    private CityDto city;

    @NotBlank
    private String address;

    @Valid
    @NotNull
    private GeoLocationDto location;

    @NotNull
    @Size(min = 1, max = 32)
    private List<MultipartFile> images;

    @NotNull
    @Size(min = 1, max = 32)
    private List<MultipartFile> proofs;

}
