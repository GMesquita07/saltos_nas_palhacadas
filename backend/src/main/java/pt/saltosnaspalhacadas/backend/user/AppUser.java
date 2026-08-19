package pt.saltosnaspalhacadas.backend.user;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 254) private String email;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private UserRole role;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected AppUser() { }
    public AppUser(String email, String passwordHash, UserRole role) { this.email = email.toLowerCase(); this.passwordHash = passwordHash; this.role = role; }
    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; } public String getEmail() { return email; } public String getPasswordHash() { return passwordHash; } public UserRole getRole() { return role; } public boolean isActive() { return active; }
}
