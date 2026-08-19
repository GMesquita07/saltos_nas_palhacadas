package pt.saltosnaspalhacadas.backend.auth;

import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import pt.saltosnaspalhacadas.backend.user.AppUser;

@Service
public class JwtService {
    private final SecretKey key; private final long expirationHours;
    public JwtService(@Value("${app.security.jwt.secret}") String secret, @Value("${app.security.jwt.expiration-hours}") long expirationHours) { this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)); this.expirationHours = expirationHours; }
    public String createToken(AppUser user) { Instant now = Instant.now(); return Jwts.builder().subject(user.getEmail()).claim("role", user.getRole().name()).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationHours * 3600))).signWith(key).compact(); }
    public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}
