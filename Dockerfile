# Multi-stage build: gradle:8-jdk21 build stage -> eclipse-temurin:21-jre-alpine runtime

# Stage 1: Build
FROM gradle:8-jdk21 AS builder

WORKDIR /build

# Copy build files
COPY build.gradle.kts settings.gradle.kts gradle.properties* detekt.yml* ./
COPY gradle ./gradle
COPY src ./src

# Build the application
RUN gradle build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

# Create non-root user for security
RUN addgroup -S fintech && adduser -S app -G fintech

WORKDIR /app

# Copy the JAR from build stage
COPY --from=builder /build/build/libs/currency-converter-kotlin-*.jar app.jar

# Set ownership to non-root user
RUN chown -R app:fintech /app

# Switch to non-root user
USER app

# Expose port
EXPOSE 8080

# Health check (requires curl or wget in image, or use healthcheck via external means)
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD java -cp app.jar org.springframework.boot.loader.launch.PropertiesLauncher \
        -Dspring.boot.strategy=org.springframework.boot.loader.PropertiesLauncher \
        || exit 1

# Start application
ENTRYPOINT ["java", "-jar", "app.jar"]


