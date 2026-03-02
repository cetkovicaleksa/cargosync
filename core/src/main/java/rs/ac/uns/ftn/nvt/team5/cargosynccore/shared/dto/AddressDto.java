package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.City;

import java.io.Serializable;

/**
 * DTO for rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Address
 */
@Deprecated
@Data
public class AddressDto {

    String countryCode;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    String countryName;

    String cityCode;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    String cityName;

    @NotBlank String street;

}