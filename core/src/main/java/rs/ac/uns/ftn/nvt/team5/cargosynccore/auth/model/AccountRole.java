package rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model;

import jakarta.persistence.Transient;
import org.springframework.security.core.GrantedAuthority;

public enum AccountRole implements GrantedAuthority {
    CUSTOMER, MANAGER;

    @Transient
    public String getAuthority() {
        return "ROLE_" + name();
    }
}
