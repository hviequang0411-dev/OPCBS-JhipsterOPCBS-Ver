package com.ocbs.repository;

import com.ocbs.domain.BlogComment;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository cho BlogComment.
 */
@Repository
public interface BlogCommentRepository extends JpaRepository<BlogComment, Long> {

    List<BlogComment> findByBlogPost_IdAndIsApprovedTrueOrderByCreatedDateDesc(Long blogPostId);

    long countByBlogPost_IdAndIsApprovedTrue(Long blogPostId);
}
