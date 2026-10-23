package com.ocbs.repository;

import com.ocbs.domain.BlogPost;
import com.ocbs.domain.enumeration.BlogStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository cho BlogPost.
 * Tất cả tính toán và lọc dữ liệu được thực thi trực tiếp ở tầng Database.
 */
@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    @Query("SELECT b FROM BlogPost b JOIN FETCH b.doctor d JOIN FETCH d.user u WHERE b.id = :id")
    Optional<BlogPost> findByIdWithDoctor(@Param("id") Long id);

    @Query(
        value = "SELECT b FROM BlogPost b JOIN FETCH b.doctor d JOIN FETCH d.user u WHERE b.status = :status AND (:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.excerpt) LIKE LOWER(CONCAT('%', :keyword, '%')))",
        countQuery = "SELECT COUNT(b) FROM BlogPost b WHERE b.status = :status AND (:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.excerpt) LIKE LOWER(CONCAT('%', :keyword, '%')))"
    )
    Page<BlogPost> findByStatusAndKeyword(@Param("status") BlogStatus status, @Param("keyword") String keyword, Pageable pageable);

    List<BlogPost> findByDoctor_IdOrderByCreatedDateDesc(Long doctorId);

    List<BlogPost> findByStatusOrderBySubmittedAtDesc(BlogStatus status);

    long countByStatus(BlogStatus status);

    long countByDoctor_Id(Long doctorId);

    @Modifying
    @Query("UPDATE BlogPost b SET b.viewCount = b.viewCount + 1 WHERE b.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
