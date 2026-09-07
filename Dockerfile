# =========================
# 1. Build the application
# =========================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

# Use GitHub Maven credentials only during dependency resolution
RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    mvn dependency:go-offline

COPY src ./src

RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    mvn clean package -DskipTests


# =========================
# 2. Run the application
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/participation-service.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]