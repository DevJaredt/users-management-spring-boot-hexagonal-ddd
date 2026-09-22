# =============================================================================
# Build stage — compila el JAR ejecutable con Maven + JDK 17
# =============================================================================
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY src ./src

RUN mvn -B -q clean package -DskipTests

# =============================================================================
# Runtime stage — imagen ligera solo con el JRE
# =============================================================================
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

# Railway inyecta el puerto en la variable PORT (server.port=${PORT:8080})
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
