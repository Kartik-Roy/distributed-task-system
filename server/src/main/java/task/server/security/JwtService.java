package task.server.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.issuer}")
    private String issuer;
    @Value("${app.jwt.secret}")
    private byte[] keyBytes;
    @Value("${app.jwt.ttlSeconds}")
    private long ttlSeconds;

    public String mintNodeToken(String nodeId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(nodeId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(ttlSeconds)))
                .claim("typ", "node")
                .signWith(Keys.hmacShaKeyFor(keyBytes), SignatureAlgorithm.HS256)
                .compact();
    }

    public String parseNodeId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(keyBytes))
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token)
                .getBody();

        if (!"node".equals(claims.get("typ", String.class))) {
            throw new JwtException("Invalid token type");
        }
        return claims.getSubject();
    }

    public String mintUserToken(String adminId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(adminId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(ttlSeconds)))
                .claim("typ", role)
                .claim("role", role)
                .signWith(Keys.hmacShaKeyFor(keyBytes), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(keyBytes))
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


}

