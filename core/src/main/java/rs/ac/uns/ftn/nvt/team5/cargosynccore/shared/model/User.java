package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;


import static lombok.AccessLevel.PROTECTED;

@Getter
@Setter
@ToString
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PROTECTED)
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Entity
public abstract class User extends AbstractPersistable {

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "avatar")
    private String avatar;

}