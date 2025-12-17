package rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;

    public String login(String username, String password) {
        var auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        var userDetails = (UserDetails) auth.getPrincipal();
        return jwtUtil.generateAccessToken(userDetails);
    }

}
