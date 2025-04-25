#!/usr/bin/env python3
"""
score_resumes.py
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Reads a single JSON object from STDIN that contains:
    resume_text   : str  (plain‑text resume)
    job_text      : str  (plain‑text job description)
    resume_json   : obj  (parsed resume from ResumeParser.py)
    job_json      : obj  (parsed JD   from JobDescriptionParser.py)

Writes a JSON object to STDOUT with:
    SemanticScore, SkillsScore, EducationScore, ExperienceScore, FinalScore

Sub‑scores:
    • SemanticScore   — sentence‑transformer cosine similarity (0‑100)
    • SkillsScore     — OpenAI grades skills overlap            (0‑100)
    • EducationScore  — OpenAI grades education fit             (0‑100)
    • ExperienceScore — OpenAI grades experience fit            (0‑100)

FinalScore = 40 % Semantic + 30 % Skills + 15 % Education + 15 % Experience
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
"""

import sys, json, os, re
from pathlib import Path
from openai import OpenAI
from sentence_transformers import SentenceTransformer, util

# ---------- weights ------------------------------------------------------
W_SEM, W_SK, W_ED, W_EX, W_OV, W_LLM = 0.05, 0.15, 0.25, 0.15, 0.05, 0.35
# -------------------------------------------------------------------------

# ---------- helpers ------------------------------------------------------
def read_stdin_json() -> dict:
    try:
        data = sys.stdin.read()
        return json.loads(data)
    except json.JSONDecodeError as e:
        sys.stderr.write(f"Invalid JSON on stdin – {e}\n")
        sys.exit(1)

def clean(txt: str) -> str:
    return re.sub(r"\s+", " ", txt or "").strip()

def semantic_score(r_txt: str, j_txt: str, model) -> float:
    emb_r = model.encode(r_txt, convert_to_tensor=True)
    emb_j = model.encode(j_txt, convert_to_tensor=True)
    cos   = util.cos_sim(emb_r, emb_j).item()         # −1…1
    return max(0.0, min(1.0, cos))*100                # 0…100
# -------------------------------------------------------------------------

def main() -> None:
    payload = read_stdin_json()

    resume_txt   = clean(payload.get("resume_text", ""))
    job_txt      = clean(payload.get("job_text", ""))
    resume_json  = payload.get("resume_json", {})
    job_json     = payload.get("job_json",   {})

    overlap = float(payload.get("Overlap", 0.0))  # NEW
    llm_score = float(payload.get("LLMscore", 0.0))  # NEW


    if not resume_txt or not job_txt:
        sys.stderr.write("resume_text or job_text missing/empty\n")
        sys.exit(1)

    # -- 1. semantic similarity ------------------------------------------
    model = SentenceTransformer('all-MiniLM-L6-v2')
    sem_score = semantic_score(resume_txt, job_txt, model)

    # -- 2‑4. let OpenAI grade the structured pieces ---------------------
    client = OpenAI()        # needs OPENAI_API_KEY in environment

    user_prompt = (
        "You are an ATS scoring engine. "
        "Using the parsed JSON for a RESUME and a JOB DESCRIPTION, "
        "output STRICT JSON with *exactly* three numeric keys "
        "SkillsScore, EducationScore, ExperienceScore (each 0‑100, floats). "
        "• SkillsScore ‑ percentage fit of resume skills to job skills. "
        "• EducationScore ‑ how well education meets/ exceeds requirements "
        "(higher degree that satisfies requirement ⇒ higher score). "
        "• ExperienceScore ‑ relevance & years vs. JD responsibilities.\n\n"
        f"RESUME_JSON:\n{json.dumps(resume_json)}\n\n"
        f"JOB_JSON:\n{json.dumps(job_json)}"
    )

    chat = client.chat.completions.create(
        model="o4-mini",
        response_format={"type": "json_object"},
        messages=[
            {"role": "system", "content": "You output only JSON with the keys requested."},
            {"role": "user",   "content": user_prompt}
        ]
    )

    subs = json.loads(chat.choices[0].message.content)
    sk   = float(subs.get("SkillsScore",     0.0))
    ed   = float(subs.get("EducationScore",  0.0))
    ex   = float(subs.get("ExperienceScore", 0.0))

    final = (
            sem_score * W_SEM +
            sk * W_SK +
            ed * W_ED +
            ex * W_EX +
            overlap * W_OV +
            llm_score * W_LLM
    )

    print(json.dumps({
        "SemanticScore": round(sem_score, 1),
        "SkillsScore": round(sk, 1),
        "EducationScore": round(ed, 1),
        "ExperienceScore": round(ex, 1),
        "Overlap": round(overlap, 1),
        "LLMscore": round(llm_score, 1),
        "FinalScore": round(final, 1)
    }))

# -------------------------------------------------------------------------
if __name__ == "__main__":
    main()
