package com.ocbs.domain.enumeration;

/**
 * Trạng thái bài viết Blog (Tham chiếu chuẩn C# gốc OPCBS).
 */
public enum BlogStatus {
    DRAFT, // Bản nháp của bác sĩ
    SUBMITTED, // Đã gửi chờ CustomerSupport / Admin kiểm duyệt
    APPROVED, // Đã phê duyệt nội dung
    REJECTED, // Bị từ chối duyệt (kèm lý do)
    PUBLISHED, // Đã xuất bản công khai
    ARCHIVED, // Đã lưu trữ / ẩn khỏi trang chủ
}
