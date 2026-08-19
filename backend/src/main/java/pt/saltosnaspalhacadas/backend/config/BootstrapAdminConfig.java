package pt.saltosnaspalhacadas.backend.config;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import pt.saltosnaspalhacadas.backend.user.*;
@Configuration public class BootstrapAdminConfig {
 @Bean CommandLineRunner bootstrapAdmin(AppUserRepository users, PasswordEncoder passwords, @Value("${app.bootstrap.admin.email}") String email, @Value("${app.bootstrap.admin.password}") String password) { return args -> { if (!users.existsByRole(UserRole.ADMIN)) users.save(new AppUser(email, passwords.encode(password), UserRole.ADMIN)); }; }
 @Bean PasswordEncoder passwordEncoder() { return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(); }
 @Bean UserDetailsService userDetailsService(AppUserRepository users) { return email -> users.findByEmailAndActiveTrue(email).map(user -> User.withUsername(user.getEmail()).password(user.getPasswordHash()).roles(user.getRole().name()).build()).orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("Utilizador não encontrado")); }
}
