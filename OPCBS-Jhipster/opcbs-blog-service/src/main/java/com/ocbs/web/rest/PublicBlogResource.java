package com.ocbs.web.rest;

import com.ocbs.domain.BlogComment;
import com.ocbs.domain.BlogPost;
import com.ocbs.domain.PatientProfile;
import com.ocbs.domain.enumeration.BlogStatus;
import com.ocbs.repository.BlogCommentRepository;
import com.ocbs.repository.BlogPostRepository;
import com.ocbs.repository.PatientProfileRepository;
import com.ocbs.security.SecurityUtils;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller công khai cho Độc giả và Bệnh nhân đọc Blog y khoa.
 */
@RestController
@RequestMapping("/api/blogs")
@Transactional
public class PublicBlogResource {

    private static final Logger LOG = LoggerFactory.getLogger(PublicBlogResource.class);
    private static final String ENTITY_NAME = "blogPost";

    private final BlogPostRepository blogPostRepository;
    private final BlogCommentRepository blogCommentRepository;
    private final PatientProfileRepository patientProfileRepository;

    public PublicBlogResource(
        BlogPostRepository blogPostRepository,
        BlogCommentRepository blogCommentRepository,
        PatientProfileRepository patientProfileRepository
    ) {
        this.blogPostRepository = blogPostRepository;
        this.blogCommentRepository = blogCommentRepository;
        this.patientProfileRepository = patientProfileRepository;
    }

    /**
     * {@code GET /blogs} : Lấy danh sách các bài viết đã xuất bản công khai (PUBLISHED).
     * Hỗ trợ tìm kiếm từ khóa và phân trang xử lý trực tiếp ở tầng Database.
     */
    @GetMapping
    public ResponseEntity<Page<BlogPost>> getPublishedBlogs(
        @RequestParam(name = "keyword", required = false) String keyword,
        @PageableDefault(size = 9, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<BlogPost> page = blogPostRepository.findByStatusAndKeyword(BlogStatus.PUBLISHED, keyword, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * {@code GET /blogs/:id} : Xem chi tiết một bài viết và tự động tăng số lượt xem ở tầng Database.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BlogPost> getBlogDetails(@PathVariable("id") Long id) {
        BlogPost post = blogPostRepository
            .findByIdWithDoctor(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy bài viết", ENTITY_NAME, "blognotfound"));

        if (post.getStatus() != BlogStatus.PUBLISHED) {
            throw new BadRequestAlertException("Bài viết này chưa được xuất bản công khai", ENTITY_NAME, "notpublished");
        }

        // Tăng lượt xem trực tiếp bằng query DB tối ưu, tránh race condition
        blogPostRepository.incrementViewCount(id);
        post.setViewCount(post.getViewCount() + 1);

        return ResponseEntity.ok(post);
    }

    /**
     * {@code GET /blogs/:id/comments} : Lấy danh sách bình luận đã được duyệt cho bài viết.
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<BlogComment>> getComments(@PathVariable("id") Long id) {
        List<BlogComment> comments = blogCommentRepository.findByBlogPost_IdAndIsApprovedTrueOrderByCreatedDateDesc(id);
        return ResponseEntity.ok(comments);
    }

    /**
     * {@code POST /blogs/:id/comments} : Gửi bình luận bài viết (hỗ trợ cả thành viên và khách).
     */
    @PostMapping("/{id}/comments")
    public ResponseEntity<BlogComment> addComment(
        @PathVariable("id") Long id,
        @Valid @RequestBody BlogComment comment
    ) throws URISyntaxException {
        BlogPost post = blogPostRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy bài viết", ENTITY_NAME, "blognotfound"));

        comment.setBlogPost(post);
        comment.setIsApproved(true);

        // Nếu người dùng đã đăng nhập, tự động liên kết PatientProfile
        Optional<String> currentUserLogin = SecurityUtils.getCurrentUserLogin();
        if (currentUserLogin.isPresent()) {
            patientProfileRepository.findByUser_Login(currentUserLogin.get()).ifPresent(comment::setPatient);
        }

        BlogComment result = blogCommentRepository.save(comment);
        LOG.info("💬 Đã lưu bình luận mới cho bài viết ID {}: '{}'", id, comment.getContent());
        return ResponseEntity.created(new URI("/api/blogs/" + id + "/comments/" + result.getId())).body(result);
    }
}
