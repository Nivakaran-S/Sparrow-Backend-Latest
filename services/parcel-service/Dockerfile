# ----------------------------
# Build stage
# ----------------------------
FROM gradle:8.4.0-jdk17-alpine AS build
WORKDIR /app

# Ensure certificates are up-to-date in the same step
RUN apk update && \
    apk add --no-cache ca-certificates bash curl

# Copy project files
COPY build.gradle settings.gradle ./
COPY src ./src

# Build the project
RUN gradle build -x test --no-daemon

# ----------------------------
# Runtime stage
# ----------------------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install wget for healthcheck
RUN apk add --no-cache wget

# Copy built jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Expose port
EXPOSE 8081

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
