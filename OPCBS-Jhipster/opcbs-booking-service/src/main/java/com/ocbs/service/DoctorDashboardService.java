package com.ocbs.service;

import com.ocbs.domain.Appointment;
import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.enumeration.AppointmentStatus;
import com.ocbs.repository.AppointmentRepository;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.service.dto.DoctorDashboardDTO;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class DoctorDashboardService {

    private static final Logger LOG = LoggerFactory.getLogger(DoctorDashboardService.class);
    private static final long CACHE_TTL_SECONDS = 300; // 5 phút TTL
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DoctorProfileRepository doctorProfileRepository;
    private final AppointmentRepository appointmentRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public DoctorDashboardService(
        DoctorProfileRepository doctorProfileRepository,
        AppointmentRepository appointmentRepository,
        @Autowired(required = false) StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper
    ) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.appointmentRepository = appointmentRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Lấy dữ liệu Dashboard Bác sĩ với tính toán số liệu chính xác theo kỳ và cơ chế Redis Cache an toàn.
     */
    public DoctorDashboardDTO getDoctorDashboardOverview(String doctorLogin, String period) {
        long startTime = System.currentTimeMillis();
        String safePeriod = (period != null && !period.isBlank()) ? period.toLowerCase().trim() : "today";
        if (!safePeriod.equals("today") && !safePeriod.equals("week") && !safePeriod.equals("month")) {
            safePeriod = "today";
        }
        String cacheKey = "dashboard:doctor:" + doctorLogin + ":" + safePeriod;

        // 1. Kiểm tra Redis Cache Hit
        if (redisTemplate != null) {
            try {
                String cachedJson = redisTemplate.opsForValue().get(cacheKey);
                if (cachedJson != null && !cachedJson.isBlank()) {
                    DoctorDashboardDTO cachedDto = objectMapper.readValue(cachedJson, DoctorDashboardDTO.class);
                    long latency = System.currentTimeMillis() - startTime;
                    cachedDto.setCacheHit(true);
                    cachedDto.setExecutionTimeMs(latency);
                    LOG.info("⚡ [REDIS CACHE HIT] Key: '{}' (Latency: {} ms)", cacheKey, latency);
                    return cachedDto;
                }
            } catch (Exception e) {
                LOG.warn("⚠️ [REDIS CACHE READ ERROR] Key: '{}' - Fallback trực tiếp sang Database: {}", cacheKey, e.getMessage());
            }
        }

        LOG.info("🗄️ [REDIS CACHE MISS] Key: '{}' -> Đang tổng hợp số liệu kỳ '{}' từ MySQL Database...", cacheKey, safePeriod);

        // 2. Tra cứu thông tin Bác sĩ hiện tại - BẢO MẬT: Tuyệt đối không fallback sang bác sĩ khác
        DoctorProfile doctor = doctorProfileRepository.findByUser_Login(doctorLogin)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy hồ sơ bác sĩ cho tài khoản: " + doctorLogin
            ));

        DoctorDashboardDTO dashboard = new DoctorDashboardDTO();
        dashboard.setPeriod(safePeriod);
        dashboard.setCacheKey(cacheKey);
        dashboard.setCachedAt(Instant.now());

        // Header Info của chính bác sĩ này
        dashboard.setDoctorId(doctor.getId());
        dashboard.setDoctorName(doctor.getFullName() != null ? doctor.getFullName() : "Bác sĩ " + doctorLogin);
        dashboard.setProfessionalTitle(doctor.getTitle() != null ? doctor.getTitle() : "Chuyên Gia Tâm Lý Học Lâm Sàng");
        dashboard.setPhone(doctor.getPhone());
        dashboard.setEmail(doctor.getUser() != null ? doctor.getUser().getEmail() : "");
        dashboard.setVerified(doctor.getIsActive() != null && doctor.getIsActive());
        dashboard.setVerificationStatus(dashboard.isVerified() ? "VERIFIED" : "PENDING");

        ZonedDateTime nowZoned = ZonedDateTime.now(VIETNAM_ZONE);
        int currentHour = nowZoned.getHour();
        if (currentHour < 12) {
            dashboard.setGreeting("Chào buổi sáng, " + dashboard.getDoctorName());
        } else if (currentHour < 18) {
            dashboard.setGreeting("Chào buổi chiều, " + dashboard.getDoctorName());
        } else {
            dashboard.setGreeting("Chào buổi tối, " + dashboard.getDoctorName());
        }

        // 3. Xác định ranh giới thời gian chính xác cho Kỳ Hiện Tại và Kỳ Liền Trước
        LocalDate today = nowZoned.toLocalDate();
        Instant currentStart;
        Instant currentEnd;
        Instant prevStart;
        Instant prevEnd;
        String periodLabel;

        DateTimeFormatter dmyFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter dmFormatter = DateTimeFormatter.ofPattern("dd/MM");

        if ("today".equals(safePeriod)) {
            currentStart = today.atStartOfDay(VIETNAM_ZONE).toInstant();
            currentEnd = today.atTime(LocalTime.MAX).atZone(VIETNAM_ZONE).toInstant();
            prevStart = today.minusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();
            prevEnd = today.minusDays(1).atTime(LocalTime.MAX).atZone(VIETNAM_ZONE).toInstant();
            periodLabel = "Hôm nay (" + today.format(dmyFormatter) + ")";
        } else if ("week".equals(safePeriod)) {
            LocalDate monday = today.with(DayOfWeek.MONDAY);
            LocalDate sunday = today.with(DayOfWeek.SUNDAY);
            currentStart = monday.atStartOfDay(VIETNAM_ZONE).toInstant();
            currentEnd = sunday.atTime(LocalTime.MAX).atZone(VIETNAM_ZONE).toInstant();
            prevStart = monday.minusWeeks(1).atStartOfDay(VIETNAM_ZONE).toInstant();
            prevEnd = sunday.minusWeeks(1).atTime(LocalTime.MAX).atZone(VIETNAM_ZONE).toInstant();
            periodLabel = "Tuần này (" + monday.format(dmFormatter) + " - " + sunday.format(dmyFormatter) + ")";
        } else { // month
            LocalDate firstDay = today.withDayOfMonth(1);
            // Kỳ hiện tại MTD: Từ ngày 1 đến hết ngày hiện tại (không tính ngày tương lai vào tăng trưởng)
            currentStart = firstDay.atStartOfDay(VIETNAM_ZONE).toInstant();
            currentEnd = today.atTime(LocalTime.MAX).atZone(VIETNAM_ZONE).toInstant();

            // Cùng kỳ MTD của tháng trước, xử lý an toàn khi tháng trước ngắn hơn tháng hiện tại
            LocalDate prevMonth = today.minusMonths(1);
            int currentDayOfMonth = today.getDayOfMonth();
            int maxDaysInPrevMonth = prevMonth.lengthOfMonth();
            int targetPrevDay = Math.min(currentDayOfMonth, maxDaysInPrevMonth);

            LocalDate prevFirstDay = prevMonth.withDayOfMonth(1);
            LocalDate prevTargetDay = prevMonth.withDayOfMonth(targetPrevDay);

            prevStart = prevFirstDay.atStartOfDay(VIETNAM_ZONE).toInstant();
            prevEnd = prevTargetDay.atTime(LocalTime.MAX).atZone(VIETNAM_ZONE).toInstant();

            if (currentDayOfMonth >= today.lengthOfMonth()) {
                periodLabel = "Tháng " + today.getMonthValue() + "/" + today.getYear();
            } else {
                periodLabel = "Tháng " + today.getMonthValue() + "/" + today.getYear() + " (Đến " + today.format(dmFormatter) + ")";
            }
        }
        dashboard.setPeriodLabel(periodLabel);

        // 4. Lấy dữ liệu ca khám theo kỳ thực tế từ Repository
        List<Appointment> currentPeriodAppointments = appointmentRepository.findByDoctorIdAndDateRange(doctor.getId(), currentStart, currentEnd);
        List<Appointment> prevPeriodAppointments = appointmentRepository.findByDoctorIdAndDateRange(doctor.getId(), prevStart, prevEnd);

        // 4.1. KPI Ca khám và Tăng trưởng so với kỳ trước
        int countCurrent = currentPeriodAppointments.size();
        int countPrev = prevPeriodAppointments.size();
        dashboard.setAppointmentsThisMonth(countCurrent);

        if (countPrev > 0) {
            double growth = Math.round(((countCurrent - countPrev) * 1000.0) / countPrev) / 10.0;
            dashboard.setAppointmentsGrowthPercent(growth);
            dashboard.setAppointmentsGrowthPositive(growth >= 0);
        } else {
            dashboard.setAppointmentsGrowthPercent(null); // Không đủ dữ liệu kỳ trước, không hardcode
            dashboard.setAppointmentsGrowthPositive(countCurrent > 0);
        }

        // 4.2. Doanh thu ước tính (theo đơn giá khám × số ca đã hoàn thành trong kỳ)
        BigDecimal feePerSession = doctor.getConsultationFee() != null ? doctor.getConsultationFee() : new BigDecimal("500000.00");
        long completedCurrent = currentPeriodAppointments.stream()
            .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
            .count();
        long completedPrev = prevPeriodAppointments.stream()
            .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
            .count();

        BigDecimal estRevenueCurrent = feePerSession.multiply(new BigDecimal(completedCurrent));
        dashboard.setRevenueThisMonth(estRevenueCurrent);
        dashboard.setRevenueNote("Ước tính theo đơn giá (" + feePerSession.toPlainString() + " ₫) × " + completedCurrent + " ca hoàn tất");

        if (completedPrev > 0) {
            double revGrowth = Math.round(((completedCurrent - completedPrev) * 1000.0) / completedPrev) / 10.0;
            dashboard.setRevenueGrowthPercent(revGrowth);
            dashboard.setRevenueGrowthPositive(revGrowth >= 0);
        } else {
            dashboard.setRevenueGrowthPercent(null); // Không có dữ liệu kỳ trước
            dashboard.setRevenueGrowthPositive(completedCurrent > 0);
        }

        // 4.3. Thống kê Thân chủ trong kỳ (Mới vs Quay lại) dựa trên lịch sử ca khám
        List<Appointment> allDoctorAppointments = appointmentRepository.findByDoctorId(doctor.getId());
        Set<Long> patientIdsInCurrent = new HashSet<>();
        for (Appointment a : currentPeriodAppointments) {
            if (a.getPatient() != null && a.getPatient().getId() != null) {
                patientIdsInCurrent.add(a.getPatient().getId());
            }
        }

        int returningPatients = 0;
        int newPatients = 0;
        for (Long pid : patientIdsInCurrent) {
            boolean hadBefore = allDoctorAppointments.stream().anyMatch(a ->
                a.getPatient() != null &&
                pid.equals(a.getPatient().getId()) &&
                a.getScheduledStart() != null &&
                a.getScheduledStart().isBefore(currentStart)
            );
            if (hadBefore) {
                returningPatients++;
            } else {
                newPatients++;
            }
        }

        int activePatients = patientIdsInCurrent.size();
        dashboard.setActivePatientsCount(activePatients);
        dashboard.setNewPatientsThisMonth(newPatients);
        dashboard.setReturningPatientsCount(returningPatients);
        dashboard.setReturningPatientRate(activePatients > 0 ? Math.round(((double) returningPatients / activePatients) * 100.0) : 0.0);

        // 4.4. Phân loại Thân chủ (Retention Distribution) tính từ dữ liệu thực tế
        List<Appointment> completedHistory = appointmentRepository.findCompletedByDoctorId(doctor.getId(), AppointmentStatus.COMPLETED);
        Map<Long, Integer> completedCountsPerPatient = new HashMap<>();
        for (Appointment a : completedHistory) {
            if (a.getPatient() != null && a.getPatient().getId() != null) {
                Long pid = a.getPatient().getId();
                completedCountsPerPatient.put(pid, completedCountsPerPatient.getOrDefault(pid, 0) + 1);
            }
        }

        int totalCompletedPatients = completedCountsPerPatient.size();
        int singleVisitCount = 0;
        int repeat2To3Count = 0;
        int loyal4PlusCount = 0;

        for (int completedSessions : completedCountsPerPatient.values()) {
            if (completedSessions == 1) {
                singleVisitCount++;
            } else if (completedSessions >= 2 && completedSessions <= 3) {
                repeat2To3Count++;
            } else if (completedSessions >= 4) {
                loyal4PlusCount++;
            }
        }

        if (totalCompletedPatients > 0) {
            dashboard.setSingleVisitPercent((int) Math.round((singleVisitCount * 100.0) / totalCompletedPatients));
            dashboard.setRepeat2To3Percent((int) Math.round((repeat2To3Count * 100.0) / totalCompletedPatients));
            dashboard.setLoyal4PlusPercent((int) Math.round((loyal4PlusCount * 100.0) / totalCompletedPatients));
            dashboard.setRetentionNote("Phân loại từ " + totalCompletedPatients + " thân chủ đã hoàn tất ca khám");
        } else {
            dashboard.setSingleVisitPercent(0);
            dashboard.setRepeat2To3Percent(0);
            dashboard.setLoyal4PlusPercent(0);
            dashboard.setRetentionNote("Chưa có thân chủ hoàn tất ca khám để phân loại");
        }

        // 5. Action Center & Lịch Khám Trong Ngày
        Instant todayStart = today.atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant todayEnd = today.atTime(LocalTime.MAX).atZone(VIETNAM_ZONE).toInstant();
        List<Appointment> todayAppointments = appointmentRepository.findByDoctorIdAndDateRange(doctor.getId(), todayStart, todayEnd);

        int todaySessions = todayAppointments.size();
        int todayCompleted = 0;
        int todayUpcoming = 0;
        List<DoctorDashboardDTO.AppointmentSummaryDTO> todayList = new ArrayList<>();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(VIETNAM_ZONE);

        for (Appointment app : todayAppointments) {
            if (app.getStatus() == AppointmentStatus.COMPLETED) {
                todayCompleted++;
            } else if (app.getStatus() == AppointmentStatus.ACCEPTED || app.getStatus() == AppointmentStatus.PENDING) {
                todayUpcoming++;
            }

            DoctorDashboardDTO.AppointmentSummaryDTO sum = toSummaryDTO(app, timeFormatter, dmyFormatter);
            todayList.add(sum);
        }

        dashboard.setTodaySessionsCount(todaySessions);
        dashboard.setTodayCompletedCount(todayCompleted);
        dashboard.setTodayUpcomingCount(todayUpcoming);
        dashboard.setTodaySessions(todayList);

        // Action Center Counts
        int pendingApprovals = 0;
        int inProgress = 0;
        int pendingNotes = 0;
        List<DoctorDashboardDTO.AppointmentSummaryDTO> upcomingList = new ArrayList<>();

        for (Appointment app : allDoctorAppointments) {
            if (app.getStatus() == AppointmentStatus.PENDING) {
                pendingApprovals++;
            } else if (app.getStatus() == AppointmentStatus.IN_PROGRESS) {
                inProgress++;
            } else if (app.getStatus() == AppointmentStatus.COMPLETED) {
                // Kiểm tra ghi chú lâm sàng thực tế: nếu thiếu tóm tắt hoặc khuyến nghị
                if (app.getConsultationSummary() == null || app.getConsultationSummary().isBlank() ||
                    app.getRecommendations() == null || app.getRecommendations().isBlank()) {
                    pendingNotes++;
                }
            }

            if (app.getScheduledStart() != null && app.getScheduledStart().isAfter(Instant.now()) && upcomingList.size() < 5) {
                if (app.getStatus() == AppointmentStatus.ACCEPTED || app.getStatus() == AppointmentStatus.PENDING) {
                    DoctorDashboardDTO.AppointmentSummaryDTO sum = toSummaryDTO(app, timeFormatter, dmyFormatter);
                    sum.setTimeRange(dmyFormatter.withZone(VIETNAM_ZONE).format(app.getScheduledStart()) + " " + sum.getTimeRange());
                    upcomingList.add(sum);
                }
            }
        }

        dashboard.setPendingApprovalsCount(pendingApprovals);
        dashboard.setPendingNotesCount(pendingNotes);
        dashboard.setInProgressAppointmentsCount(inProgress);
        dashboard.setUpcomingSessionsCount(upcomingList.size());
        dashboard.setUpcomingAppointments(upcomingList);

        // 6. Biểu đồ Phân bổ Tuần Thực Tế (Thứ 2 -> Chủ Nhật của tuần hiện tại)
        LocalDate weekMonday = today.with(DayOfWeek.MONDAY);
        LocalDate weekSunday = today.with(DayOfWeek.SUNDAY);
        Instant weekStart = weekMonday.atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant weekEnd = weekSunday.atTime(LocalTime.MAX).atZone(VIETNAM_ZONE).toInstant();

        List<Appointment> thisWeekAppointments = appointmentRepository.findByDoctorIdAndDateRange(doctor.getId(), weekStart, weekEnd);
        int[] weekdayCompletedCounts = new int[7];
        for (Appointment app : thisWeekAppointments) {
            if (app.getScheduledStart() != null && app.getStatus() == AppointmentStatus.COMPLETED) {
                int dayIdx = app.getScheduledStart().atZone(VIETNAM_ZONE).getDayOfWeek().getValue() - 1; // 0 (Mon) -> 6 (Sun)
                if (dayIdx >= 0 && dayIdx < 7) {
                    weekdayCompletedCounts[dayIdx]++;
                }
            }
        }

        String[] weekdayLabels = new String[] { "T2", "T3", "T4", "T5", "T6", "T7", "CN" };
        List<DoctorDashboardDTO.WeeklyChartPointDTO> chart = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            int cnt = weekdayCompletedCounts[i];
            BigDecimal estRev = feePerSession.multiply(new BigDecimal(cnt));
            chart.add(new DoctorDashboardDTO.WeeklyChartPointDTO(weekdayLabels[i], cnt, estRev));
        }
        dashboard.setWeeklyChartData(chart);

        long execTime = System.currentTimeMillis() - startTime;
        dashboard.setExecutionTimeMs(execTime);
        dashboard.setCacheHit(false);

        // 7. Lưu vào Redis Cache Store với TTL
        if (redisTemplate != null) {
            try {
                String json = objectMapper.writeValueAsString(dashboard);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
                LOG.info("💾 [REDIS CACHE SAVED] Đã lưu Dashboard của bác sĩ '{}' (Key: '{}', TTL: {}s, Thời gian tính: {} ms)",
                    doctorLogin, cacheKey, CACHE_TTL_SECONDS, execTime);
            } catch (Exception e) {
                LOG.warn("⚠️ [REDIS CACHE WRITE ERROR] Key: '{}': {}", cacheKey, e.getMessage());
            }
        }

        return dashboard;
    }

    private DoctorDashboardDTO.AppointmentSummaryDTO toSummaryDTO(
        Appointment app,
        DateTimeFormatter timeFormatter,
        DateTimeFormatter dateFormatter
    ) {
        DoctorDashboardDTO.AppointmentSummaryDTO sum = new DoctorDashboardDTO.AppointmentSummaryDTO();
        sum.setId(app.getId());
        sum.setBookingCode(app.getBookingCode());
        sum.setPatientName(app.getPatient() != null && app.getPatient().getFullName() != null
            ? app.getPatient().getFullName()
            : "Thân chủ #" + app.getId());
        sum.setPatientPhone(app.getPatient() != null ? app.getPatient().getPhone() : "");
        sum.setScheduledStart(app.getScheduledStart());
        sum.setScheduledEnd(app.getScheduledEnd());
        sum.setConsultationMode(app.getConsultationMode() != null ? app.getConsultationMode().name() : "ONLINE");
        sum.setStatus(app.getStatus() != null ? app.getStatus().name() : "PENDING");
        sum.setReason(app.getReason());

        if (app.getScheduledStart() != null) {
            String startStr = timeFormatter.format(app.getScheduledStart());
            String endStr = app.getScheduledEnd() != null ? timeFormatter.format(app.getScheduledEnd()) : "";
            sum.setTimeRange(startStr + (endStr.isEmpty() ? "" : " - " + endStr));
        }
        return sum;
    }

    /**
     * Xóa Cache của Bác sĩ khi có thay đổi dữ liệu (Eviction).
     * Xóa trực tiếp danh sách các key đã biết (today, week, month) thay vì dùng lệnh quét KEYS *.
     */
    public boolean evictDoctorDashboardCache(String doctorLogin) {
        if (redisTemplate == null) {
            LOG.warn("⚠️ [REDIS CACHE EVICT SKIPPED] RedisTemplate chưa khả dụng.");
            return false;
        }
        try {
            if (doctorLogin != null && !doctorLogin.isBlank()) {
                List<String> knownKeys = List.of(
                    "dashboard:doctor:" + doctorLogin + ":today",
                    "dashboard:doctor:" + doctorLogin + ":week",
                    "dashboard:doctor:" + doctorLogin + ":month"
                );
                Long deleted = redisTemplate.delete(knownKeys);
                LOG.info("🧹 [REDIS CACHE EVICT] Đã xóa trực tiếp {}/3 keys cho bác sĩ '{}'", deleted, doctorLogin);
                return deleted != null && deleted > 0;
            } else {
                LOG.warn("⚠️ [REDIS CACHE EVICT] Bỏ qua xóa toàn bộ cache khi không xác định được doctorLogin để bảo vệ keyspace Redis");
                return false;
            }
        } catch (Exception e) {
            LOG.warn("⚠️ [REDIS CACHE EVICT ERROR] Xóa cache thất bại cho bác sĩ '{}': {}", doctorLogin, e.getMessage());
            return false;
        }
    }
}
