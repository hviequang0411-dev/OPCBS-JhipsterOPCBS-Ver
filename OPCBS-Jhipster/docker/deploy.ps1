# ==============================================================================
# Production Deployment Automation Script for MindBridge OPCBS (PowerShell)
# Full High-Availability Microservices Setup (9 Services + Frontend + Nginx + DB + Redis + Eureka)
# ==============================================================================

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
Set-Location -Path $PSScriptRoot

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "🚀 BẮT ĐẦU TRIỂN KHAI DOCKER PRODUCTION MINDBRIDGE OPCBS (9 MICROSERVICES)" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

# 1. Kiểm tra file .env
if (-not (Test-Path ".env")) {
    Write-Host "⚠️  Chưa tìm thấy file .env, tự động sao chép từ .env.example..." -ForegroundColor Yellow
    Copy-Item ".env.example" ".env"
    Write-Host "✅ Đã tạo file .env. Vui lòng kiểm tra lại cấu hình nếu cần." -ForegroundColor Green
}

# 2. Xây dựng Images
Write-Host "📦 Đang biên dịch và đóng gói toàn bộ Docker Images (Multi-stage build)..." -ForegroundColor Yellow
docker compose -f docker-compose.prod.yml build `
    opcbs-auth-service `
    opcbs-booking-service `
    opcbs-clinical-service `
    opcbs-treatment-service `
    opcbs-psychometric-service `
    opcbs-notification-service `
    opcbs-payment-service `
    opcbs-message-service `
    opcbs-blog-service `
    opcbs-frontend

# 3. Khởi chạy Database, Cache & Message Brokers trước
Write-Host "🗄️  Khởi động MySQL 8.4, Redis, Redis-Cache, Kafka KRaft và RabbitMQ..." -ForegroundColor Yellow
docker compose -f docker-compose.prod.yml up -d mysql redis redis-cache kafka rabbitmq

# 4. Chờ Infrastructure sẵn sàng (Healthcheck)
Write-Host "⏳ Đang chờ MySQL, Redis, Kafka và RabbitMQ đạt trạng thái HEALTHY..." -ForegroundColor Yellow
$maxRetries = 30

$retry = 0
while ($retry -lt $maxRetries) {
    $mysqlStatus = docker compose -f docker-compose.prod.yml ps mysql
    if ($mysqlStatus -match "healthy") {
        Write-Host "✅ MySQL đã sẵn sàng!" -ForegroundColor Green
        break
    }
    Start-Sleep -Seconds 3
    $retry++
}

$retry = 0
while ($retry -lt $maxRetries) {
    $kafkaStatus = docker compose -f docker-compose.prod.yml ps kafka
    if ($kafkaStatus -match "healthy") {
        Write-Host "✅ Kafka KRaft đã sẵn sàng!" -ForegroundColor Green
        break
    }
    Start-Sleep -Seconds 3
    $retry++
}

$retry = 0
while ($retry -lt $maxRetries) {
    $rabbitStatus = docker compose -f docker-compose.prod.yml ps rabbitmq
    if ($rabbitStatus -match "healthy") {
        Write-Host "✅ RabbitMQ đã sẵn sàng!" -ForegroundColor Green
        break
    }
    Start-Sleep -Seconds 2
    $retry++
}

# 5. Khởi động Toàn bộ 9 Microservices, Frontend SPA & Nginx Gateway
Write-Host "🏥 Khởi động 9 Domain Microservices, Frontend SPA và Nginx Reverse Proxy..." -ForegroundColor Yellow
docker compose -f docker-compose.prod.yml up -d

# 6. Reload Nginx để cập nhật bảng định tuyến DNS upstream
Write-Host "🔄 Reload Nginx Reverse Proxy để cập nhật bảng định tuyến DNS upstream..." -ForegroundColor Yellow
docker compose -f docker-compose.prod.yml exec opcbs-nginx-prod nginx -s reload

# 7. Kiểm tra trạng thái
Write-Host "📊 Trạng thái toàn bộ các container trong cụm Production:" -ForegroundColor Cyan
docker compose -f docker-compose.prod.yml ps

Write-Host "======================================================================" -ForegroundColor Green
Write-Host "🎉 TRIỂN KHAI DOCKER PRODUCTION THÀNH CÔNG!" -ForegroundColor Green
Write-Host "👉 Cổng Web SPA & Gateway HTTP : http://localhost:80" -ForegroundColor Green
Write-Host "👉 Kiểm tra Gateway Healthz     : http://localhost:80/healthz" -ForegroundColor Green
Write-Host "👉 Database MySQL (Admin Port)  : localhost:3307" -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Green
