package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Setter
@ToString(exclude = {"country"})
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(name = "cities")
@IdClass(City.CityPK.class)
public class City implements Comparable<City> {

    @Id
    @ManyToOne
    @JoinColumn(name = "country_code", nullable = false, updatable = false)
    private Country country;

    @Id
    @Pattern(regexp = "^[A-Z2-9]{3}$")
    @Column(name = "code", nullable = false, updatable = false, length = 3)
    private String code;

    @NotBlank
    @Column(name = "name")
    private String name;


    public City(String code, String name) {
        this(null, code, name);
    }

    public City(Country country, String code, String name) {
        this.country = country;
        this.code = code;
        this.name = name;

        if (country == null)
            return;

        if (country.getCities().containsKey(code))
            throw new IllegalArgumentException("Country already contains city: " + code);

        country.getCities().put(code, this);
    }


    @ToString.Include
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

    protected record CityPK(Country country, String code) implements Serializable {}

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof City city)) return false;
        return Objects.equals(country, city.country) && Objects.equals(code, city.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(country, code);
    }

}