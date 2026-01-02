package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import static jakarta.persistence.FetchType.*;
import static org.hibernate.annotations.UuidGenerator.Style.TIME;

@Getter
@Setter
@Entity
@NoArgsConstructor
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
    @Column(name = "wmi", insertable = false, updatable = false, length = 3)
    private String wmi;

    @Id
    @Column(name = "vd", insertable = false, updatable = false, length = 6)
    private String vd;

    @Id
    @UuidGenerator(style = TIME)
    @Column(name = "vi", nullable = false)
    private String vi;

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

    @Transient
    public String getVin() {
        return String.join("-", wmi, vd, vi);
    }

    public record VanPK(String wmi, String vd, String vi) implements Serializable {}

}