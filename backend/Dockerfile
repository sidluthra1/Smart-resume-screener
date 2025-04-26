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

# 1) Install Python3, pip and build tools
RUN apk add --no-cache \
      python3 \
      py3-pip \
      build-base \
      libffi-dev \
      openssl-dev \
      zlib-dev \
      libjpeg-turbo-dev \
    && ln -sf /usr/bin/python3 /usr/bin/python

WORKDIR /app

# 2) Copy your Spring Boot jar and Python scripts
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
COPY --from=builder /app/scripts ./scripts

# 3) Install CPU-only torch from the PyTorch index
RUN pip3 install --no-cache-dir \
      torch==1.13.1+cpu \
      --index-url https://download.pytorch.org/whl/cpu

# 4) Install the rest of your Python dependencies (from PyPI)
RUN pip3 install --no-cache-dir \
      sentence-transformers \
      docx2pdf \
      python-docx \
      pdfminer.six \
      reportlab \
      openai

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
