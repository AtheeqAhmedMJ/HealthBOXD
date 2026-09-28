# Multi-stage build for production-optimized image
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy POM and download dependencies
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Copy source code
COPY src ./src

# Build JAR
RUN mvn -q clean package -DskipTests

# Production image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q -O- http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# Run application with optimized JVM settings
ENTRYPOINT ["java", \
    "-Xms256m", \
    "-Xmx512m", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-XX:+UnlockExperimentalVMOptions", \
    "-XX:G1NewCollectionPercentThreshold=30", \
    "-Dspring.profiles.active=prod", \
    "-jar", \
    "app.jar"]
