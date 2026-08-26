package pt.saltosnaspalhacadas.backend.clientcontent;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.saltosnaspalhacadas.backend.portfolio.MediaType;

public interface ClientContentPostRepository extends JpaRepository<ClientContentPost, Long> {
    List<ClientContentPost> findAllByStatusOrderByEventDateDescIdDesc(ClientContentStatus status);
    List<ClientContentPost> findAllByStatusAndMediaTypeOrderByEventDateDescIdDesc(ClientContentStatus status, MediaType mediaType);
    List<ClientContentPost> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);
    List<ClientContentPost> findAllByOrderByCreatedAtDescIdDesc();
}
