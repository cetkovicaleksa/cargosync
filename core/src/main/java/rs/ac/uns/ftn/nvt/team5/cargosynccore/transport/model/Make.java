package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.util.*;

import static jakarta.persistence.CascadeType.ALL;

/** @see <a href="https://cdn.standards.iteh.ai/samples/52200/7d8a69aee84c4ad28231053f49f4966e/ISO-3779-2009.pdf">ISO-3779-2009.pdf</a>*/

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"models"})
@Entity
@Table(name = "van_makes")
public class Make {

    @NotNull
    @Pattern(regexp = "[0-9A-HJ-NPR-Z]{3}")
    @Column(name = "world_manufacturer_identifier", nullable = false, length = 3)
    private @Id String wmi;

    @NotBlank
    @Nationalized
    @Column(name = "manufacturer_name")
    private String name;

    @OrderBy("id.vd ASC")
    @OneToMany(mappedBy = "make", orphanRemoval = true, cascade = {ALL})
    private List<Model> models;


    public Make(String wmi) {
        this.wmi = wmi;
    }


    @PrePersist
    protected void prePersist() {
        (models == null
                ? models = new ArrayList<>()
                : models
        ).forEach(model -> model.setMake(this));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Make make)) return false;
        return Objects.equals(wmi, make.wmi);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(wmi);
    }

}