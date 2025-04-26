# Build stage
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,id=m2-target,target=/root/.m2 \
    mvn dependency:go-offline -B

COPY . .
RUN --mount=type=cache,id=m2-target,target=/root/.m2 \
    mvn package -DskipTests

# Run stage — use Debian-slim so we can install torch wheels
FROM openjdk:17-slim

# 1) Install Python3, pip, and build deps
RUN apt-get update && apt-get install -y \
      python3 python3-pip build-essential libffi-dev libssl-dev zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 2) Copy in your Spring Boot jar
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# 3) Copy your Python scripts
COPY --from=builder /app/scripts ./scripts

# 4) Install a CPU-only torch wheel first, then the rest of your Python deps
RUN pip3 install --no-cache-dir \
      torch==1.13.1+cpu --index-url https://download.pytorch.org/whl/cpu \
      sentence-transformers \
      docx2pdf \
      python-docx \
      pdfminer.six \
      reportlab \
      openai

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
