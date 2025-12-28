package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;


@Data
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor
@Embeddable
public class Address {

    @NotNull
    @ManyToOne
    private City city;

    @NotBlank
    private String street;

    // NOTE: Maybe add street number, etc.

}
