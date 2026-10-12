package com.ocbs.service;

import com.ocbs.domain.AvailabilitySlot;
import com.ocbs.repository.AvailabilitySlotRepository;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service xử lý nghiệp vụ hoán đổi ca khám giữa 2 Slot.
 * Phục vụ Lab Chaos Engineering (Bài 04): Mô phỏng Deadlock 2 Thread 2 Lock và giải pháp khắc phục.
 */
@Service
public class AppointmentSlotSwapService {

    private static final Logger LOG = LoggerFactory.getLogger(AppointmentSlotSwapService.class);

    private final AvailabilitySlotRepository slotRepository;

    // Cache các lock object tương ứng với từng Slot ID
    private final ConcurrentHashMap<Long, Object> slotLocks = new ConcurrentHashMap<>();

    public AppointmentSlotSwapService(AvailabilitySlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    private Object getLockForSlot(Long slotId) {
        return slotLocks.computeIfAbsent(slotId, k -> new Object());
    }

    /**
     * ❌ PHƯƠNG THỨC CHỨA LỖI DEADLOCK (2 Thread 2 Lock):
     * Khóa tài nguyên theo thứ tự tham số đầu vào (sourceSlot -> targetSlot).
     * Khi 2 Thread thực hiện hoán đổi chéo (1 -> 2 và 2 -> 1) cùng lúc:
     * - Thread 1 giữ Lock(1), chờ Lock(2)
     * - Thread 2 giữ Lock(2), chờ Lock(1)
     * -> Gây DEADLOCK vĩnh viễn (Hold & Wait + Circular Wait)!
     */
    @Transactional
    public boolean swapSlotsWithDeadlock(Long sourceSlotId, Long targetSlotId) {
        if (sourceSlotId.equals(targetSlotId)) {
            return false;
        }

        Object sourceLock = getLockForSlot(sourceSlotId);
        Object targetLock = getLockForSlot(targetSlotId);

        LOG.info("🔒 [Thread {}] Đang cố gắng lấy Lock của Slot nguồn: {}", Thread.currentThread().getName(), sourceSlotId);

        synchronized (sourceLock) {
            LOG.info("✅ [Thread {}] ĐÃ CHIẾM Lock Slot nguồn: {}. Đang xử lý và chuẩn bị lấy Lock Slot đích: {}", 
                Thread.currentThread().getName(), sourceSlotId, targetSlotId);

            try {
                // Giả lập xử lý nghiệp vụ / I/O để tạo Timing Window cho Thread 2 kịp lấy Lock thứ 2
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.error("Thread bị gián đoạn", e);
            }

            LOG.warn("⚠️ [Thread {}] Đang CHỜ Lock Slot đích: {} (Sẽ bị Deadlock nếu luồng khác đang giữ!)...", 
                Thread.currentThread().getName(), targetSlotId);

            synchronized (targetLock) {
                LOG.info("🎉 [Thread {}] Đã chiếm trọn cả 2 Lock: Slot {} và Slot {}. Tiến hành hoán đổi!", 
                    Thread.currentThread().getName(), sourceSlotId, targetSlotId);
                
                return doSwapLogic(sourceSlotId, targetSlotId);
            }
        }
    }

    /**
     * ✅ PHƯƠNG THỨC ĐÃ KHẮC PHỤC (SAFE):
     * Áp dụng Global Lock Ordering: Luôn luôn khóa Slot có ID nhỏ hơn trước (min), ID lớn hơn sau (max).
     * Bẻ gãy hoàn toàn điều kiện Circular Wait của Coffman!
     */
    @Transactional
    public boolean swapSlotsSafe(Long sourceSlotId, Long targetSlotId) {
        if (sourceSlotId.equals(targetSlotId)) {
            return false;
        }

        Long firstId = sourceSlotId < targetSlotId ? sourceSlotId : targetSlotId;
        Long secondId = sourceSlotId < targetSlotId ? targetSlotId : sourceSlotId;

        Object firstLock = getLockForSlot(firstId);
        Object secondLock = getLockForSlot(secondId);

        LOG.info("🛡️ [Thread {}] Khóa theo thứ tự chuẩn Global Lock Ordering: Slot {} -> Slot {}", 
            Thread.currentThread().getName(), firstId, secondId);

        synchronized (firstLock) {
            synchronized (secondLock) {
                LOG.info("✅ [Thread {}] Hoán đổi an toàn thành công giữa Slot {} và Slot {}", 
                    Thread.currentThread().getName(), sourceSlotId, targetSlotId);
                
                return doSwapLogic(sourceSlotId, targetSlotId);
            }
        }
    }

    private boolean doSwapLogic(Long sourceSlotId, Long targetSlotId) {
        AvailabilitySlot sourceSlot = slotRepository.findById(sourceSlotId).orElse(null);
        AvailabilitySlot targetSlot = slotRepository.findById(targetSlotId).orElse(null);

        if (sourceSlot == null || targetSlot == null) {
            return false;
        }

        // Hoán đổi thông tin ghi chú giữa 2 ca khám
        String tempNotes = sourceSlot.getNotes();
        sourceSlot.setNotes(targetSlot.getNotes());
        targetSlot.setNotes(tempNotes);

        slotRepository.save(sourceSlot);
        slotRepository.save(targetSlot);
        return true;
    }
}
