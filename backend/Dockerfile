############################################################
# Build stage (unchanged)
############################################################
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,id=your-m2-cache,target=/root/.m2 \
    mvn dependency:go-offline -B
COPY . .
RUN --mount=type=cache,id=your-m2-cache,target=/root/.m2 \
    mvn package -DskipTests

############################################################
# Run stage (Debian-slim so torch wheels install cleanly)
############################################################
FROM openjdk:17-jre-slim

# 1) Install Python3, pip and build‐deps
RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      python3 python3-pip build-essential \
      libffi-dev libssl-dev zlib1g-dev libjpeg-dev \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 2) Copy your fat JAR and all Python scripts
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
COPY --from=builder /app/scripts ./scripts

# 3) Install torch from the PyTorch index, then the rest from PyPI
RUN pip3 install --no-cache-dir \
      torch==1.13.1+cpu --index-url https://download.pytorch.org/whl/cpu \
 && pip3 install --no-cache-dir \
      sentence-transformers \
      docx2pdf \
      python-docx \
      pdfminer.six \
      reportlab \
      openai

# 4) Expose and run
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
