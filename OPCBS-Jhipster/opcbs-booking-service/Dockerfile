# ==============================================================================
# STAGE 1: Build Application with Maven & Dependency Layer Caching
# ==============================================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 1. Tận dụng Docker Cache: Copy file cấu hình Maven trước để cache dependencies
COPY pom.xml checkstyle.xml sonar-project.properties ./
COPY build-plugins ./build-plugins

# Tải dependencies về layer cache (tránh tải lại khi chỉ đổi source code)
RUN mvn dependency:go-offline -B || true

# 2. Copy toàn bộ mã nguồn vào và build production artifact
COPY src ./src
RUN mvn clean package -DskipTests -Dfrontend.skip=true -Dmaven.gitcommitid.skip=true -Dmodernizer.skip=true -Dcheckstyle.skip=true -B

# ==============================================================================
# STAGE 2: Minimal & Secure Production Runtime (Non-root, Alpine JRE 21)
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Cài đặt curl phục vụ Actuator Healthcheck & múi giờ chuẩn VN (Asia/Ho_Chi_Minh)
RUN apk add --no-cache curl tzdata bash && \
    cp /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime && \
    echo "Asia/Ho_Chi_Minh" > /etc/timezone

WORKDIR /app

# 1. Bảo mật: Tạo Non-root System User & Group (UID 10001) chống leo thang đặc quyền
RUN addgroup -g 10001 -S appgroup && \
    adduser -u 10001 -S appuser -G appgroup && \
    mkdir -p /app/uploads/certificates && \
    chown -R appuser:appgroup /app && \
    chmod -R 775 /app

# 2. Copy file JAR đã build từ Stage 1
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar /app/app.jar

# 3. Phân quyền và chuyển sang user không đặc quyền
USER appuser:appgroup

# Khai báo cổng phục vụ Core Booking Service
EXPOSE 8082

# Cấu hình JVM Flags chuyên sâu cho xử lý High-Throughput Booking (1,000+ users đồng thời)
ENV JAVA_OPTS="-XX:+UseG1GC \
               -XX:MaxGCPauseMillis=100 \
               -XX:+UseStringDeduplication \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/urandom \
               -Dfile.encoding=UTF-8"

# Liveness & Readiness Probes kiểm tra tình trạng dịch vụ thời gian thực
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://127.0.0.1:8082/management/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
