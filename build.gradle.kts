import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// ─── Plugins ──────────────────────────────────────────────────────────────────
plugins {
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    // Opens @Component/@Service/@Configuration classes for Spring proxy generation
    kotlin("plugin.spring") version "1.9.22"
    // Generates no-arg constructors required by JPA @Entity / @Embeddable
    kotlin("plugin.jpa") version "1.9.22"
    id("org.flywaydb.flyway") version "10.8.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.5"
    jacoco
}

// ─── Project coordinates ──────────────────────────────────────────────────────
group = "com.fintech"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// ─── Repositories ─────────────────────────────────────────────────────────────
repositories {
    mavenCentral()
}

// ─── Dependencies ─────────────────────────────────────────────────────────────
dependencies {

    // ── Spring Boot starters ──────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // Required for Resilience4j annotation-driven proxying (@CircuitBreaker, @Retry)
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // ── Kotlin runtime ────────────────────────────────────────────────────────
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // ── JWT (JJWT 0.12.x — fluent builder API; incompatible with 0.11.x) ─────
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // ── Feign (OpenFeign for external HTTP calls via Spring Cloud) ────────────
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // ── Resilience4j ──────────────────────────────────────────────────────────
    // resilience4j-spring-boot3 is sufficient for annotation-driven CB + Retry.
    // resilience4j-feign is intentionally excluded — it is only needed when using
    // the FeignDecorators builder API directly, which we are not.
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")

    // ── Rate Limiting (Bucket4j + Lettuce/Redis) ──────────────────────────────
    // Spring Boot 3.2.x ships Lettuce 6.3.x; Bucket4j 8.9.0 supports Lettuce 6.x ✓
    implementation("com.bucket4j:bucket4j-core:8.9.0")
    implementation("com.bucket4j:bucket4j-redis:8.9.0")

    // ── Database ──────────────────────────────────────────────────────────────────
    // Flyway 10.x splits DB driver support into separate artifacts.
    // Spring Boot 3.2.x manages flyway-core to 9.x; explicit version pins both
    // artifacts to 10.8.1 so flyway-database-postgresql (a 10.x-only module) resolves.
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core:10.8.1")
    implementation("org.flywaydb:flyway-database-postgresql:10.8.1")

    // ── Observability ─────────────────────────────────────────────────────────
    implementation("io.micrometer:micrometer-registry-prometheus")

    // ── API Documentation ─────────────────────────────────────────────────────
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // ── Logging (Logstash JSON encoder — prod profile) ────────────────────────
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // ── Testing ───────────────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")

    // MockK is the idiomatic Kotlin mocking library; springmockk bridges it with @MockBean
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("com.ninja-squad:springmockk:4.0.2")

    // Testcontainers BOM pins all TC artifacts to a single consistent version
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.19.4"))
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")

    // WireMock standalone — wiremock-jre8 is incompatible with Java 21 module system
    testImplementation("org.wiremock:wiremock:3.5.4")

    // H2 in-memory DB — pure unit tests only; never used for integration tests
    testRuntimeOnly("com.h2database:h2")
}

// ─── Spring Cloud BOM (pins Feign and other cloud starters) ──────────────────
dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.0")
    }
}

// ─── Kotlin compiler options ──────────────────────────────────────────────────
tasks.withType<KotlinCompile> {
    kotlinOptions {
        // Strict JSR-305 null-safety: Spring annotations become Kotlin non-null contracts
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

// ─── Test configuration ───────────────────────────────────────────────────────
tasks.withType<Test> {
    useJUnitPlatform()
}

// ─── JaCoCo coverage report ───────────────────────────────────────────────────
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal() // 80% line coverage — DoD D.7
            }
        }
    }
}

// ─── Detekt static analysis ───────────────────────────────────────────────────
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$projectDir/detekt.yml"))
}





