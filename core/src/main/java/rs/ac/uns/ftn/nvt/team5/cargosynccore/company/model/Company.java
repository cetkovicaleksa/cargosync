package rs.ac.uns.ftn.nvt.team5.cargosynccore.company.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "companies")
public class Company extends AbstractPersistable {

    @NotNull
    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private Customer owner;

    @NotBlank
    private String name;

    @NotNull
    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "city_country_code", referencedColumnName = "country_code"),
        @JoinColumn(name = "city_code", referencedColumnName = "code")
    })
    private City city;

    @NotBlank
    private String address;

    @Valid
    @Embedded
    private GeoLocation location;

    @Size(min = 1, max = 32)
    @ElementCollection
    private List<@NotBlank String> images = new ArrayList<>();

}
