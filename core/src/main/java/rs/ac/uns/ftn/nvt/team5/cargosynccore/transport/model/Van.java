package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;

import static jakarta.persistence.FetchType.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "model")
@Table(
        name = "vans",
        indexes = {
                @Index(name = "idx_vans_wmi", columnList = "wmi")
        }
)
@IdClass(Van.VanPK.class)
public class Van {

    @Id
    @UuidGenerator
    @Column(name = "vi", nullable = false)
    private String vi;

    @Id
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

    @Version
    @Column(name = "revision")
    private int version;


    public Van(String vi, Model model) {
        this.vi = vi;
        this.model = model;
    }


    @Transient
    public String getVin() {
        return String.join("-", model.getId().wmi(), model.getId().vd(), vi);
    }

    public record VanPK(String vi, Model model) implements Serializable {
        public VanPK(String wmi, String vd, String vi) {
            this(vi, new Model(wmi, vd, null));
        }
    }

}