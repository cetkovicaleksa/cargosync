package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model;

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
@ToString
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

        cities.forEach(c -> c.setCountry(this));
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
    public boolean equals(Object o) {
        if (!(o instanceof Country country)) return false;
        return Objects.equals(code, country.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }

    @Override
    public int compareTo(Country o) {
        return code.compareToIgnoreCase(o.code);
    }

}