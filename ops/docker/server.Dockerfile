FROM gradle:8.12-jdk17 AS builder
WORKDIR /workspace
COPY . .
RUN gradle :server:bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/server/build/libs/server-1.0.0.jar app.jar
RUN addgroup -S app && adduser -S app -G app \
    && apk add --no-cache wget
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=docker"]
