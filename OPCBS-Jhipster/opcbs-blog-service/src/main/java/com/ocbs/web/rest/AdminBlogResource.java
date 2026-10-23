package com.ocbs.web.rest;

import com.ocbs.domain.BlogPost;
import com.ocbs.domain.enumeration.BlogStatus;
import com.ocbs.repository.BlogPostRepository;
import com.ocbs.security.SecurityUtils;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller dành cho Quản trị viên (Admin) và CS kiểm duyệt bài viết Blog.
 */
@RestController
@RequestMapping("/api/admin/blogs")
@Transactional
public class AdminBlogResource {

    private static final Logger LOG = LoggerFactory.getLogger(AdminBlogResource.class);
    private static final String ENTITY_NAME = "blogPost";

    private final BlogPostRepository blogPostRepository;

    public AdminBlogResource(BlogPostRepository blogPostRepository) {
        this.blogPostRepository = blogPostRepository;
    }

    /**
     * {@code GET /admin/blogs} : Lấy danh sách bài viết theo bộ lọc trạng thái.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CS')")
    public ResponseEntity<List<BlogPost>> getBlogsForModeration(@RequestParam(name = "status", required = false) String statusStr) {
        List<BlogPost> list;
        if (statusStr != null && !statusStr.isBlank() && !"ALL".equalsIgnoreCase(statusStr)) {
            BlogStatus status = BlogStatus.valueOf(statusStr.toUpperCase());
            list = blogPostRepository.findByStatusOrderBySubmittedAtDesc(status);
        } else {
            list = blogPostRepository.findAll();
        }
        return ResponseEntity.ok(list);
    }

    /**
     * {@code POST /admin/blogs/:id/approve} : Phê duyệt và tự động xuất bản bài viết lên trang chủ.
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CS')")
    public ResponseEntity<BlogPost> approveBlog(@PathVariable("id") Long id) {
        BlogPost post = blogPostRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy bài viết", ENTITY_NAME, "blognotfound"));

        if (post.getStatus() != BlogStatus.SUBMITTED) {
            throw new BadRequestAlertException("Chỉ có thể phê duyệt bài viết đang ở trạng thái Chờ duyệt (SUBMITTED)", ENTITY_NAME, "invalidstatus");
        }

        String adminLogin = SecurityUtils.getCurrentUserLogin().orElse("admin");
        post.setStatus(BlogStatus.PUBLISHED);
        post.setApprovedAt(Instant.now());
        post.setPublishedAt(Instant.now());
        post.setRejectionReason(null);

        BlogPost result = blogPostRepository.save(post);
        LOG.info("AUDIT: Admin/CS '{}' approved and published blog ID: {}", adminLogin, post.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * {@code POST /admin/blogs/:id/reject} : Từ chối bài viết kèm theo lý do cụ thể.
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CS')")
    public ResponseEntity<BlogPost> rejectBlog(
        @PathVariable("id") Long id,
        @RequestBody(required = false) Map<String, Object> body
    ) {
        BlogPost post = blogPostRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy bài viết", ENTITY_NAME, "blognotfound"));

        if (post.getStatus() != BlogStatus.SUBMITTED) {
            throw new BadRequestAlertException("Chỉ có thể từ chối bài viết đang ở trạng thái Chờ duyệt (SUBMITTED)", ENTITY_NAME, "invalidstatus");
        }

        String reason = (body != null && body.get("reason") != null) ? body.get("reason").toString().trim() : "";
        if (reason.isBlank()) {
            throw new BadRequestAlertException("Vui lòng cung cấp lý do từ chối bài viết", ENTITY_NAME, "reasonrequired");
        }

        String adminLogin = SecurityUtils.getCurrentUserLogin().orElse("admin");
        post.setStatus(BlogStatus.REJECTED);
        post.setRejectionReason(reason);

        BlogPost result = blogPostRepository.save(post);
        LOG.info("AUDIT: Admin/CS '{}' rejected blog ID: {}. Reason: {}", adminLogin, post.getId(), reason);
        return ResponseEntity.ok(result);
    }

    /**
     * {@code POST /admin/blogs/:id/archive} : Lưu trữ hoặc gỡ bỏ bài viết khỏi trang công khai.
     */
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CS')")
    public ResponseEntity<BlogPost> archiveBlog(@PathVariable("id") Long id) {
        BlogPost post = blogPostRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy bài viết", ENTITY_NAME, "blognotfound"));

        String adminLogin = SecurityUtils.getCurrentUserLogin().orElse("admin");
        post.setStatus(BlogStatus.ARCHIVED);
        post.setArchivedAt(Instant.now());

        BlogPost result = blogPostRepository.save(post);
        LOG.info("AUDIT: Admin/CS '{}' archived blog ID: {}", adminLogin, post.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * {@code GET /admin/blogs/stats} : Thống kê số lượng bài viết theo trạng thái thực thi ở tầng DB.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CS')")
    public ResponseEntity<Map<String, Object>> getBlogStats() {
        long total = blogPostRepository.count();
        long published = blogPostRepository.countByStatus(BlogStatus.PUBLISHED);
        long submitted = blogPostRepository.countByStatus(BlogStatus.SUBMITTED);
        long rejected = blogPostRepository.countByStatus(BlogStatus.REJECTED);
        long draft = blogPostRepository.countByStatus(BlogStatus.DRAFT);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalBlogs", total);
        stats.put("publishedBlogs", published);
        stats.put("pendingBlogs", submitted);
        stats.put("rejectedBlogs", rejected);
        stats.put("draftBlogs", draft);

        return ResponseEntity.ok(stats);
    }
}
