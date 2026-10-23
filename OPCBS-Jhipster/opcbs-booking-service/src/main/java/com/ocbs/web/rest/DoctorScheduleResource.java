package com.ocbs.web.rest;

import com.ocbs.domain.AvailabilitySlot;
import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.enumeration.SlotStatus;
import com.ocbs.repository.AvailabilitySlotRepository;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.security.AuthoritiesConstants;
import com.ocbs.security.SecurityUtils;
import com.ocbs.service.dto.AvailabilitySlotDTO;
import com.ocbs.service.mapper.AvailabilitySlotMapper;
import com.ocbs.web.rest.errors.BadRequestAlertException;
import com.ocbs.web.rest.vm.DoctorSlotVM;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing Doctor's Schedule and Availability Slots.
 */
@RestController
@RequestMapping("/api/doctor")
@PreAuthorize("hasAuthority('" + AuthoritiesConstants.DOCTOR + "')")
@Transactional
public class DoctorScheduleResource {

    private static final Logger LOG = LoggerFactory.getLogger(DoctorScheduleResource.class);
    private static final String ENTITY_NAME = "doctorSchedule";

    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final AvailabilitySlotMapper availabilitySlotMapper;
    private final com.ocbs.repository.AppointmentRepository appointmentRepository;
    private final com.ocbs.service.DoctorSubscriptionEnforcementService doctorSubscriptionEnforcementService;

    public DoctorScheduleResource(
        AvailabilitySlotRepository availabilitySlotRepository,
        DoctorProfileRepository doctorProfileRepository,
        AvailabilitySlotMapper availabilitySlotMapper,
        com.ocbs.repository.AppointmentRepository appointmentRepository,
        com.ocbs.service.DoctorSubscriptionEnforcementService doctorSubscriptionEnforcementService
    ) {
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.availabilitySlotMapper = availabilitySlotMapper;
        this.appointmentRepository = appointmentRepository;
        this.doctorSubscriptionEnforcementService = doctorSubscriptionEnforcementService;
    }

    /**
     * Helper to get current authenticated Doctor Profile.
     */
    private DoctorProfile getCurrentDoctorProfile() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", ENTITY_NAME, "notauthenticated"));

        return doctorProfileRepository
            .findByUser_Login(login)
            .orElseThrow(() -> new BadRequestAlertException("Doctor profile not found for current user", ENTITY_NAME, "doctornotfound"));
    }

    /**
     * {@code GET  /doctor/my-slots} : Get all availability slots of current doctor.
     *
     * @param startDate optional start of time range.
     * @param endDate optional end of time range.
     * @return list of AvailabilitySlotDTO.
     */
    @GetMapping("/my-slots")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AvailabilitySlotDTO>> getMyAvailabilitySlots(
        @RequestParam(required = false) Instant startDate,
        @RequestParam(required = false) Instant endDate
    ) {
        DoctorProfile doctor = getCurrentDoctorProfile();
        LOG.debug("REST request to get slots for doctor ID: {}, from: {} to: {}", doctor.getId(), startDate, endDate);

        List<AvailabilitySlot> slots;
        if (startDate != null && endDate != null) {
            slots = availabilitySlotRepository.findByDoctor_IdAndStartTimeBetweenOrderByStartTimeAsc(doctor.getId(), startDate, endDate);
        } else {
            slots = availabilitySlotRepository.findByDoctor_IdOrderByStartTimeAsc(doctor.getId());
        }

        List<AvailabilitySlotDTO> dtoList = availabilitySlotMapper.toDto(slots);
        if (!dtoList.isEmpty()) {
            List<Long> slotIds = dtoList.stream().map(AvailabilitySlotDTO::getId).toList();
            List<com.ocbs.domain.Appointment> appointments = appointmentRepository.findBySlot_IdIn(slotIds);
            java.util.Map<Long, com.ocbs.domain.Appointment> slotToAppMap = appointments.stream()
                .filter(a -> a.getSlot() != null && a.getSlot().getId() != null)
                .collect(java.util.stream.Collectors.toMap(a -> a.getSlot().getId(), a -> a, (existing, replacement) -> existing));

            for (AvailabilitySlotDTO dto : dtoList) {
                com.ocbs.domain.Appointment app = slotToAppMap.get(dto.getId());
                if (app != null) {
                    dto.setAppointmentId(app.getId());
                    dto.setAppointmentBookingCode(app.getBookingCode());
                    if (app.getPatient() != null) {
                        dto.setPatientName(app.getPatient().getFullName());
                        dto.setPatientPhone(app.getPatient().getPhone());
                    }
                    dto.setAppointmentStatus(app.getStatus());
                }
            }
        }

        return ResponseEntity.ok(dtoList);
    }

    /**
     * {@code POST  /doctor/slots} : Create a single availability slot for current doctor.
     *
     * @param vm the slot creation details.
     * @return created AvailabilitySlotDTO.
     */
    @PostMapping("/slots")
    public ResponseEntity<AvailabilitySlotDTO> createSlot(@Valid @RequestBody DoctorSlotVM.CreateSlotVM vm) throws URISyntaxException {
        DoctorProfile doctor = getCurrentDoctorProfile();
        LOG.debug("REST request to create slot for doctor ID: {}, time: {} -> {}", doctor.getId(), vm.getStartTime(), vm.getEndTime());

        if (vm.getStartTime() == null || vm.getEndTime() == null) {
            throw new BadRequestAlertException("Start time and end time are required", ENTITY_NAME, "timerequired");
        }
        if (vm.getStartTime().isBefore(Instant.now())) {
            throw new BadRequestAlertException("Không thể tạo khung giờ làm việc trong quá khứ", ENTITY_NAME, "pasttime");
        }
        if (!vm.getStartTime().isBefore(vm.getEndTime())) {
            throw new BadRequestAlertException("Start time must be before end time", ENTITY_NAME, "invalidtimeorder");
        }

        // Kiểm tra subscription và giới hạn số slot tối đa trong ngày (maxDailySlotsCapacity)
        ZoneId zone = ZoneId.systemDefault();
        LocalDate slotDate = vm.getStartTime().atZone(zone).toLocalDate();
        doctorSubscriptionEnforcementService.validateDoctorCanCreateSlots(doctor, slotDate, 1);

        // Overlap check
        if (availabilitySlotRepository.existsOverlap(doctor.getId(), vm.getStartTime(), vm.getEndTime())) {
            throw new BadRequestAlertException("Slot overlaps with an existing time slot in your schedule", ENTITY_NAME, "slotoverlap");
        }

        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setStartTime(vm.getStartTime());
        slot.setEndTime(vm.getEndTime());
        slot.setConsultationMode(vm.getConsultationMode());
        slot.setStatus(SlotStatus.AVAILABLE);
        slot.setNotes(vm.getNotes());
        slot.setDoctor(doctor);

        AvailabilitySlot savedSlot = availabilitySlotRepository.save(slot);
        AvailabilitySlotDTO result = availabilitySlotMapper.toDto(savedSlot);

        return ResponseEntity.created(new URI("/api/doctor/slots/" + result.getId())).body(result);
    }

    /**
     * {@code POST  /doctor/slots/batch} : Batch generate availability slots for current doctor.
     *
     * @param vm the batch configuration.
     * @return list of created AvailabilitySlotDTOs.
     */
    @PostMapping("/slots/batch")
    public ResponseEntity<List<AvailabilitySlotDTO>> batchCreateSlots(@Valid @RequestBody DoctorSlotVM.BatchCreateSlotVM vm) {
        DoctorProfile doctor = getCurrentDoctorProfile();
        LOG.debug("REST request to batch create slots for doctor ID: {}", doctor.getId());

        // Kiểm tra subscription còn hiệu lực trước khi tạo batch
        doctorSubscriptionEnforcementService.getActiveValidSubscription(doctor)
            .orElseThrow(() -> new BadRequestAlertException(
                "Bác sĩ chưa có gói dịch vụ nào đang kích hoạt hoặc gói dịch vụ đã hết hạn. Vui lòng đăng ký/gia hạn gói để tạo lịch làm việc.",
                ENTITY_NAME,
                "nosubscription"
            ));

        LocalDate start = LocalDate.parse(vm.getStartDate());
        LocalDate end = LocalDate.parse(vm.getEndDate());
        LocalTime dailyStart = LocalTime.parse(vm.getDailyStartTime());
        LocalTime dailyEnd = LocalTime.parse(vm.getDailyEndTime());

        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        if (end.isBefore(today)) {
            throw new BadRequestAlertException("Khoảng ngày tạo lịch không thể hoàn toàn ở trong quá khứ", ENTITY_NAME, "pastdate");
        }
        if (start.isAfter(end)) {
            throw new BadRequestAlertException("Start date must be before or equal to end date", ENTITY_NAME, "invaliddateorder");
        }
        if (!dailyStart.isBefore(dailyEnd)) {
            throw new BadRequestAlertException("Daily start time must be before daily end time", ENTITY_NAME, "invalidtimeorder");
        }

        int duration = vm.getSlotDurationMinutes() > 0 ? vm.getSlotDurationMinutes() : 60;
        List<Integer> targetDays = (vm.getDaysOfWeek() != null && !vm.getDaysOfWeek().isEmpty())
            ? vm.getDaysOfWeek()
            : List.of(1, 2, 3, 4, 5); // default Mon-Fri

        List<AvailabilitySlot> createdSlots = new ArrayList<>();

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            int dayOfWeek = date.getDayOfWeek().getValue(); // 1=Mon ... 7=Sun
            if (!targetDays.contains(dayOfWeek)) {
                continue;
            }

            List<AvailabilitySlot> dateSlots = new ArrayList<>();
            LocalTime currentSlotStart = dailyStart;
            while (true) {
                LocalTime currentSlotEnd = currentSlotStart.plusMinutes(duration);
                if (currentSlotEnd.isAfter(dailyEnd)) {
                    break;
                }

                Instant slotStartInstant = ZonedDateTime.of(date, currentSlotStart, zone).toInstant();
                Instant slotEndInstant = ZonedDateTime.of(date, currentSlotEnd, zone).toInstant();

                // Skip if slot start is in the past
                if (slotStartInstant.isBefore(Instant.now())) {
                    currentSlotStart = currentSlotEnd;
                    continue;
                }

                // Skip if overlaps
                if (!availabilitySlotRepository.existsOverlap(doctor.getId(), slotStartInstant, slotEndInstant)) {
                    AvailabilitySlot slot = new AvailabilitySlot();
                    slot.setStartTime(slotStartInstant);
                    slot.setEndTime(slotEndInstant);
                    slot.setConsultationMode(vm.getConsultationMode());
                    slot.setStatus(SlotStatus.AVAILABLE);
                    slot.setNotes(vm.getNotes());
                    slot.setDoctor(doctor);
                    dateSlots.add(slot);
                }

                currentSlotStart = currentSlotEnd;
            }

            if (!dateSlots.isEmpty()) {
                // Kiểm tra giới hạn maxDailySlotsCapacity cho từng ngày cụ thể
                doctorSubscriptionEnforcementService.validateDoctorCanCreateSlots(doctor, date, dateSlots.size());
                createdSlots.addAll(dateSlots);
            }
        }

        if (createdSlots.isEmpty()) {
            throw new BadRequestAlertException("Không có khung giờ hợp lệ nào được tạo (do tất cả thời gian đã qua trong quá khứ hoặc bị trùng lịch)", ENTITY_NAME, "noslotscreated");
        }

        List<AvailabilitySlot> savedSlots = availabilitySlotRepository.saveAll(createdSlots);
        return ResponseEntity.ok(availabilitySlotMapper.toDto(savedSlots));
    }

    /**
     * {@code GET  /doctor/subscription-quota} : Lấy thông tin quyền lợi và hạn ngạch gói dịch vụ hiện hành của bác sĩ.
     */
    @GetMapping("/subscription-quota")
    @Transactional(readOnly = true)
    public ResponseEntity<com.ocbs.service.dto.DoctorSubscriptionQuotaDTO> getMySubscriptionQuota() {
        DoctorProfile doctor = getCurrentDoctorProfile();
        return ResponseEntity.ok(doctorSubscriptionEnforcementService.getDoctorSubscriptionQuota(doctor));
    }

    /**
     * {@code PUT  /doctor/slots/:id} : Update an availability slot for current doctor.
     *
     * @param id the ID of the slot.
     * @param vm the update details.
     * @return updated AvailabilitySlotDTO.
     */
    @PutMapping("/slots/{id}")
    public ResponseEntity<AvailabilitySlotDTO> updateSlot(
        @PathVariable Long id,
        @Valid @RequestBody DoctorSlotVM.CreateSlotVM vm
    ) {
        DoctorProfile doctor = getCurrentDoctorProfile();
        LOG.debug("REST request to update slot ID: {} for doctor ID: {}", id, doctor.getId());

        AvailabilitySlot slot = availabilitySlotRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Slot not found", ENTITY_NAME, "slotnotfound"));

        if (!slot.getDoctor().getId().equals(doctor.getId())) {
            throw new BadRequestAlertException("You can only modify your own slots", ENTITY_NAME, "unauthorizedslot");
        }

        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new BadRequestAlertException("Cannot edit a slot that has already been booked by a patient", ENTITY_NAME, "slotbooked");
        }

        if (vm.getStartTime() == null || vm.getEndTime() == null) {
            throw new BadRequestAlertException("Start time and end time are required", ENTITY_NAME, "timerequired");
        }

        if (vm.getStartTime().isBefore(Instant.now())) {
            throw new BadRequestAlertException("Không thể cập nhật khung giờ thành thời gian trong quá khứ", ENTITY_NAME, "pasttime");
        }

        if (!vm.getStartTime().isBefore(vm.getEndTime())) {
            throw new BadRequestAlertException("Start time must be before end time", ENTITY_NAME, "invalidtimeorder");
        }

        // Overlap check excluding this slot
        if (availabilitySlotRepository.existsOverlapExcluding(doctor.getId(), id, vm.getStartTime(), vm.getEndTime())) {
            throw new BadRequestAlertException("Updated time overlaps with another slot in your schedule", ENTITY_NAME, "slotoverlap");
        }

        slot.setStartTime(vm.getStartTime());
        slot.setEndTime(vm.getEndTime());
        slot.setConsultationMode(vm.getConsultationMode());
        slot.setNotes(vm.getNotes());

        AvailabilitySlot updatedSlot = availabilitySlotRepository.save(slot);
        return ResponseEntity.ok(availabilitySlotMapper.toDto(updatedSlot));
    }

    /**
     * {@code PUT  /doctor/slots/:id/toggle-status} : Toggle slot status between AVAILABLE and UNAVAILABLE.
     *
     * @param id the ID of the slot.
     * @return updated AvailabilitySlotDTO.
     */
    @PutMapping("/slots/{id}/toggle-status")
    public ResponseEntity<AvailabilitySlotDTO> toggleSlotStatus(@PathVariable Long id) {
        DoctorProfile doctor = getCurrentDoctorProfile();
        LOG.debug("REST request to toggle slot status ID: {} for doctor ID: {}", id, doctor.getId());

        AvailabilitySlot slot = availabilitySlotRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Slot not found", ENTITY_NAME, "slotnotfound"));

        if (!slot.getDoctor().getId().equals(doctor.getId())) {
            throw new BadRequestAlertException("You can only modify your own slots", ENTITY_NAME, "unauthorizedslot");
        }

        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new BadRequestAlertException("Cannot toggle status of a booked slot", ENTITY_NAME, "slotbooked");
        }

        if (slot.getStartTime().isBefore(Instant.now())) {
            throw new BadRequestAlertException("Không thể thay đổi trạng thái khung giờ đã qua trong quá khứ", ENTITY_NAME, "pasttime");
        }

        if (slot.getStatus() == SlotStatus.AVAILABLE) {
            slot.setStatus(SlotStatus.CANCELLED);
        } else {
            slot.setStatus(SlotStatus.AVAILABLE);
        }

        AvailabilitySlot updatedSlot = availabilitySlotRepository.save(slot);
        return ResponseEntity.ok(availabilitySlotMapper.toDto(updatedSlot));
    }

    /**
     * {@code DELETE  /doctor/slots/:id} : Delete an availability slot.
     *
     * @param id the ID of the slot.
     * @return 204 No Content.
     */
    @DeleteMapping("/slots/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) {
        DoctorProfile doctor = getCurrentDoctorProfile();
        LOG.debug("REST request to delete slot ID: {} for doctor ID: {}", id, doctor.getId());

        AvailabilitySlot slot = availabilitySlotRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Slot not found", ENTITY_NAME, "slotnotfound"));

        if (!slot.getDoctor().getId().equals(doctor.getId())) {
            throw new BadRequestAlertException("You can only delete your own slots", ENTITY_NAME, "unauthorizedslot");
        }

        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new BadRequestAlertException("Cannot delete a slot that has already been booked by a patient", ENTITY_NAME, "slotbooked");
        }

        availabilitySlotRepository.delete(slot);
        return ResponseEntity.noContent().build();
    }
}
