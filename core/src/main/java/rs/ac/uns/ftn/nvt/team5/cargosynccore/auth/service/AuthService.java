package rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.Account;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.repository.AccountRepository;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public String login(String username, String password) {
        Authentication auth;
        try {
            auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (BadCredentialsException e) {
            // In case username auth failed, try with email
            Account account = accountRepository.findByEmail(username).orElseThrow(() -> e);
            username = account.getUsername();
            auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        }

        var userDetails = (UserDetails) auth.getPrincipal();
        return jwtUtil.generateAccessToken(userDetails);
    }

    public String changePassword(String username, String oldPassword, String newPassword) {
        Authentication auth;
        Optional<Account> account = accountRepository.findByUsername(username);
        if (account.isEmpty())
            account = accountRepository.findByEmail(username);
        if (account.isEmpty())
            throw new UsernameNotFoundException("User not found");

        Account acc = account.get();
        if (!passwordEncoder.matches(oldPassword, acc.getPassword())) {
            throw new BadCredentialsException("Old password does not match");
        }
        acc.setPassword(passwordEncoder.encode(newPassword)); // newPassword is not null
        acc.setCredentialsNonExpired(true);

        accountRepository.save(acc);
        return jwtUtil.generateAccessToken(acc);
    }

}
