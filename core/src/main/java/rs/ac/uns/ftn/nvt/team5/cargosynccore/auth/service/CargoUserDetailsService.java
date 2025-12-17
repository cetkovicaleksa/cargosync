package rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.service;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.repository.AccountRepository;

@RequiredArgsConstructor
@Service
class CargoUserDetailsService implements UserDetailsService {

    private final AccountRepository accounts;

    @Nonnull
    @Override
    public UserDetails loadUserByUsername(@Nonnull String username) throws UsernameNotFoundException {
        return accounts.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User '%s' not found".formatted(username)));
    }

}
