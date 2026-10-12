package com.ocbs.demo.deadlock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * BÀI HỌC THỰC CHIẾN 2: CÁCH PHÒNG CHỐNG VÀ KHẮC PHỤC DEADLOCK (FIX DEADLOCK)
 * ════════════════════════════════════════════════════════════════════════════
 * Hai giải pháp chuẩn trong lập trình Enterprise:
 * 1. Lock Ordering (Luôn khóa theo đúng một thứ tự nhất quán trên toàn bộ hệ thống).
 * 2. ReentrantLock.tryLock(timeout) (Khóa có thời gian timeout, không chờ vô tận).
 */
public class JavaDeadlockFixed {

    private static final ReentrantLock LOCK_A = new ReentrantLock();
    private static final ReentrantLock LOCK_B = new ReentrantLock();

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("🚀 [BẮT ĐẦU] Khởi chạy phiên bản ĐÃ FIX DEADLOCK...");
        System.out.println("=================================================================\n");

        // Cách 1: Áp dụng Lock Ordering (Luôn lấy LOCK_A trước rồi mới lấy LOCK_B)
        Thread thread1 = new Thread(() -> executeWithLockOrdering("Thread-DoctorBooking-Fixed"), "Thread-DoctorBooking-Fixed");
        Thread thread2 = new Thread(() -> executeWithTryLockTimeout("Thread-SessionDeduct-Fixed"), "Thread-SessionDeduct-Fixed");

        thread1.start();
        thread2.start();
    }

    /**
     * GIẢI PHÁP 1: Lock Ordering (Thứ tự khóa nhất quán)
     * Tất cả các luồng trong hệ thống BẮT BUỘC phải tuân thủ thứ tự: Lock A -> Lock B.
     * Vòng lặp Circular Wait bị phá vỡ 100%!
     */
    private static void executeWithLockOrdering(String threadName) {
        try {
            LOCK_A.lock();
            System.out.println("🔒 [" + threadName + "] (Giải Pháp 1 - Lock Ordering) Đã giữ LOCK_A.");
            Thread.sleep(100);

            LOCK_B.lock();
            try {
                System.out.println("✅ [" + threadName + "] (Giải Pháp 1 - Lock Ordering) Đã giữ LOCK_B. Hoàn thành trọn vẹn nghiệp vụ!");
            } finally {
                LOCK_B.unlock();
                System.out.println("🔓 [" + threadName + "] Đã nhả LOCK_B.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            LOCK_A.unlock();
            System.out.println("🔓 [" + threadName + "] Đã nhả LOCK_A.\n");
        }
    }

    /**
     * GIẢI PHÁP 2: Timed Locking với tryLock(timeout)
     * Thay vì synchronized (chờ vô tận đến chết), dùng tryLock với thời gian chờ tối đa (ví dụ 300ms).
     * Nếu không lấy được lock thứ 2, chủ động nhả lock thứ 1 và retry hoặc báo lỗi lịch sự!
     */
    private static void executeWithTryLockTimeout(String threadName) {
        try {
            // Thử lấy LOCK_B trong 200ms
            if (LOCK_B.tryLock(200, TimeUnit.MILLISECONDS)) {
                try {
                    System.out.println("🔒 [" + threadName + "] (Giải Pháp 2 - tryLock) Đã giữ LOCK_B.");
                    Thread.sleep(100);

                    // Thử lấy LOCK_A trong 200ms
                    if (LOCK_A.tryLock(200, TimeUnit.MILLISECONDS)) {
                        try {
                            System.out.println("✅ [" + threadName + "] (Giải Pháp 2 - tryLock) Đã lấy được cả 2 Lock! Hoàn tất.");
                        } finally {
                            LOCK_A.unlock();
                            System.out.println("🔓 [" + threadName + "] Đã nhả LOCK_A.");
                        }
                    } else {
                        System.err.println("⚠️ [" + threadName + "] (Giải Pháp 2 - tryLock) TIMEOUT không lấy được LOCK_A! Tự động nhả LOCK_B để tránh tắc nghẽn.");
                    }
                } finally {
                    LOCK_B.unlock();
                    System.out.println("🔓 [" + threadName + "] Đã nhả LOCK_B.\n");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
