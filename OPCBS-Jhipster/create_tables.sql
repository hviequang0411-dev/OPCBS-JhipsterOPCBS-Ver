CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_login VARCHAR(100) NOT NULL,
    user_id BIGINT,
    title VARCHAR(255) NOT NULL,
    message LONGTEXT NOT NULL,
    notification_type VARCHAR(50),
    related_entity_id BIGINT,
    related_entity_type VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE,
    read_at DATETIME,
    action_url VARCHAR(500),
    channel VARCHAR(20) DEFAULT 'IN_APP',
    delivery_status VARCHAR(30) DEFAULT 'DELIVERED',
    retry_count INT DEFAULT 0,
    created_by VARCHAR(50) DEFAULT 'system',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(50) DEFAULT 'system',
    last_modified_date DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS service_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description LONGTEXT,
    duration_days INT NOT NULL,
    price DECIMAL(21,2) NOT NULL,
    max_patient_capacity INT,
    max_daily_slots_capacity INT,
    is_active BOOLEAN DEFAULT TRUE,
    is_featured BOOLEAN DEFAULT FALSE,
    display_order INT DEFAULT 0,
    created_by VARCHAR(50) DEFAULT 'system',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(50) DEFAULT 'system',
    last_modified_date DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS doctor_subscription (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id BIGINT,
    doctor_login VARCHAR(100) NOT NULL,
    service_package_id BIGINT,
    service_package_name VARCHAR(150),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_PAYMENT',
    start_date DATETIME,
    expiration_date DATETIME,
    price DECIMAL(21,2),
    transaction_code VARCHAR(100),
    created_by VARCHAR(50) DEFAULT 'system',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(50) DEFAULT 'system',
    last_modified_date DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_code VARCHAR(100) NOT NULL UNIQUE,
    subscription_id BIGINT,
    doctor_id BIGINT,
    doctor_login VARCHAR(100),
    amount DECIMAL(21,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL DEFAULT 'VNPAY',
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    status VARCHAR(50) DEFAULT 'PENDING',
    payment_url VARCHAR(1000),
    vnp_transaction_no VARCHAR(100),
    response_code VARCHAR(50),
    vnp_response_code VARCHAR(50),
    response_message VARCHAR(500),
    notes VARCHAR(500),
    paid_at DATETIME,
    created_by VARCHAR(50) DEFAULT 'system',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(50) DEFAULT 'system',
    last_modified_date DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    patient_name VARCHAR(255),
    doctor_id BIGINT NOT NULL,
    doctor_name VARCHAR(255),
    appointment_id BIGINT,
    treatment_package_id BIGINT,
    status VARCHAR(50) DEFAULT 'OPEN',
    last_message VARCHAR(1000),
    last_message_time DATETIME,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    sender_name VARCHAR(255),
    sender_role VARCHAR(50) NOT NULL,
    content LONGTEXT,
    attachment_url VARCHAR(500),
    is_read BOOLEAN DEFAULT FALSE,
    read_at DATETIME,
    client_message_id VARCHAR(100),
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE,
    UNIQUE KEY ux_chat_msg_idempotency (conversation_id, sender_id, client_message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================================================================
-- BẢNG PHÂN HỆ ĐIỀU TRỊ (TREATMENT & HOLD LIFECYCLE)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS treatment_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_name VARCHAR(255) NOT NULL,
    description LONGTEXT,
    total_sessions INT NOT NULL,
    completed_sessions INT DEFAULT 0,
    price DECIMAL(21,2),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    start_date DATETIME,
    expiration_date DATETIME,
    patient_id BIGINT,
    doctor_id BIGINT,
    total_hold_days INT DEFAULT 0,
    hold_start_date DATETIME,
    hold_end_date DATETIME,
    hold_reason LONGTEXT,
    hold_status VARCHAR(50) DEFAULT 'NONE',
    hold_requested_at DATETIME,
    hold_approved_at DATETIME,
    hold_rejected_reason LONGTEXT,
    original_expiration_date DATETIME,
    hold_request_days INT DEFAULT 0,
    created_by VARCHAR(50) DEFAULT 'system',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(50) DEFAULT 'system',
    last_modified_date DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS treatment_goal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    treatment_package_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description LONGTEXT,
    is_completed BOOLEAN DEFAULT FALSE,
    target_date DATETIME,
    completed_at DATETIME,
    created_by VARCHAR(50) DEFAULT 'system',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (treatment_package_id) REFERENCES treatment_package(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS treatment_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    treatment_package_id BIGINT NOT NULL,
    session_number INT NOT NULL,
    session_date DATETIME,
    clinical_notes LONGTEXT,
    homework_assignment LONGTEXT,
    mood_score_before INT,
    mood_score_after INT,
    status VARCHAR(50) DEFAULT 'COMPLETED',
    created_by VARCHAR(50) DEFAULT 'system',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (treatment_package_id) REFERENCES treatment_package(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================================================================
-- BẢNG PHÂN HỆ BÀI VIẾT Y KHOA & BÌNH LUẬN (BLOG & COMMENTS)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS blog_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    thumbnail_url VARCHAR(1024),
    excerpt VARCHAR(1000),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    view_count INT NOT NULL DEFAULT 0,
    rejection_reason LONGTEXT,
    submitted_at DATETIME,
    approved_at DATETIME,
    approved_by BIGINT,
    published_at DATETIME,
    archived_at DATETIME,
    created_by VARCHAR(50) DEFAULT 'system',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(50) DEFAULT 'system',
    last_modified_date DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS blog_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    blog_post_id BIGINT NOT NULL,
    patient_id BIGINT,
    author_name VARCHAR(100),
    author_email VARCHAR(100),
    content LONGTEXT NOT NULL,
    is_approved BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(50) DEFAULT 'system',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(50) DEFAULT 'system',
    last_modified_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (blog_post_id) REFERENCES blog_post(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================================================================
-- BẢNG TRANSACTIONAL OUTBOX & EVENT INBOX (EVENT-DRIVEN ARCHITECTURE)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS outbox_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    event_version VARCHAR(20) NOT NULL DEFAULT '1.0',
    aggregate_type VARCHAR(50) NOT NULL DEFAULT 'APPOINTMENT',
    aggregate_id BIGINT NOT NULL,
    idempotency_key VARCHAR(150) NOT NULL,
    payload LONGTEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    last_retry_at DATETIME(6),
    next_retry_at DATETIME(6),
    error_message VARCHAR(1000),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    sent_at DATETIME(6),
    INDEX idx_outbox_retry_poll (status, next_retry_at, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notification_inbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    idempotency_key VARCHAR(150) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    consumer_group VARCHAR(100) NOT NULL DEFAULT 'notification-service-group',
    processed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY ux_notif_inbox_key (consumer_group, idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS treatment_inbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    appointment_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL DEFAULT 'APPOINTMENT_COMPLETED',
    status VARCHAR(50) NOT NULL DEFAULT 'PROCESSED',
    idempotency_key VARCHAR(150) NOT NULL,
    consumer_group VARCHAR(100) NOT NULL DEFAULT 'treatment-service-group',
    processed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY ux_treatment_inbox_appt_event (appointment_id, event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================================================================
-- SEED DATA MẪU: SERVICE PACKAGES & BLOG POSTS
-- ==============================================================================
INSERT INTO service_package (id, name, description, duration_days, price, max_patient_capacity, max_daily_slots_capacity, is_active, is_featured, display_order)
SELECT 1, 'Standard Specialist Tier', 'Gói dịch vụ khởi đầu dành cho Bác sĩ & Chuyên gia tư vấn cá nhân.', 30, 499000.00, 50, 10, true, false, 1
WHERE NOT EXISTS (SELECT 1 FROM service_package WHERE id = 1);

INSERT INTO service_package (id, name, description, duration_days, price, max_patient_capacity, max_daily_slots_capacity, is_active, is_featured, display_order)
SELECT 2, 'Professional Clinic Pro', 'Gói tối ưu năng suất cao cấp với dung lượng khám không giới hạn và hỗ trợ ưu tiên.', 30, 999000.00, 200, 30, true, true, 2
WHERE NOT EXISTS (SELECT 1 FROM service_package WHERE id = 2);

INSERT INTO service_package (id, name, description, duration_days, price, max_patient_capacity, max_daily_slots_capacity, is_active, is_featured, display_order)
SELECT 3, 'VIP Enterprise Care', 'Giải pháp toàn diện đa khoa kèm báo cáo nâng cao và tính năng đặc quyền.', 90, 2499000.00, 9999, 999, true, false, 3
WHERE NOT EXISTS (SELECT 1 FROM service_package WHERE id = 3);

INSERT INTO blog_post (id, doctor_id, title, excerpt, content, status, view_count, published_at, created_by, created_date)
SELECT 1, 1, 'Nhận Diện Sớm Các Triệu Chứng Lo Âu & Trầm Cảm Theo Chuẩn DASS-21', 
       'Hướng dẫn chi tiết từ Bác sĩ chuyên khoa về cách phân biệt mức độ căng thẳng, lo âu và trầm cảm thông qua bộ thang đo lâm sàng DASS-21.',
       '<p>Rối loạn lo âu và trầm cảm là hai vấn đề sức khỏe tâm thần phổ biến nhất hiện nay. Thang đo DASS-21 (Depression, Anxiety and Stress Scale) được phát triển bởi Đại học New South Wales, Úc, là công cụ tầm soát lâm sàng tiêu chuẩn được áp dụng trên toàn thế giới...</p>',
       'PUBLISHED', 156, NOW(), 'doctor', NOW()
WHERE NOT EXISTS (SELECT 1 FROM blog_post WHERE id = 1);
