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
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_url VARCHAR(1000),
    vnp_transaction_no VARCHAR(100),
    vnp_response_code VARCHAR(50),
    notes VARCHAR(500),
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
    sender_login VARCHAR(100) NOT NULL,
    sender_role VARCHAR(50) NOT NULL,
    message_text LONGTEXT NOT NULL,
    attachment_url VARCHAR(500),
    is_read BOOLEAN DEFAULT FALSE,
    read_at DATETIME,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO service_package (id, name, description, duration_days, price, max_patient_capacity, max_daily_slots_capacity, is_active, is_featured, display_order)
SELECT 1, 'Standard Specialist Tier', 'Gói dịch vụ khởi đầu dành cho Bác sĩ & Chuyên gia tư vấn cá nhân.', 30, 499000.00, 50, 10, true, false, 1
WHERE NOT EXISTS (SELECT 1 FROM service_package WHERE id = 1);

INSERT INTO service_package (id, name, description, duration_days, price, max_patient_capacity, max_daily_slots_capacity, is_active, is_featured, display_order)
SELECT 2, 'Professional Clinic Pro', 'Gói tối ưu năng suất cao cấp với dung lượng khám không giới hạn và hỗ trợ ưu tiên.', 30, 999000.00, 200, 30, true, true, 2
WHERE NOT EXISTS (SELECT 1 FROM service_package WHERE id = 2);

INSERT INTO service_package (id, name, description, duration_days, price, max_patient_capacity, max_daily_slots_capacity, is_active, is_featured, display_order)
SELECT 3, 'VIP Enterprise Care', 'Giải pháp toàn diện đa khoa kèm báo cáo nâng cao và tính năng đặc quyền.', 90, 2499000.00, 9999, 999, true, false, 3
WHERE NOT EXISTS (SELECT 1 FROM service_package WHERE id = 3);
