package com.ocbs.web.rest;

import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.Specialization;
import com.ocbs.repository.DoctorProfileRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 🧪 CHAOS LAB 08: Bắt & Trị Lỗi N+1 Query Problem trong Spring Data JPA & Hibernate
 */
@RestController
@RequestMapping("/api/chaos/n-plus-one")
public class DoctorNPlusOneChaosResource {

    private static final Logger LOG = LoggerFactory.getLogger(DoctorNPlusOneChaosResource.class);

    private final DoctorProfileRepository doctorProfileRepository;
    private final JdbcTemplate jdbcTemplate;

    public DoctorNPlusOneChaosResource(DoctorProfileRepository doctorProfileRepository, JdbcTemplate jdbcTemplate) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 🚀 SEED NHANH 1,000 BÁC SĨ ĐỂ KÍCH NỔ N+1 TẢI LỚN
     */
    @PostMapping("/seed-doctors")
    @Transactional
    public ResponseEntity<Map<String, Object>> seedDoctors(@RequestParam(defaultValue = "1000") int count) {
        LOG.warn("🌱 [CHAOS SEED] BẮT ĐẦU SEED {} BÁC SĨ VÀO DATABASE...", count);
        long start = System.currentTimeMillis();

        // 1. Lấy hoặc tạo specialization mẫu
        Long specId = jdbcTemplate.query(
            "SELECT id FROM specialization LIMIT 1",
            rs -> rs.next() ? rs.getLong(1) : null
        );
        if (specId == null) {
            jdbcTemplate.update("INSERT INTO specialization (name, description) VALUES ('Tâm lý trị liệu', 'Chuyên khoa mẫu')");
            specId = jdbcTemplate.queryForObject("SELECT id FROM specialization LIMIT 1", Long.class);
        }

        long baseTime = System.currentTimeMillis();
        for (int i = 1; i <= count; i++) {
            String login = "doc_chaos_" + baseTime + "_" + i;
            String email = login + "@mindbridge.vn";

            // Insert jhi_user
            jdbcTemplate.update(
                "INSERT INTO jhi_user (login, password_hash, first_name, last_name, email, activated, lang_key, created_by, created_date) " +
                "VALUES (?, '$2a$10$mE.q65773.49qKy.qN/Z..X4494xI.Xn2a7o7w.nF28pZ81V7j/uG', 'Dr.', ?, ?, 1, 'vi', 'system', NOW())",
                login, "Doctor #" + i, email
            );

            Long userId = jdbcTemplate.queryForObject("SELECT id FROM jhi_user WHERE login = ?", Long.class, login);

            // Insert doctor_profile
            jdbcTemplate.update(
                "INSERT INTO doctor_profile (full_name, title, bio, experience_years, consultation_fee, phone, gender, is_active, user_id) " +
                "VALUES (?, 'Bác sĩ Chuyên khoa II', 'Bác sĩ giàu kinh nghiệm điều trị tâm lý', 10, 500000.00, '0988888888', 'MALE', 1, ?)",
                "Bác sĩ " + login, userId
            );

            Long doctorId = jdbcTemplate.queryForObject("SELECT id FROM doctor_profile WHERE user_id = ?", Long.class, userId);

            // Insert relation specialization
            if (specId != null) {
                jdbcTemplate.update(
                    "INSERT INTO rel_doctor_profile__specializations (doctor_profile_id, specializations_id) VALUES (?, ?)",
                    doctorId, specId
                );
            }
        }

        long duration = System.currentTimeMillis() - start;
        long totalDoctors = doctorProfileRepository.count();

        LOG.info("🌱 [CHAOS SEED] SEED THÀNH CÔNG {} BÁC SĨ TRONG {} ms! Tổng số Bác sĩ hiện có: {}", count, duration, totalDoctors);

        return ResponseEntity.ok(
            Map.of(
                "status", "SUCCESS",
                "seededCount", count,
                "totalDoctorsInDb", totalDoctors,
                "durationMs", duration,
                "message", "Đã tạo thành công " + count + " bác sĩ! Hãy thử gọi /doctors-slow để quan sát N+1 bùng nổ!"
            )
        );
    }

    /**
     * ❌ ENDPOINT GÂY LỖI N+1 QUERIES:
     * 1. Hibernate bắn 1 câu SQL lấy toàn bộ DoctorProfile (1 Query).
     * 2. Với mỗi DoctorProfile, khi duyệt gọi doctor.getUser().getLogin() và doctor.getSpecializationses(),
     *    Hibernate tiếp tục âm thầm bắn thêm N câu SQL lấy User và N câu SQL lấy Specialization!
     *    Tổng cộng: 1 + 2N câu SQL (Ví dụ 50 Bác sĩ -> 101 câu SQL!).
     */
    @GetMapping("/doctors-slow")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getDoctorsWithNPlusOneBug() {
        LOG.warn("🚨 [N+1 CHAOS] BẮT ĐẦU TRUY VẤN CÓ LỖI N+1 (findAll không fetch join)...");
        long start = System.currentTimeMillis();

        // Query #1: SELECT * FROM doctor_profile
        List<DoctorProfile> doctors = doctorProfileRepository.findAll();

        List<Map<String, Object>> results = new ArrayList<>();
        for (DoctorProfile doctor : doctors) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", doctor.getId());
            map.put("fullName", doctor.getFullName());

            // ⚠️ CỐ TÌNH TRUY CẬP RELATION LAZY ĐỂ KÍCH NỔ N QUERIES CHO USER
            if (doctor.getUser() != null) {
                map.put("userLogin", doctor.getUser().getLogin());
                map.put("userEmail", doctor.getUser().getEmail());
            }

            // ⚠️ CỐ TÌNH TRUY CẬP RELATION LAZY ĐỂ KÍCH NỔ THÊM N QUERIES CHO SPECIALIZATIONS
            List<String> specNames = doctor
                .getSpecializationses()
                .stream()
                .map(Specialization::getName)
                .toList();
            map.put("specializations", specNames);

            results.add(map);
        }

        long duration = System.currentTimeMillis() - start;
        LOG.warn("🚨 [N+1 CHAOS] HOÀN TẤT TRUY VẤN N+1 CHO {} BÁC SĨ MẤT {} ms. Hãy kiểm tra console log để đếm số lượng SQL đã bắn!", doctors.size(), duration);

        return ResponseEntity.ok(
            Map.of(
                "mode", "N+1 BUG (SLOW - 1 + 2N Queries)",
                "totalDoctors", doctors.size(),
                "executionTimeMs", duration,
                "data", results,
                "note", "Kiểm tra log Hibernate SQL trong Terminal/Console để thấy hàng chục câu SELECT con!"
            )
        );
    }

    /**
     * 🛡️ GIẢI PHÁP 1: SỬ DỤNG JOIN FETCH (JPQL / HQL)
     * Hibernate chỉ bắn duy nhất 1 câu SQL JOIN lấy tất cả dữ liệu một lần (Eager Fetching trong JPQL).
     */
    @GetMapping("/doctors-fetch-join")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getDoctorsWithFetchJoin() {
        LOG.info("🛡️ [OPTIMIZED] BẮT ĐẦU TRUY VẤN VỚI JOIN FETCH (1 câu SQL duy nhất)...");
        long start = System.currentTimeMillis();

        // 1 Query: SELECT dp FROM DoctorProfile dp LEFT JOIN FETCH dp.user
        List<DoctorProfile> doctors = doctorProfileRepository.findAllWithToOneRelationships();

        List<Map<String, Object>> results = new ArrayList<>();
        for (DoctorProfile doctor : doctors) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", doctor.getId());
            map.put("fullName", doctor.getFullName());
            if (doctor.getUser() != null) {
                map.put("userLogin", doctor.getUser().getLogin());
                map.put("userEmail", doctor.getUser().getEmail());
            }
            results.add(map);
        }

        long duration = System.currentTimeMillis() - start;
        LOG.info("🛡️ [OPTIMIZED] HOÀN TẤT TRUY VẤN JOIN FETCH CHO {} BÁC SĨ MẤT {} ms (1 câu SQL duy nhất)!", doctors.size(), duration);

        return ResponseEntity.ok(
            Map.of(
                "mode", "OPTIMIZED (JOIN FETCH - 1 Query)",
                "totalDoctors", doctors.size(),
                "executionTimeMs", duration,
                "data", results,
                "note", "Chỉ có đúng 1 câu lệnh SELECT ... LEFT OUTER JOIN ... được bắn xuống MySQL!"
            )
        );
    }

    /**
     * 🛡️ GIẢI PHÁP 2: SỬ DỤNG @EntityGraph
     * Hibernate tự động sinh LEFT OUTER JOIN cho các thuộc tính được chỉ định trong attributePaths.
     */
    @GetMapping("/doctors-entity-graph")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getDoctorsWithEntityGraph() {
        LOG.info("🛡️ [OPTIMIZED] BẮT ĐẦU TRUY VẤN VỚI @EntityGraph (1 câu SQL duy nhất)...");
        long start = System.currentTimeMillis();

        // 1 Query với @EntityGraph(attributePaths = {"user"})
        List<DoctorProfile> doctors = doctorProfileRepository.findAllWithEntityGraph();

        List<Map<String, Object>> results = new ArrayList<>();
        for (DoctorProfile doctor : doctors) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", doctor.getId());
            map.put("fullName", doctor.getFullName());
            if (doctor.getUser() != null) {
                map.put("userLogin", doctor.getUser().getLogin());
                map.put("userEmail", doctor.getUser().getEmail());
            }
            results.add(map);
        }

        long duration = System.currentTimeMillis() - start;
        LOG.info("🛡️ [OPTIMIZED] HOÀN TẤT TRUY VẤN @EntityGraph CHO {} BÁC SĨ MẤT {} ms!", doctors.size(), duration);

        return ResponseEntity.ok(
            Map.of(
                "mode", "OPTIMIZED (@EntityGraph - 1 Query)",
                "totalDoctors", doctors.size(),
                "executionTimeMs", duration,
                "data", results,
                "note", "Spring Data JPA tự động add JOIN FETCH cho các EntityGraph paths!"
            )
        );
    }

    /**
     * 🛡️ GIẢI PHÁP 3: JHIPSTER BAG RELATIONSHIPS (Trị MultipleBagFetchException)
     * Khi thực thể có cả ToOne (User) và ToMany/ManyToMany (Specializations),
     * giải pháp chuẩn là 2 câu SQL tối ưu: 1 cho ToOne + 1 cho Bag In-List.
     */
    @GetMapping("/doctors-with-specializations")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getDoctorsWithBagRelationships() {
        LOG.info("🛡️ [OPTIMIZED] BẮT ĐẦU TRUY VẤN JHIPSTER BAG RELATIONSHIPS (2 câu SQL tối ưu cho cả User & Specializations)...");
        long start = System.currentTimeMillis();

        // 2 Queries tối ưu:
        // 1. SELECT dp FROM DoctorProfile dp LEFT JOIN FETCH dp.user
        // 2. SELECT dp FROM DoctorProfile dp LEFT JOIN FETCH dp.specializationses WHERE dp IN (:doctorProfiles)
        List<DoctorProfile> doctors = doctorProfileRepository.findAllWithEagerRelationships();

        List<Map<String, Object>> results = new ArrayList<>();
        for (DoctorProfile doctor : doctors) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", doctor.getId());
            map.put("fullName", doctor.getFullName());
            if (doctor.getUser() != null) {
                map.put("userLogin", doctor.getUser().getLogin());
                map.put("userEmail", doctor.getUser().getEmail());
            }
            List<String> specNames = doctor
                .getSpecializationses()
                .stream()
                .map(Specialization::getName)
                .toList();
            map.put("specializations", specNames);
            results.add(map);
        }

        long duration = System.currentTimeMillis() - start;
        LOG.info("🛡️ [OPTIMIZED] HOÀN TẤT TRUY VẤN BAG RELATIONSHIPS CHO {} BÁC SĨ MẤT {} ms!", doctors.size(), duration);

        return ResponseEntity.ok(
            Map.of(
                "mode", "OPTIMIZED (JHipster Bag Relationships - 2 Queries)",
                "totalDoctors", doctors.size(),
                "executionTimeMs", duration,
                "data", results,
                "note", "Khắc phục triệt để MultipleBagFetchException và N+1!"
            )
        );
    }
}
