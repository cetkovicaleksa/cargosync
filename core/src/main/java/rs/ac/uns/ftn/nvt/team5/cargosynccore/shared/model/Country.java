package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Setter
@ToString(exclude = "cities")
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(name = "countries")
public class Country implements Comparable<Country> {

    /** ISO 3166-1 alpha-2 country code.*/
    @Id
    @Pattern(regexp = "^[A-Z]{2}$")
    @Column(name = "code", nullable = false, updatable = false, length = 2)
    private String code;

    @NotBlank
    @Column(name = "name")
    private String name;

    @JsonIgnore
    @MapKey(name = "code")
    @OneToMany(mappedBy = "country", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Map<String, City> cities = new HashMap<>();


    public Country(String code, String name) {
        this(code, name, List.of());
    }

    public Country(String code, String name, Collection<? extends City> cities) {
        this.code = code;
        this.name = name;
        this.cities = cities.stream()
                .collect(Collectors.toMap(City::getCode, Function.identity()));
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
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (this == obj)
            return true;

        if (obj instanceof Country other)
            return Objects.equals(code, other.code);

        return false;
    }

    @Override
    public int hashCode() {

        int hashCode = 17;

        hashCode += null == code ? 0 : code.hashCode() * 31;

        return hashCode;
    }

    @Override
    public int compareTo(Country o) {
        return code.compareToIgnoreCase(o.code);
    }

    //region Should've made a dto

    @JsonProperty("cities")
    @Transient
    private Object getCities4Json() {
        return cities.values().stream().sorted();
    }

    //endregion

}