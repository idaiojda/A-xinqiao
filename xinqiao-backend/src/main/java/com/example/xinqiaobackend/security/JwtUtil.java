package com.example.xinqiaobackend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;  
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-seconds}") long expSeconds) {
        byte[] raw;
        if (secret != null && secret.startsWith("base64:")) {
            String b64 = secret.substring("base64:".length());
            raw = Base64.getDecoder().decode(b64);
        } else {
            raw = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        }
        if (raw.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes (256 bits) per RFC 7518. Set env JWT_SECRET or app.jwt.secret to a longer or base64 value.");
        }
        this.key = Keys.hmacShaKeyFor(raw);
        this.expirationMs = expSeconds * 1000L;
    }

    public String generateToken(String username, List<String> roles) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
}
