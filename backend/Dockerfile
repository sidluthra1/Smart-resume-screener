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

# 1) Install Python3, pip, venv, and build tools
# Added python3-venv package
RUN apk add --no-cache \
      python3 \
      py3-pip \
      python3-venv \
      build-base \
      libffi-dev \
      openssl-dev \
      zlib-dev \
      libjpeg-turbo-dev \
    && ln -sf /usr/bin/python3 /usr/bin/python

WORKDIR /app

# 2) Create a virtual environment
RUN python3 -m venv /opt/venv

# 3) Activate the virtual environment for subsequent RUN commands and set PATH
# This makes sure 'pip' and 'python' commands use the venv's versions
ENV PATH="/opt/venv/bin:$PATH"

# 4) Copy your Spring Boot jar and Python scripts
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
COPY --from=builder /app/scripts ./scripts

# 5) Install CPU-only torch from the PyTorch index (within the venv)
# No need to prefix with 'source /opt/venv/bin/activate &&' because PATH is set
RUN pip install --no-cache-dir \
      torch==1.13.1+cpu \
      --index-url https://download.pytorch.org/whl/cpu

# 6) Install the rest of your Python dependencies (within the venv)
# Combined the remaining pip installs into one layer
RUN pip install --no-cache-dir \
      sentence-transformers \
      docx2pdf \
      python-docx \
      pdfminer.six \
      reportlab \
      openai

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]