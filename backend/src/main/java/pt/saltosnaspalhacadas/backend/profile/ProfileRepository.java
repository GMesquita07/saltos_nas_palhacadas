package pt.saltosnaspalhacadas.backend.profile;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    List<Profile> findAllByActiveTrueOrderByNameAsc();
    Optional<Profile> findBySlugAndActiveTrue(String slug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from Profile profile where profile.id = :id and profile.active = true")
    Optional<Profile> findByIdAndActiveTrueForUpdate(@Param("id") Long id);
}
