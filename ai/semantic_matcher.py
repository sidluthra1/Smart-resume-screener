#!/usr/bin/env python3

import sys
import re
import pdfplumber
import docx
import json


def extract_text(path):
    if path.lower().endswith(".pdf"):
        text = ""
        with pdfplumber.open(path) as pdf:
            for page in pdf.pages:
                txt = page.extract_text()
                if txt:
                    text += txt + "\n"
        return text
    elif path.lower().endswith(".docx"):
        doc = docx.Document(path)
        return "\n".join(p.text for p in doc.paragraphs)
    else:
        raise ValueError(f"Unsupported file type: {path}")


def jaccard_score(a, b):
    toks_a = set(re.findall(r"\w+", a.lower()))
    toks_b = set(re.findall(r"\w+", b.lower()))
    if not toks_a and not toks_b:
        return 0.0
    inter = toks_a & toks_b
    union = toks_a | toks_b
    return len(inter) / len(union) * 100


def main():
    if len(sys.argv) != 3:
        print("Usage: python semantic_matcher.py <resume.pdf|.docx> <job.txt>")
        sys.exit(1)

    resume_path, job_path = sys.argv[1], sys.argv[2]
    resume_txt = extract_text(resume_path)

    with open(job_path, "r", encoding="utf-8") as f:
        job_txt = f.read()

    score = jaccard_score(resume_txt, job_txt)
    output = {"Match Score": score}
    print(json.dumps(output))


if __name__ == "__main__":
    main()
