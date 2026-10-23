package com.ocbs.service;

import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.DoctorSubscription;
import com.ocbs.domain.ServicePackage;
import com.ocbs.domain.enumeration.AppointmentStatus;
import com.ocbs.repository.AppointmentRepository;
import com.ocbs.repository.AvailabilitySlotRepository;
import com.ocbs.repository.DoctorSubscriptionRepository;
import com.ocbs.repository.ServicePackageRepository;
import com.ocbs.service.dto.DoctorSubscriptionQuotaDTO;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service enforcing Service Package benefits and limits for Doctors:
 * - Active subscription verification (status = 'ACTIVE' and expirationDate > now).
 * - maxDailySlotsCapacity: Limits slot creation per calendar day.
 * - maxPatientCapacity: Limits distinct client capacity per doctor.
 * - Non-destructive enforcement: Does not delete existing appointments or slots upon expiration/downgrade,
 *   only prevents new quota-exceeding operations.
 */
@Service
@Transactional(readOnly = true)
public class DoctorSubscriptionEnforcementService {

    private static final Logger LOG = LoggerFactory.getLogger(DoctorSubscriptionEnforcementService.class);
    private static final String ENTITY_NAME = "subscriptionEnforcement";

    private final DoctorSubscriptionRepository doctorSubscriptionRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final AppointmentRepository appointmentRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;

    public DoctorSubscriptionEnforcementService(
        DoctorSubscriptionRepository doctorSubscriptionRepository,
        ServicePackageRepository servicePackageRepository,
        AppointmentRepository appointmentRepository,
        AvailabilitySlotRepository availabilitySlotRepository
    ) {
        this.doctorSubscriptionRepository = doctorSubscriptionRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.appointmentRepository = appointmentRepository;
        this.availabilitySlotRepository = availabilitySlotRepository;
    }

    /**
     * Find active, valid subscription for a doctor.
     * Criteria: status == 'ACTIVE' and expirationDate > now.
     */
    public Optional<DoctorSubscription> getActiveValidSubscription(DoctorProfile doctor) {
        if (doctor == null) {
            return Optional.empty();
        }
        Instant now = Instant.now();

        // 1. Search by doctor ID
        if (doctor.getId() != null) {
            List<DoctorSubscription> subs = doctorSubscriptionRepository.findActiveValidByDoctorId(doctor.getId(), now);
            if (!subs.isEmpty()) {
                return Optional.of(subs.get(0));
            }
        }

        // 2. Search by doctor login
        if (doctor.getUser() != null && doctor.getUser().getLogin() != null) {
            List<DoctorSubscription> subs = doctorSubscriptionRepository.findActiveValidByDoctorLogin(doctor.getUser().getLogin(), now);
            if (!subs.isEmpty()) {
                return Optional.of(subs.get(0));
            }
        }

        return Optional.empty();
    }

    /**
     * Retrieve the ServicePackage entity linked to a subscription.
     */
    public Optional<ServicePackage> getServicePackage(DoctorSubscription subscription) {
        if (subscription == null || subscription.getServicePackageId() == null) {
            return Optional.empty();
        }
        return servicePackageRepository.findById(subscription.getServicePackageId());
    }

    /**
     * Validate that doctor has an active subscription and has not exceeded daily slot capacity for a given date.
     *
     * @param doctor doctor profile
     * @param slotDate calendar date of slot(s)
     * @param slotsToCreate number of slots being added
     */
    public void validateDoctorCanCreateSlots(DoctorProfile doctor, LocalDate slotDate, int slotsToCreate) {
        DoctorSubscription sub = getActiveValidSubscription(doctor)
            .orElseThrow(() -> new BadRequestAlertException(
                "Bác sĩ chưa có gói dịch vụ nào đang kích hoạt hoặc gói dịch vụ đã hết hạn. Vui lòng đăng ký/gia hạn gói để tạo lịch làm việc.",
                ENTITY_NAME,
                "nosubscription"
            ));

        ServicePackage pkg = getServicePackage(sub).orElse(null);
        if (pkg == null) {
            LOG.warn("⚠️ Subscription ID {} has no valid ServicePackage ID {}", sub.getId(), sub.getServicePackageId());
            return;
        }

        if (pkg.getMaxDailySlotsCapacity() != null && pkg.getMaxDailySlotsCapacity() > 0) {
            ZoneId zone = ZoneId.systemDefault();
            Instant startOfDay = slotDate.atStartOfDay(zone).toInstant();
            Instant endOfDay = slotDate.plusDays(1).atStartOfDay(zone).minusNanos(1).toInstant();

            long currentSlotsCount = availabilitySlotRepository.countDoctorSlotsOnDate(doctor.getId(), startOfDay, endOfDay);
            if (currentSlotsCount + slotsToCreate > pkg.getMaxDailySlotsCapacity()) {
                String errorMsg = String.format(
                    "Đã vượt quá giới hạn khung giờ làm việc trong ngày (%d slot/ngày) theo gói dịch vụ '%s'. Ngày %s hiện đã có %d slot, không thể tạo thêm %d slot.",
                    pkg.getMaxDailySlotsCapacity(),
                    pkg.getName(),
                    slotDate.toString(),
                    currentSlotsCount,
                    slotsToCreate
                );
                LOG.warn("🚨 [Quota Exceeded] Doctor ID {}: {}", doctor.getId(), errorMsg);
                throw new BadRequestAlertException(errorMsg, ENTITY_NAME, "dailycapacityexceeded");
            }
        }
    }

    /**
     * Validate that patient can book an appointment with doctor without violating doctor's active package or patient capacity.
     *
     * @param doctor doctor profile
     * @param patientId patient profile ID
     */
    public void validateDoctorCanBeBooked(DoctorProfile doctor, Long patientId) {
        DoctorSubscription sub = getActiveValidSubscription(doctor)
            .orElseThrow(() -> new BadRequestAlertException(
                "Bác sĩ hiện không trong thời gian hoạt động gói dịch vụ hợp lệ để tiếp nhận lịch hẹn mới. Vui lòng chọn bác sĩ khác hoặc quay lại sau.",
                ENTITY_NAME,
                "doctorinactivepackage"
            ));

        ServicePackage pkg = getServicePackage(sub).orElse(null);
        if (pkg == null) {
            return;
        }

        if (pkg.getMaxPatientCapacity() != null && pkg.getMaxPatientCapacity() > 0) {
            List<AppointmentStatus> activeStatuses = List.of(
                AppointmentStatus.PENDING,
                AppointmentStatus.ACCEPTED,
                AppointmentStatus.IN_PROGRESS,
                AppointmentStatus.COMPLETED
            );

            // Check if patient is already an existing client of this doctor
            boolean alreadyClient = appointmentRepository.existsByDoctorIdAndPatientIdAndStatusIn(doctor.getId(), patientId, activeStatuses);
            if (!alreadyClient) {
                long currentClientCount = appointmentRepository.countDistinctPatientsByDoctorId(doctor.getId(), activeStatuses);
                if (currentClientCount >= pkg.getMaxPatientCapacity()) {
                    String errorMsg = String.format(
                        "Bác sĩ %s đã đạt giới hạn tối đa số thân chủ tiếp nhận (%d thân chủ) theo gói dịch vụ '%s'. Vui lòng chọn bác sĩ khác hoặc liên hệ hỗ trợ phòng khám.",
                        doctor.getFullName(),
                        pkg.getMaxPatientCapacity(),
                        pkg.getName()
                    );
                    LOG.warn("🚨 [Patient Capacity Exceeded] Doctor ID {}: {}", doctor.getId(), errorMsg);
                    throw new BadRequestAlertException(errorMsg, ENTITY_NAME, "maxpatientcapacityreached");
                }
            }
        }
    }

    /**
     * Validate that doctor can accept an appointment from a patient without violating maxPatientCapacity.
     *
     * @param doctor doctor profile
     * @param patientId patient profile ID
     */
    public void validateDoctorCanAcceptPatient(DoctorProfile doctor, Long patientId) {
        DoctorSubscription sub = getActiveValidSubscription(doctor)
            .orElseThrow(() -> new BadRequestAlertException(
                "Gói dịch vụ của bạn đã hết hạn hoặc chưa kích hoạt. Vui lòng gia hạn gói để tiếp nhận và duyệt lịch hẹn của thân chủ.",
                ENTITY_NAME,
                "nosubscription"
            ));

        ServicePackage pkg = getServicePackage(sub).orElse(null);
        if (pkg == null) {
            return;
        }

        if (pkg.getMaxPatientCapacity() != null && pkg.getMaxPatientCapacity() > 0) {
            List<AppointmentStatus> activeStatuses = List.of(
                AppointmentStatus.ACCEPTED,
                AppointmentStatus.IN_PROGRESS,
                AppointmentStatus.COMPLETED
            );

            boolean alreadyClient = appointmentRepository.existsByDoctorIdAndPatientIdAndStatusIn(doctor.getId(), patientId, activeStatuses);
            if (!alreadyClient) {
                long currentClientCount = appointmentRepository.countDistinctPatientsByDoctorId(doctor.getId(), activeStatuses);
                if (currentClientCount >= pkg.getMaxPatientCapacity()) {
                    String errorMsg = String.format(
                        "Bạn đã đạt giới hạn tối đa số thân chủ tiếp nhận (%d thân chủ) theo gói dịch vụ '%s'. Vui lòng nâng cấp gói để tiếp nhận thêm thân chủ mới.",
                        pkg.getMaxPatientCapacity(),
                        pkg.getName()
                    );
                    LOG.warn("🚨 [Patient Capacity Exceeded] Doctor ID {}: {}", doctor.getId(), errorMsg);
                    throw new BadRequestAlertException(errorMsg, ENTITY_NAME, "maxpatientcapacityreached");
                }
            }
        }
    }

    /**
     * Get detailed quota overview for doctor.
     */
    public DoctorSubscriptionQuotaDTO getDoctorSubscriptionQuota(DoctorProfile doctor) {
        DoctorSubscriptionQuotaDTO dto = new DoctorSubscriptionQuotaDTO();
        Optional<DoctorSubscription> subOpt = getActiveValidSubscription(doctor);

        if (subOpt.isEmpty()) {
            dto.setHasActiveSubscription(false);
            dto.setStatus("INACTIVE_OR_EXPIRED");
            dto.setMessage("Bác sĩ hiện không có gói dịch vụ nào đang kích hoạt. Vui lòng đăng ký gói dịch vụ để mở khóa đầy đủ quyền lợi.");
            return dto;
        }

        DoctorSubscription sub = subOpt.get();
        dto.setHasActiveSubscription(true);
        dto.setSubscriptionId(sub.getId());
        dto.setServicePackageId(sub.getServicePackageId());
        dto.setServicePackageName(sub.getServicePackageName());
        dto.setStatus(sub.getStatus());
        dto.setStartDate(sub.getStartDate());
        dto.setExpirationDate(sub.getExpirationDate());

        if (sub.getExpirationDate() != null) {
            long days = Duration.between(Instant.now(), sub.getExpirationDate()).toDays();
            dto.setDaysRemaining(Math.max(0, days));
        }

        ServicePackage pkg = getServicePackage(sub).orElse(null);
        if (pkg != null) {
            dto.setMaxDailySlotsCapacity(pkg.getMaxDailySlotsCapacity());
            dto.setMaxPatientCapacity(pkg.getMaxPatientCapacity());

            List<AppointmentStatus> activeStatuses = List.of(
                AppointmentStatus.PENDING,
                AppointmentStatus.ACCEPTED,
                AppointmentStatus.IN_PROGRESS,
                AppointmentStatus.COMPLETED
            );
            long patientCount = appointmentRepository.countDistinctPatientsByDoctorId(doctor.getId(), activeStatuses);
            dto.setCurrentPatientCount(patientCount);

            if (pkg.getMaxPatientCapacity() != null && pkg.getMaxPatientCapacity() > 0) {
                long remaining = Math.max(0, pkg.getMaxPatientCapacity() - patientCount);
                dto.setRemainingPatientCapacity(remaining);
            }
        }

        dto.setMessage("Gói dịch vụ đang kích hoạt và có hiệu lực.");
        return dto;
    }
}
