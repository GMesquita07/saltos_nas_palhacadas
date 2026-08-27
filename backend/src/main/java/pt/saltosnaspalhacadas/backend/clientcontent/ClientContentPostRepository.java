package pt.saltosnaspalhacadas.backend.clientcontent;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.saltosnaspalhacadas.backend.portfolio.MediaType;

public interface ClientContentPostRepository extends JpaRepository<ClientContentPost, Long> {
    List<ClientContentPost> findAllByStatusOrderByEventDateDescIdDesc(ClientContentStatus status);
    List<ClientContentPost> findAllByStatusAndMediaTypeOrderByEventDateDescIdDesc(ClientContentStatus status, MediaType mediaType);
    List<ClientContentPost> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);
    List<ClientContentPost> findAllByOrderByCreatedAtDescIdDesc();

    @Query("""
            select count(post) > 0
            from ClientContentPost post
            where (
                post.mediaUrl like concat('%', :privateMediaPath)
                or post.thumbnailUrl like concat('%', :privateMediaPath)
            )
            and (:admin = true or post.user.id = :userId)
            """)
    boolean existsVisibleLegacyPrivateMedia(
            @Param("privateMediaPath") String privateMediaPath,
            @Param("userId") Long userId,
            @Param("admin") boolean admin);
}
