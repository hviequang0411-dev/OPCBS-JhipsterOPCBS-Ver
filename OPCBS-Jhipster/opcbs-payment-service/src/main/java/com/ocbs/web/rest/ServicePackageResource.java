package com.ocbs.web.rest;

import com.ocbs.domain.*;
import com.ocbs.repository.*;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
@Transactional
public class ServicePackageResource {

    private static final Logger LOG = LoggerFactory.getLogger(ServicePackageResource.class);
    private static final String ENTITY_NAME = "servicePackage";
    private static final String ADMIN_DASHBOARD_CACHE_KEY = "dashboard:admin:overview";
    private static final long ADMIN_DASHBOARD_TTL_SECONDS = 120; // 2 phút TTL (độ tươi mới cao, hạn chế độ trễ khi chưa có distributed event bus)

    private final ServicePackageRepository servicePackageRepository;
    private final DoctorSubscriptionRepository doctorSubscriptionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final SpecializationRepository specializationRepository;
    private final AppointmentRepository appointmentRepository;
    private final PsychometricTestRepository psychometricTestRepository;
    private final PsychometricSubmissionRepository psychometricSubmissionRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ServicePackageResource(
        ServicePackageRepository servicePackageRepository,
        DoctorSubscriptionRepository doctorSubscriptionRepository,
        PaymentTransactionRepository paymentTransactionRepository,
        DoctorProfileRepository doctorProfileRepository,
        SpecializationRepository specializationRepository,
        AppointmentRepository appointmentRepository,
        PsychometricTestRepository psychometricTestRepository,
        PsychometricSubmissionRepository psychometricSubmissionRepository,
        @Autowired(required = false) StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper
    ) {
        this.servicePackageRepository = servicePackageRepository;
        this.doctorSubscriptionRepository = doctorSubscriptionRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.specializationRepository = specializationRepository;
        this.appointmentRepository = appointmentRepository;
        this.psychometricTestRepository = psychometricTestRepository;
        this.psychometricSubmissionRepository = psychometricSubmissionRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Dọn dẹp cache Dashboard Admin khi có biến động dữ liệu nghiệp vụ.
     * @return true nếu đã xóa cache thành công trong Redis, false nếu Redis không khả dụng hoặc lỗi
     */
    public boolean evictAdminDashboardCache() {
        if (redisTemplate == null) {
            LOG.warn("⚠️ [REDIS CACHE EVICT SKIPPED] RedisTemplate không khả dụng.");
            return false;
        }
        try {
            Boolean deleted = redisTemplate.delete(ADMIN_DASHBOARD_CACHE_KEY);
            LOG.info("🧹 [REDIS CACHE EVICT] Xóa Admin Dashboard Cache (Key: '{}', Result: {})", ADMIN_DASHBOARD_CACHE_KEY, deleted);
            return Boolean.TRUE.equals(deleted);
        } catch (Exception e) {
            LOG.warn("⚠️ [REDIS CACHE EVICT ERROR] Admin Dashboard: {}", e.getMessage());
            return false;
        }
    }

    /**
     * {@code GET /service-packages} : Get all active platform service packages for doctors.
     */
    @GetMapping("/service-packages")
    public ResponseEntity<List<ServicePackage>> getAllActiveServicePackages() {
        List<ServicePackage> list = servicePackageRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc();
        return ResponseEntity.ok(list);
    }

    /**
     * {@code GET /service-packages/:id} : Get service package details.
     */
    @GetMapping("/service-packages/{id}")
    public ResponseEntity<ServicePackage> getServicePackage(@PathVariable("id") Long id) {
        ServicePackage pkg = servicePackageRepository.findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Service package not found", ENTITY_NAME, "idnotfound"));
        return ResponseEntity.ok(pkg);
    }

    /**
     * {@code POST /service-packages} : Admin creates a new service package tier.
     */
    @PostMapping({"/service-packages", "/admin/service-packages"})
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ServicePackage> createServicePackage(@Valid @RequestBody ServicePackage servicePackage) throws URISyntaxException {
        if (servicePackage.getId() != null) {
            throw new BadRequestAlertException("A new service package cannot already have an ID", ENTITY_NAME, "idexists");
        }
        ServicePackage result = servicePackageRepository.save(servicePackage);
        evictAdminDashboardCache();
        return ResponseEntity.created(new URI("/api/service-packages/" + result.getId())).body(result);
    }

    /**
     * {@code GET /admin/service-packages} : Admin gets all configured service packages.
     */
    @GetMapping("/admin/service-packages")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<ServicePackage>> getAllServicePackagesForAdmin() {
        List<ServicePackage> list = servicePackageRepository.findAllByOrderByDisplayOrderAscIdAsc();
        return ResponseEntity.ok(list);
    }

    /**
     * {@code GET /admin/service-packages/stats} : Admin gets package analytics metrics.
     */
    @GetMapping("/admin/service-packages/stats")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> getServicePackageStats() {
        List<ServicePackage> all = servicePackageRepository.findAll();
        long total = all.size();
        long active = all.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).count();
        long inactive = total - active;
        double avgPrice = all.stream().mapToDouble(p -> p.getPrice() != null ? p.getPrice().doubleValue() : 0.0).average().orElse(0.0);

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalPackages", total);
        stats.put("activePackages", active);
        stats.put("inactivePackages", inactive);
        stats.put("averagePrice", avgPrice);
        return ResponseEntity.ok(stats);
    }

    /**
     * {@code GET /admin/dashboard/overview} : Admin gets live aggregated dashboard metrics from normalized database tables.
     */
    @GetMapping("/admin/dashboard/overview")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdminDashboardOverview() {
        long startTime = System.currentTimeMillis();

        // 1. Kiểm tra Redis Cache Hit
        if (redisTemplate != null) {
            try {
                String cachedJson = redisTemplate.opsForValue().get(ADMIN_DASHBOARD_CACHE_KEY);
                if (cachedJson != null && !cachedJson.isBlank()) {
                    Map<String, Object> cachedData = objectMapper.readValue(
                        cachedJson,
                        new TypeReference<Map<String, Object>>() {}
                    );
                    long latency = System.currentTimeMillis() - startTime;
                    cachedData.put("cacheHit", true);
                    cachedData.put("executionTimeMs", latency);
                    LOG.info("⚡ [REDIS CACHE HIT] Admin Dashboard Key: '{}' (Latency: {} ms)", ADMIN_DASHBOARD_CACHE_KEY, latency);
                    return ResponseEntity.ok(cachedData);
                }
            } catch (Exception e) {
                LOG.warn("⚠️ [REDIS CACHE READ ERROR] Admin Dashboard - Fallback to Database: {}", e.getMessage());
            }
        }

        LOG.info("🗄️ [REDIS CACHE MISS] Admin Dashboard -> Đang tổng hợp số liệu trực tiếp từ MySQL Database...");

        Map<String, Object> response = new HashMap<>();

        // 1. Doctors & Specializations from DB
        long totalDoctors = doctorProfileRepository.count();
        long totalSpecializations = specializationRepository.count();
        List<Specialization> allSpecs = specializationRepository.findAll();
        List<DoctorProfile> allDoctors = doctorProfileRepository.findAllWithEagerRelationships();

        Map<Long, Long> specDoctorCounts = new HashMap<>();
        for (DoctorProfile doc : allDoctors) {
            if (doc.getSpecializationses() != null) {
                for (Specialization sp : doc.getSpecializationses()) {
                    specDoctorCounts.put(sp.getId(), specDoctorCounts.getOrDefault(sp.getId(), 0L) + 1L);
                }
            }
        }

        long totalAssigned = specDoctorCounts.values().stream().mapToLong(Long::longValue).sum();
        List<Map<String, Object>> specBreakdown = new ArrayList<>();
        String[] colors = new String[]{"#0f766e", "#6366f1", "#10b981", "#f59e0b", "#ec4899", "#8b5cf6", "#3b82f6", "#14b8a6"};
        int colorIdx = 0;
        for (Specialization s : allSpecs) {
            long docCount = specDoctorCounts.getOrDefault(s.getId(), 0L);
            int percentage = totalAssigned > 0 ? (int) Math.round((docCount * 100.0) / totalAssigned) : 0;
            Map<String, Object> item = new HashMap<>();
            item.put("name", s.getName());
            item.put("doctorCount", docCount);
            item.put("percentage", percentage);
            item.put("color", colors[colorIdx % colors.length]);
            colorIdx++;
            specBreakdown.add(item);
        }

        // 2. Subscriptions & Payments from DB với đối chiếu chống cộng trùng 2 chiều
        List<DoctorSubscription> allSubs = doctorSubscriptionRepository.findAll();
        List<PaymentTransaction> allTx = paymentTransactionRepository.findAll();

        long activeSubs = allSubs.stream().filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus())).count();

        // Tập hợp các ID subscription và TransactionCode đã thanh toán thành công qua PaymentTransaction
        Set<String> successfulTxCodes = new HashSet<>();
        Set<Long> successfulSubIds = new HashSet<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal unconfirmedRevenue = BigDecimal.ZERO;

        for (PaymentTransaction tx : allTx) {
            if ("SUCCESS".equalsIgnoreCase(tx.getPaymentStatus()) && tx.getAmount() != null) {
                totalRevenue = totalRevenue.add(tx.getAmount());
                if (tx.getTransactionCode() != null && !tx.getTransactionCode().isBlank()) {
                    successfulTxCodes.add(tx.getTransactionCode());
                }
                if (tx.getSubscriptionId() != null) {
                    successfulSubIds.add(tx.getSubscriptionId());
                }
            } else if ("PENDING".equalsIgnoreCase(tx.getPaymentStatus()) && tx.getAmount() != null) {
                unconfirmedRevenue = unconfirmedRevenue.add(tx.getAmount());
            }
        }

        for (DoctorSubscription sub : allSubs) {
            boolean isPaidStatus = "ACTIVE".equalsIgnoreCase(sub.getStatus()) || "EXPIRED".equalsIgnoreCase(sub.getStatus());
            boolean paidViaTx = (sub.getTransactionCode() != null && successfulTxCodes.contains(sub.getTransactionCode()))
                || (sub.getId() != null && successfulSubIds.contains(sub.getId()));

            if (isPaidStatus && !paidViaTx && sub.getPrice() != null) {
                totalRevenue = totalRevenue.add(sub.getPrice());
            } else if ("PENDING_PAYMENT".equalsIgnoreCase(sub.getStatus()) && sub.getPrice() != null && !paidViaTx) {
                unconfirmedRevenue = unconfirmedRevenue.add(sub.getPrice());
            }
        }

        // 3. Appointments Breakdown from DB
        long totalAppointments = appointmentRepository.count();
        List<Appointment> allAppts = appointmentRepository.findAll();
        Map<String, Long> statusCounts = new HashMap<>();
        for (Appointment a : allAppts) {
            String st = a.getStatus() != null ? a.getStatus().name() : "PENDING";
            statusCounts.put(st, statusCounts.getOrDefault(st, 0L) + 1L);
        }

        List<Map<String, Object>> apptBreakdown = new ArrayList<>();
        String[][] statusConfigs = new String[][]{
            {"COMPLETED", "Đã hoàn thành", "#10b981"},
            {"CONFIRMED", "Đã xác nhận", "#0f766e"},
            {"PENDING", "Chờ xử lý", "#f59e0b"},
            {"CANCELLED", "Đã hủy", "#ef4444"},
            {"IN_PROGRESS", "Đang khám", "#6366f1"}
        };
        for (String[] cfg : statusConfigs) {
            long count = statusCounts.getOrDefault(cfg[0], 0L);
            int percentage = totalAppointments > 0 ? (int) Math.round((count * 100.0) / totalAppointments) : 0;
            Map<String, Object> stItem = new HashMap<>();
            stItem.put("status", cfg[0]);
            stItem.put("label", cfg[1]);
            stItem.put("color", cfg[2]);
            stItem.put("count", count);
            stItem.put("percentage", percentage);
            apptBreakdown.add(stItem);
        }

        // 4. Monthly Revenue Trend (Dynamic 6 Calendar Months, Asia/Ho_Chi_Minh Zone, cùng quy tắc chống cộng trùng)
        List<Map<String, Object>> monthlyTrend = new ArrayList<>();
        ZoneId zoneVn = ZoneId.of("Asia/Ho_Chi_Minh");
        YearMonth currentMonth = YearMonth.now(zoneVn);
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = currentMonth.minusMonths(i);
            String monthLabel = "T" + ym.getMonthValue() + "/" + ym.getYear();

            ZonedDateTime startOfMonth = ym.atDay(1).atStartOfDay(zoneVn);
            ZonedDateTime endOfMonth = ym.atEndOfMonth().atTime(23, 59, 59, 999_999_999).atZone(zoneVn);
            Instant startInstant = startOfMonth.toInstant();
            Instant endInstant = endOfMonth.toInstant();

            BigDecimal monthRev = BigDecimal.ZERO;
            long monthSubs = 0;

            // 4.1. Doanh thu từ PaymentTransaction SUCCESS trong tháng
            for (PaymentTransaction tx : allTx) {
                Instant date = tx.getPaidAt() != null ? tx.getPaidAt() : tx.getCreatedDate();
                if (date != null && !date.isBefore(startInstant) && !date.isAfter(endInstant)) {
                    if ("SUCCESS".equalsIgnoreCase(tx.getPaymentStatus()) && tx.getAmount() != null) {
                        monthRev = monthRev.add(tx.getAmount());
                    }
                }
            }

            // 4.2. Doanh thu từ Subscription hợp lệ trong tháng (chưa có Transaction SUCCESS tương ứng)
            for (DoctorSubscription s : allSubs) {
                Instant date = s.getStartDate() != null ? s.getStartDate() : s.getCreatedDate();
                if (date != null && !date.isBefore(startInstant) && !date.isAfter(endInstant)) {
                    monthSubs++;
                    boolean isPaidStatus = "ACTIVE".equalsIgnoreCase(s.getStatus()) || "EXPIRED".equalsIgnoreCase(s.getStatus());
                    boolean paidViaTx = (s.getTransactionCode() != null && successfulTxCodes.contains(s.getTransactionCode()))
                        || (s.getId() != null && successfulSubIds.contains(s.getId()));

                    if (isPaidStatus && !paidViaTx && s.getPrice() != null) {
                        monthRev = monthRev.add(s.getPrice());
                    }
                }
            }

            Map<String, Object> monthItem = new HashMap<>();
            monthItem.put("month", monthLabel);
            monthItem.put("revenue", monthRev.doubleValue());
            monthItem.put("subscriptions", monthSubs);
            monthlyTrend.add(monthItem);
        }

        // 5. Psychometrics Counts
        long totalTests = psychometricTestRepository.count();
        long totalSubmissions = psychometricSubmissionRepository.count();

        // 6. Packages
        List<ServicePackage> recentPkgs = servicePackageRepository.findAllByOrderByDisplayOrderAscIdAsc();

        response.put("totalRevenue", totalRevenue.doubleValue());
        response.put("unconfirmedRevenue", unconfirmedRevenue.doubleValue());
        response.put("activeSubscriptions", activeSubs);
        response.put("totalDoctors", totalDoctors);
        response.put("totalSpecializations", totalSpecializations);
        response.put("totalAppointments", totalAppointments);
        response.put("totalTests", totalTests);
        response.put("totalSubmissions", totalSubmissions);
        response.put("specializationBreakdown", specBreakdown);
        response.put("monthlyRevenueTrend", monthlyTrend);
        response.put("appointmentStatusBreakdown", apptBreakdown);
        response.put("recentPackages", recentPkgs);
        response.put("revenueNote", "Doanh thu thực tế đã ghi nhận từ các giao dịch VNPay thành công và gói đăng ký dịch vụ bác sĩ kích hoạt trực tiếp (đã đối chiếu chống cộng trùng 2 chiều giữa Subscription và Transaction). Không bao gồm các khoản đang chờ thanh toán hoặc đã hủy.");
        response.put("dataFreshnessNote", "TTL bộ nhớ đệm: 120s (2 phút). Invalidate tức thì khi phát sinh giao dịch hoặc cập nhật gói dịch vụ.");

        long execTime = System.currentTimeMillis() - startTime;
        response.put("cacheHit", false);
        response.put("cacheKey", ADMIN_DASHBOARD_CACHE_KEY);
        response.put("cachedAt", Instant.now().toString());
        response.put("executionTimeMs", execTime);

        // Lưu vào Redis Cache với TTL
        if (redisTemplate != null) {
            try {
                String json = objectMapper.writeValueAsString(response);
                redisTemplate.opsForValue().set(ADMIN_DASHBOARD_CACHE_KEY, json, ADMIN_DASHBOARD_TTL_SECONDS, TimeUnit.SECONDS);
                LOG.info("💾 [REDIS CACHE SAVED] Đã lưu Admin Dashboard vào Cache (TTL: {}s, Thời gian tính toán: {} ms)",
                    ADMIN_DASHBOARD_TTL_SECONDS, execTime);
            } catch (Exception e) {
                LOG.warn("⚠️ [REDIS CACHE WRITE ERROR] Admin Dashboard: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(response);
    }

    /**
     * {@code POST /admin/dashboard/cache/evict} : Admin chủ động làm mới/xóa Redis Cache của Dashboard.
     */
    @PostMapping("/admin/dashboard/cache/evict")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> evictAdminDashboardCacheEndpoint() {
        boolean success = evictAdminDashboardCache();
        Map<String, Object> body = new HashMap<>();
        body.put("success", success);
        body.put("cacheKey", ADMIN_DASHBOARD_CACHE_KEY);
        body.put("message", success ? "Đã xóa Redis Cache thành công cho Admin Dashboard" : "Khóa cache không tồn tại hoặc Redis tạm thời không phản hồi");
        return ResponseEntity.ok(body);
    }

    /**
     * {@code PUT /admin/service-packages/:id} : Admin updates an existing service package.
     */
    @PutMapping({"/service-packages/{id}", "/admin/service-packages/{id}"})
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ServicePackage> updateServicePackage(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody ServicePackage servicePackage
    ) {
        if (servicePackage.getId() == null || !java.util.Objects.equals(id, servicePackage.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!servicePackageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        ServicePackage result = servicePackageRepository.save(servicePackage);
        evictAdminDashboardCache();
        return ResponseEntity.ok(result);
    }

    /**
     * {@code PATCH /admin/service-packages/:id/toggle-active} : Toggle package active state.
     */
    @PatchMapping("/admin/service-packages/{id}/toggle-active")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ServicePackage> togglePackageActive(@PathVariable("id") Long id) {
        ServicePackage pkg = servicePackageRepository.findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));

        pkg.setIsActive(!Boolean.TRUE.equals(pkg.getIsActive()));
        ServicePackage result = servicePackageRepository.save(pkg);
        evictAdminDashboardCache();
        return ResponseEntity.ok(result);
    }

    /**
     * {@code DELETE /admin/service-packages/:id} : Admin deletes a service package.
     */
    @DeleteMapping({"/service-packages/{id}", "/admin/service-packages/{id}"})
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteServicePackage(@PathVariable("id") Long id) {
        if (!servicePackageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        servicePackageRepository.deleteById(id);
        evictAdminDashboardCache();
        return ResponseEntity.noContent().build();
    }
}
