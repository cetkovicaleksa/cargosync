package rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.repository.AccountRepository;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public String login(String username, String password) {
        var auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        var userDetails = (UserDetails) auth.getPrincipal();
        return jwtUtil.generateAccessToken(userDetails);
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        var account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(oldPassword, account.getPassword())) {
            throw new BadCredentialsException("Old password does not match");
        }

        account.setPassword(passwordEncoder.encode(newPassword));
        account.setRequiresPasswordChange(false);
        accountRepository.save(account);
    }

}
