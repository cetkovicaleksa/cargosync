package rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.dto;

import lombok.Data;
import lombok.Value;
import org.springframework.security.core.GrantedAuthority;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.AccountRole;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.UserDto;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for {@link rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.Account}
 */
@Data
public class AccountDto implements Serializable {
    String username;
    UserDto owner;
    AccountRole role;
    boolean requiresPasswordChange;
    List<GrantedAuthority> authorities;
}