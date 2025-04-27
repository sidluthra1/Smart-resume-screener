# Smart Resume Screener

A full‑stack web application that lets HR professionals upload resumes and job descriptions, then leverages AI (OpenAI + NLP) to parse, match, and score candidate fit.

---

## 🚀 Features

- **User Authentication**: Signup & login with JWT (Spring Security + React).
- **Resume Upload & Parsing**: Upload PDF/DOCX, extract text/fields via Python scripts.
- **Job Description Entry**: Manual entry or file upload, parsed via Python + OpenAI.
- **Semantic Matching & Scoring**: BERT-based & LLM scoring (0–100) with breakdown (skills, experience, education).
- **Dashboards & Detail Pages**: React UI with Tailwind, showing lists, filters, badges, charts.

---

## 🏗️ Architecture

```
┌─────────────────────┐       ┌─────────────────┐      ┌─────────────────────┐
│    React Frontend   │ ←→ Axios │  Spring Boot API │ ←→ │ Python AI Services  │
└─────────────────────┘       └─────────────────┘      └─────────────────────┘
       | JWT auth                                 | spa  
       | REST endpoints                           | scripts + OpenAI
```

- **Frontend**: React, TailwindCSS, react‑router, axios
- **Backend**: Spring Boot, Spring Security, JPA + PostgreSQL
- **AI**: Python scripts for parsing, `sentence-transformers` for semantic matching, OpenAI for LLM scoring
- **Deployment**: Docker multi‑stage build, Alpine (builder) + Ubuntu Jammy (runtime)

---

## 📥 Getting Started

### Prerequisites

- Java 17
- Maven
- Node.js & npm
- Docker & Docker Compose
- (Optional) Python locally for AI scripts

### Environment Variables

Create a `.env` file in the root with:

```dotenv
# Spring Boot
DB_HOST=<your-db-host>
DB_PORT=<your-db-port>
DB_NAME=<your-db-name>
DB_USER=<your-db-user>
DB_PASS=<your-db-password>
JWT_SECRET=<your-256-bit-secret>
AI_PYTHON_EXECUTABLE=/opt/venv/bin/python
FILE_UPLOAD_DIR=/path/to/uploads

# OpenAI
OPENAI_API_KEY=<your-openai-key>
```

### Local Development

#### Backend

```bash
cd backend
mvn spring-boot:run
```

#### Frontend

```bash
cd frontend
npm install
npm start
```

#### Python AI scripts

```bash
cd backend/scripts
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

---

## 🐳 Docker Deployment

```bash
# Build & run database
docker-compose up -d db

# Build backend & frontend images
docker build -t resume‑screener:latest .
docker run -d -p 8080:8080 --env-file .env --name api resume‑screener:latest
```

Optionally, front end can be served on Vercel or Netlify pointing at the API URL.

---

## 📑 API Endpoints

- `POST /auth/signup` — register new user
- `POST /auth/login` — obtain JWT
- `POST /job/createManual` — create JD via raw text
- `POST /job/uploadFile` — create JD via file
- `GET /job/all` — list JDs
- `GET /job/{id}` — single JD
- `DELETE /job/{id}` — delete JD
- `POST /resume/upload` — upload resume (+ optional jobId)
- `POST /resume/score` — re‑score existing resume
- `GET /resume/all` — list resumes
- `GET /resume/{id}` — resume details
- `PATCH /resume/{id}/status` — update status
- `GET /resume/download/{id}` — download file

---

## 👩‍💻 Code Structure

```
/backend       # Spring Boot + Python scripts
/frontend      # React + Tailwind
/scripts       # Python parsers & matchers
/Dockerfile    # Multi‑stage build
/docker-compose.yml
```

---

## 🛠️ Troubleshooting

- **JSONB cast errors**: ensure `parsed_json` is cast to `jsonb` in Hibernate mappings or DB column type.
- **Python missing deps**: adjust Dockerfile to install required pip packages (e.g. `pdfplumber`).
- **CORS / 502 errors**: verify front end baseURL matches deployed API URL and CORS config in `SecurityConfig`.

---

## 🎉 Credits

Built by Siddarth Luthra as a Smart Resume Screener coding challenge.

