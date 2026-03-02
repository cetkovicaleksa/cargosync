package rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Redis-backed JWT blacklist.
 * On logout, the token is stored with a TTL equal to its remaining validity.
 * AuthFilter checks this before authenticating each request.
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    public void blacklist(String token) {
        Date expiration = jwtUtil.extractExpiration(token);
        if (expiration == null) return;

        Duration ttl = Duration.between(Instant.now(), expiration.toInstant());
        if (ttl.isNegative() || ttl.isZero()) return;

        redisTemplate.opsForValue().set(KEY_PREFIX + token, "1", ttl);
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + token));
    }
}
