package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.jpa.domain.AbstractPersistable;

import java.util.UUID;

@Getter
@Setter
@ToString
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Entity
public abstract class User extends AbstractPersistable<UUID> {

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(name = "avatar", nullable = false)
    private String avatar;

}