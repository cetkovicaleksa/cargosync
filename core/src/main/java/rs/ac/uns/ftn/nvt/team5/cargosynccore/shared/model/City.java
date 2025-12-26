package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.data.jpa.domain.AbstractPersistable;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Setter
@ToString
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor
@Entity
@Table(
        name = "cities",
        uniqueConstraints = {
                @UniqueConstraint(name = "uc_cities_code_country_code", columnNames = {"code", "country_code"})
        }
)
public class City extends AbstractPersistable<UUID> implements Comparable<City> {

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "country_code", nullable = false)
    private Country country;

    @NotNull
    @Pattern(regexp = "^[A-Z2-9]{3}$")
    @Column(name = "code", nullable = false, updatable = false, length = 3)
    private String code;

    @NotBlank
    @Column(name = "name")
    private String name;

    @Transient
    public String getLocode() {
        return "%s %s".formatted(country.getCode(), name);
    }

    @PrePersist
    @PreUpdate
    private void normalizeCode() {
        if (code != null) {
            code = code.replaceAll("\\s+", "")
                    .toUpperCase(Locale.ROOT);
        }
    }

    @Override
    public int compareTo(@NotNull City o) {
        return code.compareToIgnoreCase(o.code);
    }

    //region Should've made a dto

    @JsonProperty("countryCode")
    @Transient
    private String getCountryCode() {
        return Optional.ofNullable(country)
                .map(Country::getCode)
                .orElse(null);
    }

    //endregion

}