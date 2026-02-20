# --- Build stage ---
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradle/ gradle/
COPY gradlew settings.gradle.kts build.gradle.kts ./
RUN ./gradlew dependencies --no-daemon --quiet

COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseVirtualThreads", \
  "-XX:MaxRAMPercentage=75", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
