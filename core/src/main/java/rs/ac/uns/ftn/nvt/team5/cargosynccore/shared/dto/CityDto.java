package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Country;

import java.io.Serializable;
import java.util.Optional;

@Data
public class CityDto implements Serializable {

    @Pattern(regexp = "^[A-Z]{2}$")
    String countryCode;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    String countryName;

    @NotNull
    @Pattern(regexp = "^[A-Z2-9]{3}$")
    String code;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    String name;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    String locode;

    @JsonIgnore
    public Country getCountry() {
        return new Country(countryCode, countryName);
    }

    @JsonIgnore
    public void setCountry(Country country) {
        countryCode = Optional.ofNullable(country).map(Country::getCode).orElse(null);
        countryName = Optional.ofNullable(country).map(Country::getName).orElse(null);
    }

}
