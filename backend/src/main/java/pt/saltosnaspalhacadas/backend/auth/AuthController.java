package pt.saltosnaspalhacadas.backend.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import pt.saltosnaspalhacadas.backend.user.*;

@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
    private final AppUserRepository users; private final PasswordEncoder passwords; private final JwtService jwt;
    public AuthController(AppUserRepository users, PasswordEncoder passwords, JwtService jwt) { this.users = users; this.passwords = passwords; this.jwt = jwt; }
    @PostMapping("/login") TokenResponse login(@Valid @RequestBody LoginRequest request) { AppUser user = users.findByEmailAndActiveTrue(request.email().toLowerCase()).filter(candidate -> passwords.matches(request.password(), candidate.getPasswordHash())).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas")); return TokenResponse.from(user, jwt.createToken(user)); }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) TokenResponse register(@Valid @RequestBody RegisterRequest request) { if (users.findByEmailAndActiveTrue(request.email().toLowerCase()).isPresent()) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT, "Email já registado"); AppUser user = users.save(new AppUser(request.email(), passwords.encode(request.password()), UserRole.CUSTOMER)); return TokenResponse.from(user, jwt.createToken(user)); }
    record LoginRequest(@Email String email, @NotBlank String password) { } record RegisterRequest(@Email String email, @Size(min=12,max=128) String password) { } record TokenResponse(String accessToken, String tokenType, String role) { static TokenResponse from(AppUser user, String token) { return new TokenResponse(token, "Bearer", user.getRole().name()); } }
}
