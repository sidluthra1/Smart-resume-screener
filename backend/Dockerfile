# 1) Build stage
FROM maven:3.8.7-openjdk-17-slim AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

# 2) Run stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# configure an uploads directory
ENV FILE_UPLOAD_DIR=/tmp/uploads
VOLUME ["${FILE_UPLOAD_DIR}"]

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
