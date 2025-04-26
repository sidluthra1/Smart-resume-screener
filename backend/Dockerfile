# Build stage
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,id=your-m2-cache,target=/root/.m2 \
    mvn dependency:go-offline -B

COPY . .
RUN --mount=type=cache,id=your-m2-cache,target=/root/.m2 \
    mvn package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine

# 1) Install Python3
RUN apk add --no-cache python3

# 2) (Optional) Make sure "python" is python3
RUN ln -sf /usr/bin/python3 /usr/bin/python

WORKDIR /app

# 3) Copy the Spring Boot jar
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# 4) Copy your Python scripts into /app/scripts
COPY --from=builder /app/scripts ./scripts

ENTRYPOINT ["java","-jar","app.jar"]
