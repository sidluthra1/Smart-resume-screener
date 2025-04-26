# 1) build your Spring Boot jar with Maven (no change here)
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,id=your-m2-cache,target=/root/.m2 mvn dependency:go-offline -B
COPY . .
RUN --mount=type=cache,id=your-m2-cache,target=/root/.m2 mvn package -DskipTests

# 2) run stage on Debian-slim so pip can pull torch wheels
FROM eclipse-temurin:17-jre-slim

# install python3, pip and build deps
RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      python3 python3-pip build-essential libffi-dev libssl-dev \
 && ln -sf /usr/bin/python3 /usr/bin/python \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# copy in your Spring Boot jar
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# copy your python scripts folder
COPY --from=builder /app/scripts ./scripts

# install all python deps, including torch + sentence-transformers CPU wheel
RUN pip3 install --no-cache-dir \
      torch --index-url https://download.pytorch.org/whl/cpu \
      sentence-transformers \
      docx2pdf \
      python-docx \
      pdfminer.six \
      reportlab \
      openai

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
