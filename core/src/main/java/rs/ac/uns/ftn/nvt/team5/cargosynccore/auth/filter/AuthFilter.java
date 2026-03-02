package rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.filter;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.service.JwtUtil;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.service.TokenBlacklistService;

import java.io.IOException;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class AuthFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklist;

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain
    ) throws ServletException, IOException
    {
        Optional<String> token = Optional.ofNullable(request.getHeader("Authorization"))
                .filter(authHeader -> authHeader.startsWith("Bearer "))
                .map(authHeader -> authHeader.substring("Bearer ".length()));

        if (token.isEmpty() && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    token = Optional.of(cookie.getValue());
                    break;
                }
            }
        }

        token.filter(jwtUtil::isTokenValid)
                .filter(t -> !tokenBlacklist.isBlacklisted(t))
                .map(jwtUtil::extractUsername)
                .map(userDetailsService::loadUserByUsername)
                .ifPresent(userDetails -> {
                    var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(auth);
                });

        filterChain.doFilter(request, response);
    }

}
