package com.ocbs.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SlotAnalyticsTracker {

    private static final Logger LOG = LoggerFactory.getLogger(SlotAnalyticsTracker.class);

    // CỐ TÌNH GÂY BUG RACE CONDITION: Biến nguyên thủy dùng chung trong Spring Singleton Bean
    private int totalViewCount = 0;

    // CỐ TÌNH GÂY THREAD STARVATION: Cờ trạng thái dùng cho vòng lặp bận
    private volatile boolean highPriorityTaskRunning = false;

    /**
     * Tăng số lượng lượt xem slot.
     * VÙNG NGUY HIỂM (CRITICAL SECTION): Đọc -> Sửa -> Ghi không được đồng bộ!
     */
    public void recordSlotView() {
        // totalViewCount++ thực chất gồm 3 bước Bytecode:
        // 1. GETFIELD (Đọc giá trị từ RAM vào CPU Register)
        // 2. IADD (Tăng giá trị lên 1 trong ALU)
        // 3. PUTFIELD (Ghi ngược lại vào RAM)
        totalViewCount++;
    }

    public int getTotalViewCount() {
        return totalViewCount;
    }

    public void resetCount() {
        this.totalViewCount = 0;
    }

    /**
     * Mô phỏng Thread Starvation bằng Busy-Waiting (Spin-Lock tự chế)
     */
    public void executeLowPriorityTask() {
        long start = System.currentTimeMillis();
        // CỐ TÌNH GÂY BUG: Vòng lặp while ngốn 100% Core CPU, không nhường luồng (Thread Starvation)
        while (highPriorityTaskRunning) {
            // Không hề sleep hoặc yield, chiếm trọn CPU slot
            if (System.currentTimeMillis() - start > 5000) {
                LOG.warn("Low priority task bị Starvation quá 5 giây!");
                break;
            }
        }
    }

    public void setHighPriorityTaskRunning(boolean running) {
        this.highPriorityTaskRunning = running;
    }
}
