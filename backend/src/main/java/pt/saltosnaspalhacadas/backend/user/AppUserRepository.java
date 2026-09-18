package pt.saltosnaspalhacadas.backend.user;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailAndActiveTrue(String email);

    List<AppUser> findAllByRoleAndActiveTrue(UserRole role);

    boolean existsByEmail(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByRole(UserRole role);
}
