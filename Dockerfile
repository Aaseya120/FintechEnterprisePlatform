# Multi-stage Dockerfile for Core Banking Microservices
# Stage 1: Build stage with Maven and Eclipse Temurin JDK 21
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy root pom and module poms for dependency layer caching
COPY pom.xml .
COPY banking-common/pom.xml banking-common/
COPY api-gateway/pom.xml api-gateway/
COPY account-service/pom.xml account-service/
COPY customer-service/pom.xml customer-service/
COPY payment-service/pom.xml payment-service/
COPY loan-service/pom.xml loan-service/
COPY card-service/pom.xml card-service/
COPY notification-service/pom.xml notification-service/
COPY fraud-detection-service/pom.xml fraud-detection-service/
COPY reporting-service/pom.xml reporting-service/
COPY batch-service/pom.xml batch-service/
COPY exchange-rate-service/pom.xml exchange-rate-service/

# Build offline dependencies
RUN apk add --no-cache maven && mvn dependency:go-offline -B

# Copy all source trees and build artifacts
COPY . .
ARG MODULE_NAME
RUN mvn clean package -pl ${MODULE_NAME} -am -DskipTests

# Stage 2: Hardened, Minimal Runtime Container
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-privileged financial service user for container security
RUN addgroup -S banking && adduser -S banking -G banking

ARG MODULE_NAME
COPY --from=builder /workspace/${MODULE_NAME}/target/*.jar app.jar

# Enforce secure file ownership
RUN chown -R banking:banking /app
USER banking:banking

# Production JVM tuning for high-throughput containerized workloads
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/tmp \
               -Djava.security.egd=file:/dev/./urandom \
               -XX:+UseG1GC \
               -XX:InitiatingHeapOccupancyPercent=45 \
               -XX:G1ReservePercent=15"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
