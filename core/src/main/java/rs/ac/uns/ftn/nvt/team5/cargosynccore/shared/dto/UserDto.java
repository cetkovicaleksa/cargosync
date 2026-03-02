package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.User}
 */
@Data
public class UserDto implements Serializable {
    String email;
    String avatar;
}