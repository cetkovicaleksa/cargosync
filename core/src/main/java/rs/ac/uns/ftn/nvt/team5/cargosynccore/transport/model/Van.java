package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.io.Serializable;

import static jakarta.persistence.FetchType.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "vans",
        indexes = {
                @Index(name = "idx_vans_wmi", columnList = "wmi")
        }
)
public class Van {

    private @EmbeddedId VanPK id;

    @MapsId("model")
    @ManyToOne(fetch = LAZY)
    @JoinColumns({
            @JoinColumn(name = "wmi", referencedColumnName = "world_manufacturer_identifier"),
            @JoinColumn(name = "vd", referencedColumnName = "vehicle_descriptor")
    })
    private Model model;

    @NotNull
    @Nationalized
    @Column(name = "registration_number", nullable = false, length = 15)
    private String registrationNumber;

    @Positive
    @Column(name = "max_payload_kg")
    private int maxPayload;

    @Transient
    public String getVin() {
        return "%s-%s-%s".formatted(id.model.wmi(), id.model.vd(), id.vi);
    }

    @Embeddable
    public record VanPK(
            @Embedded
            Model.ModelPK model,

            @Column(name = "vi", nullable = false)
            String vi
    ) implements Serializable {
        public VanPK(String wmi, String vd, String vi) {
            this(new Model.ModelPK(wmi, vd), vi);
        }
    }

}