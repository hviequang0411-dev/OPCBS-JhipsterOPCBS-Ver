package com.ocbs.demo.deadlock;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * BÀI HỌC THỰC CHIẾN 1: MÔ PHỎNG VÀ PHÂN TÍCH DEADLOCK TRONG JAVA (JVM)
 * ════════════════════════════════════════════════════════════════════════════
 * Chu trình 7 bước:
 * 1. Understand -> 2. Code -> 3. Run -> 4. Debug -> 5. Break it -> 6. Fix it -> 7. Explain it
 */
public class JavaDeadlockSimulation {

    // Hai tài nguyên cần khóa (Shared Locks)
    private static final Object LOCK_A = new Object(); // Ví dụ: Khóa Bệnh Nhân (Patient Lock)
    private static final Object LOCK_B = new Object(); // Ví dụ: Khóa Gói Trị Liệu (Package Lock)

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("🚀 [BẮT ĐẦU] Khởi chạy mô phỏng Deadlock (Circular Wait)...");
        System.out.println("=================================================================\n");

        // -------------------------------------------------------------
        // Thread 1: Đặt lịch hẹn -> Cần LOCK_A (Patient) rồi LOCK_B (Package)
        // -------------------------------------------------------------
        Thread thread1 = new Thread(() -> {
            String threadName = Thread.currentThread().getName();
            synchronized (LOCK_A) {
                System.out.println("🔒 [" + threadName + "] ĐÃ GIỮ LOCK_A. Đang xử lý hồ sơ bệnh nhân...");
                try {
                    // Giả lập xử lý nghiệp vụ 100ms (tạo điều kiện cho Thread 2 kịp lấy LOCK_B)
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                System.out.println("⏳ [" + threadName + "] Đang chờ giành lấy LOCK_B...");
                synchronized (LOCK_B) {
                    System.out.println("✅ [" + threadName + "] ĐÃ LẤY ĐƯỢC CẢ 2 LOCK! Xử lý thành công.");
                }
            }
        }, "Thread-DoctorBooking");

        // -------------------------------------------------------------
        // Thread 2: Trừ số buổi -> Cần LOCK_B (Package) rồi LOCK_A (Patient) -> ĐẢO NGƯỢC THỨ TỰ!
        // -------------------------------------------------------------
        Thread thread2 = new Thread(() -> {
            String threadName = Thread.currentThread().getName();
            synchronized (LOCK_B) {
                System.out.println("🔒 [" + threadName + "] ĐÃ GIỮ LOCK_B. Đang xử lý trừ buổi gói trị liệu...");
                try {
                    // Giả lập xử lý nghiệp vụ 100ms
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                System.out.println("⏳ [" + threadName + "] Đang chờ giành lấy LOCK_A...");
                synchronized (LOCK_A) {
                    System.out.println("✅ [" + threadName + "] ĐÃ LẤY ĐƯỢC CẢ 2 LOCK! Xử lý thành công.");
                }
            }
        }, "Thread-SessionDeduct");

        // Khởi động 2 threads song song
        thread1.start();
        thread2.start();

        // -------------------------------------------------------------
        // Watchdog Thread: Dùng ThreadMXBean để tự động phát hiện Deadlock
        // -------------------------------------------------------------
        new Thread(() -> {
            try {
                Thread.sleep(800); // Chờ 800ms để Deadlock xảy ra hoàn toàn
                System.out.println("\n-----------------------------------------------------------------");
                System.out.println("🔍 [JVM WATCHDOG] Đang quét toàn bộ Thread Dump tìm Deadlock...");
                System.out.println("-----------------------------------------------------------------");

                ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
                long[] deadlockedThreadIds = threadMXBean.findDeadlockedThreads();

                if (deadlockedThreadIds != null && deadlockedThreadIds.length > 0) {
                    System.err.println("💥 [DEADLOCK DETECTED] Phát hiện " + deadlockedThreadIds.length + " thread bị tắc nghẽn (Deadlocked)!");
                    ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(deadlockedThreadIds);
                    for (ThreadInfo info : threadInfos) {
                        System.err.println("\n📌 Chi tiết Thread bị treo:");
                        System.err.println("   - Thread Name: " + info.getThreadName() + " (ID: " + info.getThreadId() + ")");
                        System.err.println("   - Trạng thái: " + info.getThreadState() + " (BLOCKED)");
                        System.err.println("   - Đang bị chặn bởi Lock: " + info.getLockName());
                        System.err.println("   - Đang bị giữ bởi Thread: " + info.getLockOwnerName() + " (ID: " + info.getLockOwnerId() + ")");
                    }
                    System.out.println("\n💡 [GIẢI THÍCH]:");
                    System.out.println("   Thread-DoctorBooking giữ LOCK_A và CHỜ LOCK_B.");
                    System.out.println("   Thread-SessionDeduct giữ LOCK_B và CHỜ LOCK_A.");
                    System.out.println("   -> Tạo thành vòng chờ tròn (Circular Wait), cả 2 vĩnh viễn không bao giờ nhả lock!");
                    System.out.println("\n=================================================================");
                    System.out.println("🎯 [KẾT LUẬN]: Đã chứng minh và bắt trọn Deadlock thành công!");
                    System.out.println("=================================================================");
                } else {
                    System.out.println("✅ Không phát hiện Deadlock.");
                }
                Thread.sleep(500);
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Deadlock-Detector").start();
    }
}
