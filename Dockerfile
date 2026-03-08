# Multi-stage build: gradle:8-jdk21 → layer extractor → eclipse-temurin:21-jre-alpine
# Note: Spring Boot 3 fat JAR with JRE ≈ 260 MB compressed; distroless/jlink alternatives
# cause dynamic class-loading failures with JDBC/Hibernate. eclipse-temurin:21-jre-alpine
# is the recommended base per Spring Boot documentation.

# ── Stage 1: Build application ───────────────────────────────────────────────
FROM gradle:8-jdk21 AS builder
WORKDIR /build
COPY build.gradle.kts settings.gradle.kts gradle.properties* detekt.yml* ./
COPY gradle ./gradle
COPY src ./src
RUN gradle build -x test --no-daemon

# ── Stage 2: Extract layered JAR ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS extractor
WORKDIR /extract
COPY --from=builder /build/build/libs/currency-converter-kotlin-*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ── Stage 3: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S fintech && adduser -S app -G fintech && apk add --no-cache curl

WORKDIR /app

# Copy layers least-to-most volatile (maximises Docker layer cache reuse)
COPY --from=extractor /extract/dependencies/ ./
COPY --from=extractor /extract/spring-boot-loader/ ./
COPY --from=extractor /extract/snapshot-dependencies/ ./
COPY --from=extractor /extract/application/ ./

RUN chown -R app:fintech /app
USER app

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=6 \
    CMD curl -sf http://localhost:8080/api/v1/actuator/health > /dev/null 2>&1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
