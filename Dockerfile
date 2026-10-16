# =========================================================================
# Multi-Stage Dockerfile for Employee Leave Platform
# Stage 1: Build & Package
# Stage 2: Minimal Distroless / Temurin JRE Runtime (Non-root user)
# =========================================================================

# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

# Cache dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

# Copy source code and package application (skip tests in container build; tests run in CI)
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Run as dedicated unprivileged system user for security
RUN groupadd -r appgroup && useradd -r -g appgroup -s /bin/false appuser

COPY --from=builder --chown=appuser:appgroup /workspace/target/*.jar app.jar

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
