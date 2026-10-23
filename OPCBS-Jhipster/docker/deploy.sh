#!/bin/bash
# ==============================================================================
# Production Deployment Automation Script for MindBridge OPCBS (Linux/Bash)
# Full High-Availability Microservices Setup (9 Services + Frontend + Nginx + DB + Redis + Eureka)
# ==============================================================================
set -euo pipefail

cd "$(dirname "$0")"

echo "======================================================================"
echo "🚀 BẮT ĐẦU TRIỂN KHAI DOCKER PRODUCTION MINDBRIDGE OPCBS (9 MICROSERVICES)"
echo "======================================================================"

# 1. Kiểm tra file .env
if [ ! -f .env ]; then
    echo "⚠️  Chưa tìm thấy file .env, tự động sao chép từ .env.example..."
    cp .env.example .env
    echo "✅ Đã tạo file .env. Vui lòng kiểm tra lại mật khẩu và JWT Secret nếu cần."
fi

# 2. Xây dựng Images
echo "📦 Đang biên dịch và đóng gói toàn bộ Docker Images (Multi-stage build)..."
docker compose -f docker-compose.prod.yml build \
    opcbs-auth-service \
    opcbs-booking-service \
    opcbs-clinical-service \
    opcbs-treatment-service \
    opcbs-psychometric-service \
    opcbs-notification-service \
    opcbs-payment-service \
    opcbs-message-service \
    opcbs-blog-service \
    opcbs-frontend

# 3. Khởi chạy Database, Cache & Message Brokers trước
echo "🗄️  Khởi động MySQL 8.4, Redis, Redis-Cache, Kafka KRaft và RabbitMQ..."
docker compose -f docker-compose.prod.yml up -d mysql redis redis-cache kafka rabbitmq

# 4. Chờ Infrastructure sẵn sàng (Healthcheck)
echo "⏳ Đang chờ MySQL, Redis, Kafka và RabbitMQ đạt trạng thái HEALTHY..."
until docker compose -f docker-compose.prod.yml ps mysql | grep -q "(healthy)"; do
    echo "   ...đang chờ MySQL hoàn tất khởi tạo..."
    sleep 3
done
echo "✅ MySQL đã sẵn sàng!"

until docker compose -f docker-compose.prod.yml ps redis | grep -q "(healthy)"; do
    echo "   ...đang chờ Redis hoàn tất khởi tạo..."
    sleep 2
done
echo "✅ Redis đã sẵn sàng!"

until docker compose -f docker-compose.prod.yml ps redis-cache | grep -q "(healthy)"; do
    echo "   ...đang chờ Redis Cache hoàn tất khởi tạo..."
    sleep 2
done
echo "✅ Redis Cache đã sẵn sàng!"

until docker compose -f docker-compose.prod.yml ps kafka | grep -q "(healthy)"; do
    echo "   ...đang chờ Kafka KRaft hoàn tất khởi tạo..."
    sleep 3
done
echo "✅ Kafka KRaft đã sẵn sàng!"

until docker compose -f docker-compose.prod.yml ps rabbitmq | grep -q "(healthy)"; do
    echo "   ...đang chờ RabbitMQ hoàn tất khởi tạo..."
    sleep 2
done
echo "✅ RabbitMQ đã sẵn sàng!"

# 5. Khởi động Toàn bộ 9 Microservices, Frontend SPA & Nginx Gateway
echo "🏥 Khởi động 9 Domain Microservices, Frontend SPA và Nginx Reverse Proxy..."
docker compose -f docker-compose.prod.yml up -d

# 6. Reload Nginx để cập nhật bảng định tuyến DNS upstream
echo "🔄 Reload Nginx Reverse Proxy để cập nhật bảng định tuyến DNS upstream..."
docker compose -f docker-compose.prod.yml exec opcbs-nginx-prod nginx -s reload || true

# 7. Kiểm tra trạng thái toàn cụm
echo "📊 Trạng thái toàn bộ các container trong cụm Production:"
docker compose -f docker-compose.prod.yml ps

echo "======================================================================"
echo "🎉 TRIỂN KHAI DOCKER PRODUCTION THÀNH CÔNG!"
echo "👉 Cổng Web SPA & Gateway HTTP : http://localhost:80"
echo "👉 Kiểm tra Gateway Healthz     : http://localhost:80/healthz"
echo "👉 Database MySQL (Admin Port)  : localhost:3307"
echo "======================================================================"
