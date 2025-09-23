package com.example.botforconsultations.core.service;



import com.example.botforconsultations.core.exception.AuthenticationException;
import com.example.botforconsultations.core.model.AdminUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Instant;
import java.util.Date;


@Service
public class JwtProvider {

    private final SecretKey jwtSecret;

    @Getter
    private final long jwtExpiration;


    public JwtProvider(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration}") long jwtExpiration
    ) {
        this.jwtSecret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.jwtExpiration = jwtExpiration;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException expEx) {
            //log.error("Token expired", expEx);
            throw new AuthenticationException("Token expired");
        } catch (UnsupportedJwtException unsEx) {
            //log.error("Unsupported jwt", unsEx);
            throw new AuthenticationException("Unsupported jwt");
        } catch (MalformedJwtException mjEx) {
            //log.error("Malformed jwt", mjEx);
            throw new AuthenticationException("Malformed jwt");
        } catch (SignatureException sEx) {
            //log.error("Invalid signature", sEx);
            throw new AuthenticationException("Invalid signature");
        } catch (Exception e) {
            //log.error("invalid token", e);
            throw new AuthenticationException("invalid token");
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getLoginFromAuthHeader(String authHeader) {
        Claims claims = getClaims(getTokenFromAuthHeader(authHeader));
        return claims.getSubject();
    }

    public Long getUserIdFromAuthHeader(String authHeader) {
        Claims claims = getClaims(getTokenFromAuthHeader(authHeader));
        return Long.parseLong(claims.get("userId").toString());
    }

    public String getTokenFromAuthHeader(String authHeader) {
        if (authHeader == null || authHeader.isEmpty())
            throw new AuthenticationException("Not contain Authorization header");
        String token = null;
        if (authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        return token;
    }

    public String generateToken(AdminUser user) {
        final Date expiration = Date.from(Instant.now().plusSeconds(jwtExpiration));

        return Jwts.builder()
                .setSubject(user.getLogin())
                .setExpiration(expiration)
                .signWith(jwtSecret)
                .claim("userId", user.getId().toString())
                .claim("role", user.getRole())
                .compact();
    }

}