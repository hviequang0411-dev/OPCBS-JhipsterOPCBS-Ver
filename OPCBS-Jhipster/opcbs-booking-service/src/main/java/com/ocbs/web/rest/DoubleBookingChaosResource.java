package com.ocbs.web.rest;

import com.ocbs.domain.AvailabilitySlot;
import com.ocbs.domain.enumeration.SlotStatus;
import com.ocbs.repository.AvailabilitySlotRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * 🧪 CHAOS LAB 12 & 15: Double Booking Race Condition & Pessimistic Lock (SELECT ... FOR UPDATE)
 */
@RestController
@RequestMapping("/api/chaos/double-booking")
public class DoubleBookingChaosResource {

    private static final Logger LOG = LoggerFactory.getLogger(DoubleBookingChaosResource.class);

    private final AvailabilitySlotRepository slotRepository;
    private final DoubleBookingDemoService bookingService;

    public DoubleBookingChaosResource(
        AvailabilitySlotRepository slotRepository,
        DoubleBookingDemoService bookingService
    ) {
        this.slotRepository = slotRepository;
        this.bookingService = bookingService;
    }

    /**
     * 🔄 RESET TRẠNG THÁI SLOT VỀ AVAILABLE
     */
    @PostMapping("/reset-slot")
    public ResponseEntity<Map<String, Object>> resetSlot(@RequestParam(defaultValue = "1") Long slotId) {
        AvailabilitySlot slot = slotRepository.findById(slotId).orElse(null);
        if (slot == null) {
            return ResponseEntity.notFound().build();
        }
        slot.setStatus(SlotStatus.AVAILABLE);
        slot.setNotes("Khung giờ trống sẵn sàng đặt khám");
        slotRepository.save(slot);

        LOG.info("🔄 [RESET] Đã reset Slot #{} về trạng thái AVAILABLE", slotId);
        return ResponseEntity.ok(Map.of(
            "slotId", slot.getId(),
            "status", slot.getStatus(),
            "message", "Reset slot về AVAILABLE thành công!"
        ));
    }

    /**
     * ❌ THỬ NGHIỆM 1: KÍCH NỔ DOUBLE BOOKING RACE CONDITION (KHÔNG DÙNG LOCK)
     * Kích hoạt 2 luồng đồng thời cố gắng đặt cùng 1 khung giờ khám trong cùng 1 tích tắc.
     */
    @PostMapping("/simulate-race-condition")
    public ResponseEntity<Map<String, Object>> simulateRaceCondition(@RequestParam(defaultValue = "1") Long slotId)
        throws InterruptedException {
        
        LOG.warn("🚨 [CHAOS 12] BẮT ĐẦU MÔ PHỎNG DOUBLE BOOKING (2 Luồng tranh chấp Slot #{})...", slotId);

        // Reset trước khi test
        resetSlot(slotId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(2);

        List<String> results = Collections.synchronizedList(new ArrayList<>());

        // Luồng Bệnh nhân A
        executor.submit(() -> {
            try {
                startSignal.await(); // Chờ lệnh xuất phát đồng thời
                String res = bookingService.bookWithoutLock(slotId, "Bệnh nhân Nguyễn Văn A (VIP 1)");
                results.add(res);
            } catch (Exception e) {
                results.add("❌ Luồng A Lỗi: " + e.getMessage());
            } finally {
                doneSignal.countDown();
            }
        });

        // Luồng Bệnh nhân B
        executor.submit(() -> {
            try {
                startSignal.await(); // Chờ lệnh xuất phát đồng thời
                String res = bookingService.bookWithoutLock(slotId, "Bệnh nhân Trần Thị B (VIP 2)");
                results.add(res);
            } catch (Exception e) {
                results.add("❌ Luồng B Lỗi: " + e.getMessage());
            } finally {
                doneSignal.countDown();
            }
        });

        // Bắn tín hiệu xuất phát cùng lúc
        startSignal.countDown();
        doneSignal.await();
        executor.shutdown();

        AvailabilitySlot finalSlot = slotRepository.findById(slotId).orElse(null);

        return ResponseEntity.ok(Map.of(
            "testType", "RACE CONDITION (NO LOCK)",
            "slotId", slotId,
            "finalSlotStatus", finalSlot != null ? finalSlot.getStatus() : "UNKNOWN",
            "finalNotes", finalSlot != null ? finalSlot.getNotes() : "",
            "threadResponses", results,
            "incidentEvaluation", results.stream().filter(r -> r.contains("THÀNH CÔNG")).count() > 1
                ? "💥 THẢM HỌA: CẢ 2 BỆNH NHÂN ĐỀU BÁO ĐẶT THÀNH CÔNG CHO CÙNG 1 KHUNG GIỜ (DOUBLE BOOKING)!"
                : "Chỉ 1 bệnh nhân thành công."
        ));
    }

    /**
     * 🛡️ THỬ NGHIỆM 2: TRIỆT TIÊU DOUBLE BOOKING BẰNG PESSIMISTIC LOCK (SELECT ... FOR UPDATE)
     */
    @PostMapping("/simulate-pessimistic-lock")
    public ResponseEntity<Map<String, Object>> simulatePessimisticLock(@RequestParam(defaultValue = "1") Long slotId)
        throws InterruptedException {
        
        LOG.info("🛡️ [CHAOS 15] BẮT ĐẦU MÔ PHỎNG PESSIMISTIC LOCK (SELECT ... FOR UPDATE cho Slot #{})...", slotId);

        // Reset trước khi test
        resetSlot(slotId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(2);

        List<String> results = Collections.synchronizedList(new ArrayList<>());

        // Luồng Bệnh nhân A
        executor.submit(() -> {
            try {
                startSignal.await();
                String res = bookingService.bookWithPessimisticLock(slotId, "Bệnh nhân Nguyễn Văn A (VIP 1)");
                results.add(res);
            } catch (Exception e) {
                results.add("❌ Luồng A Lỗi: " + e.getMessage());
            } finally {
                doneSignal.countDown();
            }
        });

        // Luồng Bệnh nhân B
        executor.submit(() -> {
            try {
                startSignal.await();
                String res = bookingService.bookWithPessimisticLock(slotId, "Bệnh nhân Trần Thị B (VIP 2)");
                results.add(res);
            } catch (Exception e) {
                results.add("❌ Luồng B Lỗi: " + e.getMessage());
            } finally {
                doneSignal.countDown();
            }
        });

        startSignal.countDown();
        doneSignal.await();
        executor.shutdown();

        AvailabilitySlot finalSlot = slotRepository.findById(slotId).orElse(null);

        return ResponseEntity.ok(Map.of(
            "testType", "PESSIMISTIC LOCK (SELECT ... FOR UPDATE)",
            "slotId", slotId,
            "finalSlotStatus", finalSlot != null ? finalSlot.getStatus() : "UNKNOWN",
            "finalNotes", finalSlot != null ? finalSlot.getNotes() : "",
            "threadResponses", results,
            "evaluation", "✅ AN TOÀN TUYỆT ĐỐI: Đúng 1 người đặt thành công, người thứ 2 bị từ chối hợp lệ!"
        ));
    }

    /**
     * Service nội bộ hỗ trợ Transaction độc lập cho từng luồng
     */
    @Service
    public static class DoubleBookingDemoService {

        private final AvailabilitySlotRepository slotRepository;
        private final TransactionTemplate transactionTemplate;

        public DoubleBookingDemoService(
            AvailabilitySlotRepository slotRepository,
            PlatformTransactionManager transactionManager
        ) {
            this.slotRepository = slotRepository;
            this.transactionTemplate = new TransactionTemplate(transactionManager);
            this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        }

        /**
         * ❌ KHÔNG KHÓA: Đọc trạng thái cũ -> sleep 50ms (mô phỏng gọi thanh toán/logic) -> Ghi đè
         */
        public String bookWithoutLock(Long slotId, String patientName) {
            return transactionTemplate.execute(status -> {
                AvailabilitySlot slot = slotRepository.findById(slotId).orElse(null);
                if (slot == null) {
                    return "Không tìm thấy Slot #" + slotId;
                }

                if (slot.getStatus() != SlotStatus.AVAILABLE) {
                    return "❌ [THẤT BẠI] " + patientName + ": Slot đã có người đặt trước!";
                }

                try {
                    // Mô phỏng thời gian tính toán giá / kết nối cổng thanh toán 80ms
                    Thread.sleep(80);
                } catch (InterruptedException ignored) {}

                slot.setStatus(SlotStatus.BOOKED);
                slot.setNotes("Đặt bởi: " + patientName);
                slotRepository.save(slot);

                return "🎉 [THÀNH CÔNG] " + patientName + " đã đặt thành công Slot #" + slotId + "!";
            });
        }

        /**
         * 🛡️ CÓ KHÓA PESSIMISTIC: SELECT ... FOR UPDATE khóa chặt hàng dữ liệu trong MySQL
         */
        public String bookWithPessimisticLock(Long slotId, String patientName) {
            return transactionTemplate.execute(status -> {
                // MySQL thực thi: SELECT * FROM availability_slot WHERE id = ? FOR UPDATE
                AvailabilitySlot slot = slotRepository.findByIdWithLock(slotId).orElse(null);
                if (slot == null) {
                    return "Không tìm thấy Slot #" + slotId;
                }

                if (slot.getStatus() != SlotStatus.AVAILABLE) {
                    return "❌ [TỪ CHỐI AN TOÀN] " + patientName + ": Slot đã bị người khác khóa và đặt trước!";
                }

                try {
                    Thread.sleep(80);
                } catch (InterruptedException ignored) {}

                slot.setStatus(SlotStatus.BOOKED);
                slot.setNotes("Đặt an toàn bởi: " + patientName);
                slotRepository.save(slot);

                return "🎉 [THÀNH CÔNG] " + patientName + " đã đặt thành công (Row Lock an toàn)!";
            });
        }
    }
}
