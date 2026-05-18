# ──────────────────────────────────────────────
# Multi-stage Docker build for Render
# Stage 1: Build the JAR with Maven
# Stage 2: Run the JAR with JRE
# ──────────────────────────────────────────────

FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
# Download dependencies first (layer caching)
RUN mvn dependency:go-offline -B -q || true
COPY src ./src
RUN mvn package -DskipTests -B -q

FROM eclipse-temurin:17-jre
WORKDIR /app

# Create data directory for SimpleVectorStore (local fallback)
RUN mkdir -p /app/data

COPY --from=builder /app/target/*.jar app.jar

# Render provides PORT env var
EXPOSE ${PORT:-8081}

# JVM optimizations for container
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
