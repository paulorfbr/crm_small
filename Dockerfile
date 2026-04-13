# ── Stage 1: Build ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /workspace

COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./

# Download dependencies in a separate layer (cache-friendly)
RUN ./mvnw dependency:go-offline -q

COPY src ./src
COPY ui ./ui

# Build fat JAR, skip tests (tests require a live PostgreSQL)
RUN ./mvnw package -DskipTests -q

# ── Stage 2: Runtime ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Non-root user for least-privilege execution
RUN addgroup -S crm && adduser -S crm -G crm
USER crm

COPY --from=build /workspace/target/crm-small-*.jar app.jar

# Expose default Spring Boot port
EXPOSE 8080

# JVM tuning for containers:
#   - UseContainerSupport: honour cgroup memory limits
#   - MaxRAMPercentage:    use up to 75 % of available RAM for the heap
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
