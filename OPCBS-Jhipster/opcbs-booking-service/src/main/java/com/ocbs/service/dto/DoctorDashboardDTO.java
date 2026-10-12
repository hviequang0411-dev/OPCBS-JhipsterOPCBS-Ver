package com.ocbs.service.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DoctorDashboardDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 1. Doctor Header Info
    private Long doctorId;
    private String doctorName;
    private String professionalTitle;
    private String avatarUrl;
    private String phone;
    private String email;
    private boolean isVerified;
    private String verificationStatus;
    private String greeting;

    // 2. Filter Period
    private String period; // today, week, month

    // 3. KPI Metrics
    private int todaySessionsCount;
    private int todayCompletedCount;
    private int todayUpcomingCount;
    private int upcomingSessionsCount;
    
    private int appointmentsThisMonth;
    private double appointmentsGrowthPercent;
    private boolean isAppointmentsGrowthPositive;

    private int newPatientsThisMonth;
    private int activePatientsCount;
    private int returningPatientsCount;
    private double returningPatientRate;

    private BigDecimal revenueThisMonth;
    private double revenueGrowthPercent;
    private boolean isRevenueGrowthPositive;

    // 4. Action Center (Cần xử lý ngay)
    private int pendingApprovalsCount;
    private int pendingNotesCount;
    private int inProgressAppointmentsCount;

    // 5. Today's Sessions & Upcoming Appointments
    private List<AppointmentSummaryDTO> todaySessions = new ArrayList<>();
    private List<AppointmentSummaryDTO> upcomingAppointments = new ArrayList<>();

    // 6. Analytics & Retention Distribution
    private List<WeeklyChartPointDTO> weeklyChartData = new ArrayList<>();
    private int singleVisitPercent;
    private int repeat2To3Percent;
    private int loyal4PlusPercent;

    // 7. Redis Cache Metadata
    private boolean cacheHit;
    private String cacheKey;
    private Instant cachedAt;
    private long executionTimeMs;

    public static class AppointmentSummaryDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String bookingCode;
        private String patientName;
        private String patientPhone;
        private String timeRange;
        private Instant scheduledStart;
        private Instant scheduledEnd;
        private String consultationMode;
        private String status;
        private String reason;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getBookingCode() { return bookingCode; }
        public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }
        public String getPatientName() { return patientName; }
        public void setPatientName(String patientName) { this.patientName = patientName; }
        public String getPatientPhone() { return patientPhone; }
        public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }
        public String getTimeRange() { return timeRange; }
        public void setTimeRange(String timeRange) { this.timeRange = timeRange; }
        public Instant getScheduledStart() { return scheduledStart; }
        public void setScheduledStart(Instant scheduledStart) { this.scheduledStart = scheduledStart; }
        public Instant getScheduledEnd() { return scheduledEnd; }
        public void setScheduledEnd(Instant scheduledEnd) { this.scheduledEnd = scheduledEnd; }
        public String getConsultationMode() { return consultationMode; }
        public void setConsultationMode(String consultationMode) { this.consultationMode = consultationMode; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class WeeklyChartPointDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String dayOfWeek; // Thứ 2, Thứ 3, ...
        private int sessionsCount;
        private BigDecimal revenue;

        public WeeklyChartPointDTO() {}
        public WeeklyChartPointDTO(String dayOfWeek, int sessionsCount, BigDecimal revenue) {
            this.dayOfWeek = dayOfWeek;
            this.sessionsCount = sessionsCount;
            this.revenue = revenue;
        }
        public String getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
        public int getSessionsCount() { return sessionsCount; }
        public void setSessionsCount(int sessionsCount) { this.sessionsCount = sessionsCount; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    }

    // Getters and Setters
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public String getProfessionalTitle() { return professionalTitle; }
    public void setProfessionalTitle(String professionalTitle) { this.professionalTitle = professionalTitle; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
    public String getGreeting() { return greeting; }
    public void setGreeting(String greeting) { this.greeting = greeting; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public int getTodaySessionsCount() { return todaySessionsCount; }
    public void setTodaySessionsCount(int todaySessionsCount) { this.todaySessionsCount = todaySessionsCount; }
    public int getTodayCompletedCount() { return todayCompletedCount; }
    public void setTodayCompletedCount(int todayCompletedCount) { this.todayCompletedCount = todayCompletedCount; }
    public int getTodayUpcomingCount() { return todayUpcomingCount; }
    public void setTodayUpcomingCount(int todayUpcomingCount) { this.todayUpcomingCount = todayUpcomingCount; }
    public int getUpcomingSessionsCount() { return upcomingSessionsCount; }
    public void setUpcomingSessionsCount(int upcomingSessionsCount) { this.upcomingSessionsCount = upcomingSessionsCount; }
    public int getAppointmentsThisMonth() { return appointmentsThisMonth; }
    public void setAppointmentsThisMonth(int appointmentsThisMonth) { this.appointmentsThisMonth = appointmentsThisMonth; }
    public double getAppointmentsGrowthPercent() { return appointmentsGrowthPercent; }
    public void setAppointmentsGrowthPercent(double appointmentsGrowthPercent) { this.appointmentsGrowthPercent = appointmentsGrowthPercent; }
    public boolean isAppointmentsGrowthPositive() { return isAppointmentsGrowthPositive; }
    public void setAppointmentsGrowthPositive(boolean appointmentsGrowthPositive) { isAppointmentsGrowthPositive = appointmentsGrowthPositive; }
    public int getNewPatientsThisMonth() { return newPatientsThisMonth; }
    public void setNewPatientsThisMonth(int newPatientsThisMonth) { this.newPatientsThisMonth = newPatientsThisMonth; }
    public int getActivePatientsCount() { return activePatientsCount; }
    public void setActivePatientsCount(int activePatientsCount) { this.activePatientsCount = activePatientsCount; }
    public int getReturningPatientsCount() { return returningPatientsCount; }
    public void setReturningPatientsCount(int returningPatientsCount) { this.returningPatientsCount = returningPatientsCount; }
    public double getReturningPatientRate() { return returningPatientRate; }
    public void setReturningPatientRate(double returningPatientRate) { this.returningPatientRate = returningPatientRate; }
    public BigDecimal getRevenueThisMonth() { return revenueThisMonth; }
    public void setRevenueThisMonth(BigDecimal revenueThisMonth) { this.revenueThisMonth = revenueThisMonth; }
    public double getRevenueGrowthPercent() { return revenueGrowthPercent; }
    public void setRevenueGrowthPercent(double revenueGrowthPercent) { this.revenueGrowthPercent = revenueGrowthPercent; }
    public boolean isRevenueGrowthPositive() { return isRevenueGrowthPositive; }
    public void setRevenueGrowthPositive(boolean revenueGrowthPositive) { isRevenueGrowthPositive = revenueGrowthPositive; }
    public int getPendingApprovalsCount() { return pendingApprovalsCount; }
    public void setPendingApprovalsCount(int pendingApprovalsCount) { this.pendingApprovalsCount = pendingApprovalsCount; }
    public int getPendingNotesCount() { return pendingNotesCount; }
    public void setPendingNotesCount(int pendingNotesCount) { this.pendingNotesCount = pendingNotesCount; }
    public int getInProgressAppointmentsCount() { return inProgressAppointmentsCount; }
    public void setInProgressAppointmentsCount(int inProgressAppointmentsCount) { this.inProgressAppointmentsCount = inProgressAppointmentsCount; }
    public List<AppointmentSummaryDTO> getTodaySessions() { return todaySessions; }
    public void setTodaySessions(List<AppointmentSummaryDTO> todaySessions) { this.todaySessions = todaySessions; }
    public List<AppointmentSummaryDTO> getUpcomingAppointments() { return upcomingAppointments; }
    public void setUpcomingAppointments(List<AppointmentSummaryDTO> upcomingAppointments) { this.upcomingAppointments = upcomingAppointments; }
    public List<WeeklyChartPointDTO> getWeeklyChartData() { return weeklyChartData; }
    public void setWeeklyChartData(List<WeeklyChartPointDTO> weeklyChartData) { this.weeklyChartData = weeklyChartData; }
    public int getSingleVisitPercent() { return singleVisitPercent; }
    public void setSingleVisitPercent(int singleVisitPercent) { this.singleVisitPercent = singleVisitPercent; }
    public int getRepeat2To3Percent() { return repeat2To3Percent; }
    public void setRepeat2To3Percent(int repeat2To3Percent) { this.repeat2To3Percent = repeat2To3Percent; }
    public int getLoyal4PlusPercent() { return loyal4PlusPercent; }
    public void setLoyal4PlusPercent(int loyal4PlusPercent) { this.loyal4PlusPercent = loyal4PlusPercent; }
    public boolean isCacheHit() { return cacheHit; }
    public void setCacheHit(boolean cacheHit) { this.cacheHit = cacheHit; }
    public String getCacheKey() { return cacheKey; }
    public void setCacheKey(String cacheKey) { this.cacheKey = cacheKey; }
    public Instant getCachedAt() { return cachedAt; }
    public void setCachedAt(Instant cachedAt) { this.cachedAt = cachedAt; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
}
