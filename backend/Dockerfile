# Build stage
# Using Maven on Temurin 17 Alpine to build the Java application
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy POM and download dependencies using cache mount for efficiency
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# Copy the rest of the source code
COPY . .

# Build the application using cache mount, skipping tests
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests

############################################################
# Run stage
# Using Temurin 17 JRE on Debian Bullseye for compatibility with pre-compiled Python wheels
############################################################
FROM eclipse-temurin:17-jre-bullseye

# 1) Install Python3, pip, venv, dev headers, build tools, and other dependencies using apt-get
RUN apt-get update && apt-get install -y --no-install-recommends \
      python3 \
      python3-pip \
      python3-venv \
      python3-dev \
      build-essential \
      libffi-dev \
      libssl-dev \
      zlib1g-dev \
      libjpeg-dev \
    # Clean up apt cache to reduce image size
    && rm -rf /var/lib/apt/lists/* \
    # Link python3 to python (optional, but can help scripts expecting 'python')
    && ln -sf /usr/bin/python3 /usr/bin/python

WORKDIR /app

# 2) Create a Python virtual environment
RUN python3 -m venv /opt/venv

# 3) Add venv bin to PATH - ensures subsequent commands use the venv
ENV PATH="/opt/venv/bin:$PATH"

# 4) Copy the compiled Spring Boot jar and Python scripts from the builder stage
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
COPY --from=builder /app/scripts ./scripts

# 5) Install CPU-only torch from the PyTorch index (within the venv)
# Using version 2.3.0 based on previous attempts, should find compatible wheels for Debian/Python3.9
RUN pip install --no-cache-dir \
      torch==2.3.0+cpu \
      --index-url https://download.pytorch.org/whl/cpu

# 6) Install the rest of your Python dependencies (within the venv)
# These should install much faster using pre-compiled wheels on Debian
RUN pip install --no-cache-dir \
      sentence-transformers \
      docx2pdf \
      python-docx \
      pdfminer.six \
      reportlab \
      openai

# Expose the port the Spring Boot application listens on
EXPOSE 8080

# Set the entrypoint to run the Spring Boot application
ENTRYPOINT ["java","-jar","app.jar"]