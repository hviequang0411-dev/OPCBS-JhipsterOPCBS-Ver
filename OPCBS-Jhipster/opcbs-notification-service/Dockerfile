FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml checkstyle.xml sonar-project.properties ./
COPY build-plugins ./build-plugins
RUN mvn dependency:go-offline -B || true

COPY src ./src
RUN mvn clean package -DskipTests -Dfrontend.skip=true -Dmaven.gitcommitid.skip=true -Dmodernizer.skip=true -Dcheckstyle.skip=true -B

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN apk add --no-cache curl tzdata bash && \
    cp /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime && \
    echo "Asia/Ho_Chi_Minh" > /etc/timezone

WORKDIR /app

RUN addgroup -g 10001 -S appgroup && \
    adduser -u 10001 -S appuser -G appgroup

COPY --from=builder --chown=appuser:appgroup /build/target/*.jar /app/app.jar
USER appuser:appgroup

EXPOSE 8086

ENV JAVA_OPTS="-XX:+UseG1GC \
               -XX:MaxGCPauseMillis=100 \
               -XX:+UseStringDeduplication \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/urandom \
               -Dfile.encoding=UTF-8"

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://127.0.0.1:8086/management/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
