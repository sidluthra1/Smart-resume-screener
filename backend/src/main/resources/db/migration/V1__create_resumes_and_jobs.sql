-- V1__create_resumes_and_jobs.sql
-- create core tables for jobs, resumes, skills, join tables, experiences

CREATE TABLE skills (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE job_descriptions (
                                  id SERIAL PRIMARY KEY,
                                  title VARCHAR(255) NOT NULL,
                                  category VARCHAR(255),
                                  location VARCHAR(255),
                                  description_text TEXT NOT NULL,
                                  summary VARCHAR(2000),
                                  requirements TEXT,
                                  responsibilities TEXT,
                                  parsed_json JSONB
);

CREATE TABLE resumes (
                         id SERIAL PRIMARY KEY,
                         file_name VARCHAR(255) NOT NULL,
                         candidate_name VARCHAR(255) NOT NULL,
                         upload_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
                         file_path TEXT NOT NULL,
                         content_type VARCHAR(255) NOT NULL,
                         size BIGINT NOT NULL,
                         match_score DOUBLE PRECISION,
                         email VARCHAR(320),
                         phone VARCHAR(32),
                         summary TEXT,
                         education VARCHAR(256)
);

CREATE TABLE resume_skills (
                               resume_id INTEGER NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
                               skill_id  INTEGER NOT NULL REFERENCES skills(id)  ON DELETE CASCADE,
                               PRIMARY KEY (resume_id, skill_id)
);

CREATE TABLE job_skills (
                            job_id   INTEGER NOT NULL REFERENCES job_descriptions(id) ON DELETE CASCADE,
                            skill_id INTEGER NOT NULL REFERENCES skills(id)          ON DELETE CASCADE,
                            PRIMARY KEY (job_id, skill_id)
);

CREATE TABLE experiences (
                             id SERIAL PRIMARY KEY,
                             resume_id INTEGER NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
                             description TEXT
);
