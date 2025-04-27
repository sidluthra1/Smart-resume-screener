# 1) Build your Jar in a Maven container
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests

# 2) Runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install python3 + venv and system deps
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
      python3 python3-venv python3-dev build-essential \
      libffi-dev libssl-dev zlib1g-dev libjpeg-dev && \
    rm -rf /var/lib/apt/lists/*

# Copy your Spring JAR and the AI scripts
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
COPY --from=builder /app/scripts       scripts

# Create & activate venv
RUN python3 -m venv /app/venv
ENV PATH="/app/venv/bin:$PATH"

# Install all your Python dependencies into the venv
RUN pip install --no-cache-dir \
      --extra-index-url https://download.pytorch.org/whl/cpu \
      torch==2.3.0+cpu \
      sentence-transformers \
      numpy \
      scipy \
      scikit-learn \
      sentencepiece \
      transformers \
      docx2pdf \
      python-docx \
      pdfminer.six \
      reportlab \
      pdfplumber \
      openai

# Tell Spring Boot to use the venv‐python for all its ai.python-executable calls
ENV AI_PYTHON_EXECUTABLE=/app/venv/bin/python

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
