# syntax=docker/dockerfile:1.6

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Cache dependencies first
COPY pom.xml ./
RUN mvn -B -q -e -DskipTests dependency:go-offline

# Build the jar
COPY src ./src
RUN mvn -B -q -e -DskipTests package \
    && cp target/*.jar app.jar

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build /workspace/app.jar app.jar

ENV JAVA_OPTS=""
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
