package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stub entity
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "companies")
public class Company extends AbstractAuditable {

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column
    private String address;

    public Company(String name, String address) {
        this.name = name;
        this.address = address;
    }
}
