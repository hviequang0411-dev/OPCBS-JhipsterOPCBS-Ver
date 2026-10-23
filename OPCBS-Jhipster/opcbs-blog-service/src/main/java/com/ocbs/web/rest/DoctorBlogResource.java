package com.ocbs.web.rest;

import com.ocbs.domain.BlogPost;
import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.enumeration.BlogStatus;
import com.ocbs.repository.BlogPostRepository;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.security.SecurityUtils;
import com.ocbs.service.util.HtmlSanitizerUtil;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller dành cho Bác sĩ viết và quản lý bài viết Blog cá nhân.
 */
@RestController
@RequestMapping("/api/doctor/blogs")
@Transactional
public class DoctorBlogResource {

    private static final Logger LOG = LoggerFactory.getLogger(DoctorBlogResource.class);
    private static final String ENTITY_NAME = "blogPost";

    private final BlogPostRepository blogPostRepository;
    private final DoctorProfileRepository doctorProfileRepository;

    public DoctorBlogResource(
        BlogPostRepository blogPostRepository,
        DoctorProfileRepository doctorProfileRepository
    ) {
        this.blogPostRepository = blogPostRepository;
        this.doctorProfileRepository = doctorProfileRepository;
    }

    private DoctorProfile getCurrentDoctor() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("Người dùng chưa được xác thực", ENTITY_NAME, "unauthenticated"));
        return doctorProfileRepository
            .findByUser_Login(login)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy hồ sơ bác sĩ", ENTITY_NAME, "doctornotfound"));
    }

    /**
     * {@code GET /doctor/blogs} : Lấy danh sách bài viết thuộc về bác sĩ hiện tại.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<List<BlogPost>> getMyBlogs() {
        DoctorProfile doctor = getCurrentDoctor();
        List<BlogPost> blogs = blogPostRepository.findByDoctor_IdOrderByCreatedDateDesc(doctor.getId());
        return ResponseEntity.ok(blogs);
    }

    /**
     * {@code GET /doctor/blogs/:id} : Lấy chi tiết bài viết của chính bác sĩ để chỉnh sửa.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<BlogPost> getMyBlogById(@PathVariable("id") Long id) {
        DoctorProfile doctor = getCurrentDoctor();
        BlogPost post = blogPostRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy bài viết", ENTITY_NAME, "blognotfound"));

        if (!post.getDoctor().getId().equals(doctor.getId())) {
            throw new BadRequestAlertException("Bạn chỉ có quyền quản lý bài viết của chính mình", ENTITY_NAME, "forbidden");
        }
        return ResponseEntity.ok(post);
    }

    /**
     * {@code POST /doctor/blogs} : Bác sĩ tạo bài viết mới (mặc định trạng thái DRAFT hoặc SUBMITTED).
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<BlogPost> createBlog(@Valid @RequestBody BlogPost blogPost) throws URISyntaxException {
        DoctorProfile doctor = getCurrentDoctor();
        blogPost.setDoctor(doctor);
        blogPost.setViewCount(0);

        blogPost.setTitle(HtmlSanitizerUtil.stripAllHtml(blogPost.getTitle()));
        blogPost.setExcerpt(HtmlSanitizerUtil.stripAllHtml(blogPost.getExcerpt()));
        blogPost.setContent(HtmlSanitizerUtil.sanitizeContent(blogPost.getContent()));

        if (blogPost.getStatus() == null) {
            blogPost.setStatus(BlogStatus.DRAFT);
        }

        if (blogPost.getStatus() == BlogStatus.SUBMITTED) {
            blogPost.setSubmittedAt(Instant.now());
        }

        BlogPost result = blogPostRepository.save(blogPost);
        LOG.info("AUDIT: Doctor ID {} created blog ID {}, status: {}", doctor.getId(), result.getId(), result.getStatus());
        return ResponseEntity.created(new URI("/api/doctor/blogs/" + result.getId())).body(result);
    }

    /**
     * {@code PUT /doctor/blogs/:id} : Bác sĩ cập nhật nội dung bài viết của mình.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<BlogPost> updateBlog(
        @PathVariable("id") Long id,
        @Valid @RequestBody BlogPost blogPost
    ) {
        DoctorProfile doctor = getCurrentDoctor();
        BlogPost existing = blogPostRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy bài viết", ENTITY_NAME, "blognotfound"));

        if (!existing.getDoctor().getId().equals(doctor.getId())) {
            throw new BadRequestAlertException("Bạn chỉ có quyền chỉnh sửa bài viết của chính mình", ENTITY_NAME, "forbidden");
        }

        if (existing.getStatus() != BlogStatus.DRAFT && existing.getStatus() != BlogStatus.REJECTED) {
            throw new BadRequestAlertException("Chỉ có thể chỉnh sửa bài viết đang ở trạng thái Bản nháp (DRAFT) hoặc Bị từ chối (REJECTED)", ENTITY_NAME, "invalidstatus");
        }

        existing.setTitle(HtmlSanitizerUtil.stripAllHtml(blogPost.getTitle()));
        existing.setContent(HtmlSanitizerUtil.sanitizeContent(blogPost.getContent()));
        existing.setThumbnailUrl(blogPost.getThumbnailUrl());
        existing.setExcerpt(HtmlSanitizerUtil.stripAllHtml(blogPost.getExcerpt()));

        BlogPost result = blogPostRepository.save(existing);
        LOG.info("AUDIT: Doctor ID {} updated blog ID {}", doctor.getId(), result.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * {@code POST /doctor/blogs/:id/submit} : Bác sĩ gửi bài viết sang hàng đợi kiểm duyệt (SUBMITTED).
     */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<BlogPost> submitBlogForReview(@PathVariable("id") Long id) {
        DoctorProfile doctor = getCurrentDoctor();
        BlogPost post = blogPostRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy bài viết", ENTITY_NAME, "blognotfound"));

        if (!post.getDoctor().getId().equals(doctor.getId())) {
            throw new BadRequestAlertException("Bạn chỉ có quyền gửi duyệt bài viết của chính mình", ENTITY_NAME, "forbidden");
        }

        if (post.getStatus() != BlogStatus.DRAFT && post.getStatus() != BlogStatus.REJECTED) {
            throw new BadRequestAlertException("Chỉ có thể gửi duyệt bài viết đang ở trạng thái Bản nháp hoặc Bị từ chối", ENTITY_NAME, "invalidstatus");
        }

        post.setStatus(BlogStatus.SUBMITTED);
        post.setSubmittedAt(Instant.now());
        post.setRejectionReason(null);

        BlogPost result = blogPostRepository.save(post);
        LOG.info("AUDIT: Doctor ID {} submitted blog ID {} for review", doctor.getId(), post.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * {@code DELETE /doctor/blogs/:id} : Bác sĩ xóa bài viết của mình.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<Void> deleteBlog(@PathVariable("id") Long id) {
        DoctorProfile doctor = getCurrentDoctor();
        BlogPost post = blogPostRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Không tìm thấy bài viết", ENTITY_NAME, "blognotfound"));

        if (!post.getDoctor().getId().equals(doctor.getId())) {
            throw new BadRequestAlertException("Bạn chỉ có quyền xóa bài viết của chính mình", ENTITY_NAME, "forbidden");
        }

        if (post.getStatus() == BlogStatus.PUBLISHED) {
            throw new BadRequestAlertException("Không thể xóa bài viết đã xuất bản, vui lòng liên hệ Admin", ENTITY_NAME, "cannotdeletepublished");
        }

        blogPostRepository.delete(post);
        LOG.info("AUDIT: Doctor ID {} deleted blog ID {}", doctor.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
