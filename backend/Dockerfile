# 1) Build Stage --------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# cache Maven downloads
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

# build your JAR
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests


# 2) Runtime Stage ------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy

# install python, venv, build-tools, system libs
RUN apt-get update && apt-get install -y --no-install-recommends \
      python3 python3-venv python3-dev build-essential \
      libffi-dev libssl-dev zlib1g-dev libjpeg-dev \
    && rm -rf /var/lib/apt/lists/* \
    # ensure "python" points to python3
    && ln -sf /usr/bin/python3 /usr/bin/python

WORKDIR /app

# copy your Spring Boot JAR and Python scripts
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
COPY --from=builder /app/scripts ./scripts

# create & activate a venv in one layer
RUN python -m venv venv
ENV PATH="/app/venv/bin:$PATH"

# install PyTorch + all other deps in one shot
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

# ensure your JwtUtil / AuthService picks up this venv-python
ENV ai.python-executable=python

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
