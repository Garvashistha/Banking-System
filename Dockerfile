# ===== Build Stage =====
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only pom.xml first to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy the full source
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -X


# ===== Run Stage =====
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
