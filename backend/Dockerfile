# Build stage
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app
COPY pom.xml .
# Use cache mount for Maven dependencies
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

COPY . .
# Use cache mount for Maven build process
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine

# 1) Install Python3, pip, and build tools
# Removed python3-venv as it's typically included in the python3 package
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

# 2) Create a virtual environment
# This command should work as 'venv' module comes with 'python3'
RUN python3 -m venv /opt/venv

# 3) Activate the virtual environment for subsequent RUN commands and set PATH
# This makes sure 'pip' and 'python' commands use the venv's versions
ENV PATH="/opt/venv/bin:$PATH"

# 4) Copy your Spring Boot jar and Python scripts
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
COPY --from=builder /app/scripts ./scripts

# 5) Install CPU-only torch from the PyTorch index (within the venv)
RUN pip install --no-cache-dir \
      torch==2.3.0+cpu \
      --index-url https://download.pytorch.org/whl/cpu

# 6) Install the rest of your Python dependencies (within the venv)
RUN pip install --no-cache-dir \
      sentence-transformers \
      docx2pdf \
      python-docx \
      pdfminer.six \
      reportlab \
      openai

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]