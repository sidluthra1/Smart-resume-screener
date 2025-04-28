# 1) Build stage --------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# grab deps
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

# build jar
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests


# 2) Runtime stage ------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# install python3 + venv + minimal system libs
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
      python3 python3-venv python3-dev build-essential \
      libffi-dev libssl-dev zlib1g-dev libjpeg-dev && \
    rm -rf /var/lib/apt/lists/*

# copy your fat-jar and your AI scripts
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
COPY --from=builder /app/scripts               scripts

# make & enter a venv
RUN python3 -m venv /app/venv
ENV PATH="/app/venv/bin:$PATH"

# install everything you need into the venv
RUN pip install --no-cache-dir \
      --extra-index-url https://download.pytorch.org/whl/cpu \
      torch==2.3.0+cpu \
      sentence-transformers \
      numpy scipy scikit-learn sentencepiece transformers \
      docx2pdf python-docx pdfminer.six reportlab pdfplumber openai

#–– tell Spring Boot “ai.python-executable” to use the venv’s interpreter ––
#    (Spring Boot will pick this up as the `@Value("${ai.python-executable}")`)
ENV AI_PYTHON_EXECUTABLE=/app/venv/bin/python3

#–– and point all of your python.* scripts at the absolute paths in /app/scripts ––
#    Spring Boot’s `@Value("${python.text-extractor}")` ←→ $PYTHON_TEXT_EXTRACTOR
ENV PYTHON_TEXT_EXTRACTOR=/app/scripts/text_extractor.py
ENV PYTHON_RESUME_PARSER=/app/scripts/ResumeParser.py
ENV PYTHON_JOB_PARSER=/app/scripts/JobDescriptionParser.py
ENV PYTHON_SEMANTIC_MATCHER=/app/scripts/semantic_matcher.py
ENV PYTHON_SCORER=/app/scripts/score_resumes.py

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
