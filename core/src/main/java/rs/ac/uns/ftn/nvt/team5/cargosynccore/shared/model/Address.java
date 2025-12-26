package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

import static lombok.AccessLevel.PROTECTED;


@Data
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor
@Embeddable
public class Address {

    @JsonIgnore
    @NotNull
    @ManyToOne
    private City city;

    @NotBlank
    private String street;

    // NOTE: Maybe add street number, etc.

    //region Should've made a dto

    @JsonProperty("city")
    @Transient
    private String getCityCode() {
        return Optional.ofNullable(city)
                .map(City::getCode)
                .orElse(null);
    }

    @JsonProperty("country")
    @Transient
    private String getCountryCode() {
        return Optional.ofNullable(city)
                .map(City::getCountry)
                .map(Country::getCode)
                .orElse(null);
    }

    //endregion

}
