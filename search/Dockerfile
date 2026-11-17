# =============================================================================
# Build — compila el JAR del microservicio search
# =============================================================================
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY src ./src

RUN mvn -B -q clean package -DskipTests

# =============================================================================
# Runtime
# =============================================================================
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

# Render inyecta el puerto en PORT (server.port=${PORT:8083})
EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
