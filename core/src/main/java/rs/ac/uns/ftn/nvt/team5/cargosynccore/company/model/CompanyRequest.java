package rs.ac.uns.ftn.nvt.team5.cargosynccore.company.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.*;

import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@ToString
@Table(
        name = "company_requests",
        indexes = {
                @Index(name = "idx_company_requests_status", columnList = "status")
        }
)
public class CompanyRequest extends AbstractAuditable {

    @NotBlank
    private String name;

    @NotNull
    @ManyToOne
    private City city;

    @NotBlank
    private String address;

    @Valid
    @Embedded
    private GeoLocation location;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @Size(min = 1, max = 32)
    @ElementCollection
    private List<@NotBlank String> images;

    @Size(min = 1, max = 32)
    @ElementCollection
    private List<@NotBlank String> proofs;

    @Version
    @Column(name = "revision")
    private int version;

    public enum Status {
        PENDING, APPROVED, REJECTED
    }

}
