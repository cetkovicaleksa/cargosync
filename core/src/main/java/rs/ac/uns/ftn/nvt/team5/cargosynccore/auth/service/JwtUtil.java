package rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.config.properties.JwtProperties;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtUtil {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    @Autowired
    public JwtUtil(JwtProperties properties) {
        this.properties = properties;

        byte[] secret = Hex.decode(properties.accessToken().secret());
        signingKey = Keys.hmacShaKeyFor(secret);
    }

    public String generateAccessToken(UserDetails userDetails) {
        return generateToken(userDetails, properties.accessToken().ttl());
    }

    private String generateToken(UserDetails userDetails, Duration expiration) {
        var now = new Date();
        var notAfter = new Date(now.getTime() + expiration.toMillis());
        var authorities = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toArray();

        return Jwts.builder()
                .issuer(properties.issuer())
                .issuedAt(now)
                .subject(userDetails.getUsername())
//                .claim("roles", authorities)
                .claim("authorities", authorities)
                .expiration(notAfter)
                .signWith(signingKey)
                .compact();
    }

    @Nullable
    public String extractUsername(String jwt) {
        try {
            return extractClaims(jwt).getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isTokenValid(String jwt) {
        try {
            var claims = extractClaims(jwt);
            return claims.getExpiration() != null && new Date().before(claims.getExpiration());
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    private Claims extractClaims(String jwt) {
        return Jwts.parser()
                .requireIssuer(properties.issuer())
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }

}
