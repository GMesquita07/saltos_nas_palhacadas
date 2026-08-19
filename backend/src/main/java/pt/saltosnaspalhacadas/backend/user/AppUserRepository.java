package pt.saltosnaspalhacadas.backend.user;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AppUserRepository extends JpaRepository<AppUser, Long> { Optional<AppUser> findByEmailAndActiveTrue(String email); boolean existsByRole(UserRole role); }
