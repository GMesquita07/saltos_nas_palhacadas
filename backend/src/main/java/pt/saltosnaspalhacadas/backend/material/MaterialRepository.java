package pt.saltosnaspalhacadas.backend.material;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findAllByOrderByDisplayOrderAscNameAscIdAsc();
}
