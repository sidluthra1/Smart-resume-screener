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

# 1) Install Python3, pip3, and build tools for native extensions
RUN apk add --no-cache \
      python3 \
      py3-pip \
      build-base \
      libffi-dev \
      openssl-dev \
      zlib-dev \
      libjpeg-turbo-dev \
    && ln -sf /usr/bin/python3 /usr/bin/python

# 2) Make sure `python` → python3
RUN ln -sf /usr/bin/python3 /usr/bin/python

WORKDIR /app

# 3) Copy your Spring Boot jar
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# 4) Copy in your Python scripts
COPY --from=builder /app/scripts ./scripts

# 5) Install the Python dependencies
RUN pip3 install --no-cache-dir --break-system-packages \
      docx2pdf \
      python-docx \
      pdfminer.six \
      reportlab \
      openai \
      sentence-transformers

# 6) Expose port & start
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
