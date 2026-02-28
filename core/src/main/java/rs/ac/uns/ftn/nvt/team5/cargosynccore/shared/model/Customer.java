package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor(access = PROTECTED)
@Entity
public class Customer extends User {

    @Column
    private String firstName;

    @Column
    private String lastName;

    public Customer(String email, String avatar) {
        super(email, avatar);
    }

    public Customer(String email, String avatar, String firstName, String lastName) {
        super(email, avatar);
        this.firstName = firstName;
        this.lastName = lastName;
    }

}