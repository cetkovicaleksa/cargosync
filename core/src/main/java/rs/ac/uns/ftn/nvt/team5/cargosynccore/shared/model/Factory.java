package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "factories")
public class Factory extends AbstractAuditable {

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Embedded
    private GeoLocation location;

    @Version
    private Long version;

    @ManyToMany(mappedBy = "factories")
    private Set<Product> products = new HashSet<>();
}