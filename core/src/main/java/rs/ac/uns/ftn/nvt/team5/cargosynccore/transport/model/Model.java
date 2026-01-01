package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Nationalized;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import static jakarta.persistence.FetchType.LAZY;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"make"})
@Entity
@Table(name = "van_models")
public class Model {

    private @EmbeddedId ModelPK id;

    @MapsId("wmi")
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "world_manufacturer_identifier")
    private Make make;

    @NotBlank
    @Nationalized
    @Column(name = "model_name", nullable = false)
    private String name;


    public Model(String wmi, String vd, String name) {
        this(new Make(wmi, null, null), vd, name);
    }

    public Model(Make make, String vd, String name) {
        this.make = Objects.requireNonNull(make);
        this.id = new ModelPK(make.getWmi(), vd);
        this.name = name;
    }


    @Transient
    public String getVd() {
        return Optional.ofNullable(id)
                .map(ModelPK::vd)
                .orElse(null);
    }


    @Embeddable
    public record ModelPK(

            @Column(nullable = false, length = 3)
            String wmi,

            @NotNull
            @Pattern(regexp = "[0-9A-HJ-NPR-Z]{6}")
            @Column(name = "vehicle_descriptor", nullable = false, length = 6)
            String vd

    ) implements Serializable {}


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Model model)) return false;
        return Objects.equals(id, model.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

}