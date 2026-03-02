package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DTO for {@link rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Country}
 */
@Data
public class CountryDto implements Serializable {

        @NotNull
        @Pattern(regexp = "^[A-Z]{2}$")
        String code;

        String name;

        @JsonIgnore
        Map<String, CityDto> cities;

        @JsonProperty("cities")
        public List<CityDto> getCities4json() {
            return cities.values().stream().sorted().toList();
        }

        @JsonProperty("cities")
        public void setCities4json(@Nullable List<CityDto> cities) {
            this.cities = Optional.ofNullable(cities).orElseGet(List::of).stream()
                    .collect(Collectors.toMap(CityDto::getCode, Function.identity()));
        }

}