package com.ocbs.web.rest;

import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.Specialization;
import com.ocbs.repository.DoctorProfileRepository;
import com.ocbs.repository.SpecializationRepository;
import com.ocbs.service.DoctorProfileService;
import com.ocbs.service.dto.DoctorProfileDTO;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 🧪 CHAOS LAB 07: Bắt & Trị Lỗi LazyInitializationException & Jackson Serialization Bug
 */
@RestController
@RequestMapping("/api/chaos/lazy-bug")
public class JpaLazySerializationChaosResource {

    private static final Logger LOG = LoggerFactory.getLogger(JpaLazySerializationChaosResource.class);

    private final DoctorProfileRepository doctorProfileRepository;
    private final SpecializationRepository specializationRepository;
    private final DoctorProfileService doctorProfileService;

    @PersistenceContext
    private EntityManager entityManager;

    public JpaLazySerializationChaosResource(
        DoctorProfileRepository doctorProfileRepository,
        SpecializationRepository specializationRepository,
        DoctorProfileService doctorProfileService
    ) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.specializationRepository = specializationRepository;
        this.doctorProfileService = doctorProfileService;
    }

    /**
     * ❌ THỬ NGHIỆM 1: KÍCH NỔ LazyInitializationException
     * Nguyên nhân: Khi Entity bị Detached (ngoài Transaction / Session đã đóng),
     * việc gọi getter trên quan hệ LAZY sẽ NÉM RA LazyInitializationException!
     */
    @GetMapping("/trigger-lazy-exception")
    @Transactional
    public ResponseEntity<Map<String, Object>> triggerLazyException(@RequestParam(defaultValue = "1") Long id) {
        LOG.warn("🚨 [CHAOS 07] Bắt đầu thử nghiệm LazyInitializationException cho Doctor ID: {}...", id);

        // 1. Tìm Doctor trong Transaction
        DoctorProfile doctor = doctorProfileRepository.findById(id).orElse(null);
        if (doctor == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. ⚡ CỐ TÌNH DETACH ENTITY KHỎI HIBERNATE PERSISTENCE CONTEXT (Mô phỏng Session đã đóng)
        entityManager.detach(doctor);
        LOG.info("✅ Đã DETACH DoctorProfile (ID={}) khỏi EntityManager/Session!", doctor.getId());

        // 3. ⚠️ TRUY CẬP QUAN HỆ LAZY KHI ĐÃ BỊ DETACH:
        // Đoạn code dưới đây sẽ NÉM RA: org.hibernate.LazyInitializationException: could not initialize proxy - no Session
        LOG.warn("⚠️ Đang cố gắng gọi doctor.getSpecializationses().size() trên Detached Entity...");
        int specCount = doctor.getSpecializationses().size();

        return ResponseEntity.ok(Map.of(
            "doctorId", doctor.getId(),
            "fullName", doctor.getFullName(),
            "specializationCount", specCount
        ));
    }

    /**
     * ❌ THỬ NGHIỆM 2: TRẢ TRỰC TIẾP ENTITY RA CONTROLLER GÂY VÒNG LẶP HOẶC LỖI JACKSON PROXY
     * Trả về Entity JPA chứa trường quan hệ 2 chiều mà không qua DTO.
     */
    @GetMapping("/raw-entity-leak")
    @Transactional(readOnly = true)
    public ResponseEntity<DoctorProfile> getRawEntityLeakingProxies(@RequestParam(defaultValue = "1") Long id) {
        LOG.warn("🚨 [CHAOS 07] Trả trực tiếp Entity JPA ra Controller (Anti-Pattern)...");
        DoctorProfile doctor = doctorProfileRepository.findById(id).orElse(null);
        // Trả trực tiếp entity cho Jackson ObjectMapper serialize ra JSON
        return ResponseEntity.ok(doctor);
    }

    /**
     * 🛡️ GIẢI PHÁP CHUẨN PRODUCTION: DTO PATTERN (MapStruct / Projection)
     * Toàn bộ việc ánh xạ từ Entity sang DTO được thực hiện BÊN TRONG Transaction Boundary.
     * Tầng Controller chỉ nhận DTO thuần túy (POJO), hoàn toàn không phụ thuộc vào Hibernate Session!
     */
    @GetMapping("/safe-dto")
    public ResponseEntity<DoctorProfileDTO> getSafeDoctorDto(@RequestParam(defaultValue = "1") Long id) {
        LOG.info("🛡️ [CLEAN ARCHITECTURE] Lấy thông tin Bác sĩ an toàn qua DTO Pattern...");
        Optional<DoctorProfileDTO> dtoOptional = doctorProfileService.findOne(id);
        return dtoOptional.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
