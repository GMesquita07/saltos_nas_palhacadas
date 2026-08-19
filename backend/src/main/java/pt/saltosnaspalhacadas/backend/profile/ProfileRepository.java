package pt.saltosnaspalhacadas.backend.profile;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    List<Profile> findAllByActiveTrueOrderByNameAsc();
    Optional<Profile> findBySlugAndActiveTrue(String slug);
}
