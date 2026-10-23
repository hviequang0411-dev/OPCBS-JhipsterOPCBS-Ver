package com.ocbs.web.rest;

import com.ocbs.security.SecurityUtils;
import com.ocbs.service.DoctorDashboardService;
import com.ocbs.service.dto.DoctorDashboardDTO;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctor/dashboard")
public class DoctorDashboardResource {

    private static final Logger LOG = LoggerFactory.getLogger(DoctorDashboardResource.class);
    private static final String ENTITY_NAME = "doctorDashboard";

    private final DoctorDashboardService dashboardService;

    public DoctorDashboardResource(DoctorDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * {@code GET /doctor/dashboard/overview} : Lấy toàn bộ dữ liệu Doctor Dashboard với Redis Cache.
     */
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DoctorDashboardDTO> getDashboardOverview(
        @RequestParam(name = "period", defaultValue = "today") String period
    ) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        LOG.debug("REST request for Doctor Dashboard by user '{}' (period: {})", login, period);
        DoctorDashboardDTO dto = dashboardService.getDoctorDashboardOverview(login, period);
        return ResponseEntity.ok(dto);
    }

    /**
     * {@code POST /doctor/dashboard/cache/evict} : Xóa cache thủ công để kiểm thử / sau khi cập nhật dữ liệu.
     */
    @PostMapping("/cache/evict")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> evictDashboardCache() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "unauthenticated"));

        boolean success = dashboardService.evictDoctorDashboardCache(login);
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "Đã xóa Redis Cache thành công cho bác sĩ: " + login : "Không tìm thấy khóa cache hoặc Redis tạm thời không phản hồi",
            "doctorLogin", login
        ));
    }
}
