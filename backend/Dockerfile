# Build stage
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,id=your-m2-cache,target=/root/.m2 mvn dependency:go-offline -B

COPY . .
RUN --mount=type=cache,id=your-m2-cache,target=/root/.m2 mvn package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
