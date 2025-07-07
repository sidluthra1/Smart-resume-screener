

import sys
import re
import pdfplumber
import docx
import json
from sentence_transformers import SentenceTransformer, util
import torch
import traceback


# Keep the text extraction function as is, but add cleaning
def extract_text(path):
    if path.lower().endswith(".pdf"):
        text = ""
        with pdfplumber.open(path) as pdf:
            for page in pdf.pages:
                page_text = page.extract_text()
                if page_text:
                    text += page_text + "\n"
        text = re.sub(r'\s+', ' ', text).strip()
        return text
    elif path.lower().endswith(".docx"):
        try:
            doc = docx.Document(path)
            full_text = [p.text for p in doc.paragraphs]
            text = "\n".join(full_text)
            text = re.sub(r'\s+', ' ', text).strip()
            return text
        except Exception as e:
             # Handle potential errors reading corrupted docx files
            raise ValueError(f"Error reading DOCX file: {path} - {e}")
    else:
        raise ValueError(f"Unsupported file type: {path}")


def calculate_semantic_similarity(text_a, text_b, model):
    """Calculates cosine similarity between text embeddings."""
    try:
        # Encode texts into embeddings (vectors)
        embedding_a = model.encode(text_a, convert_to_tensor=True)
        embedding_b = model.encode(text_b, convert_to_tensor=True)

        # Calculate cosine similarity
        cosine_scores = util.cos_sim(embedding_a, embedding_b)
        similarity = cosine_scores.item() # Get the float value from the tensor

        # Scale similarity score from ~[0, 1] range to [0, 100]
        scaled_score = max(0.0, min(1.0, similarity)) * 100.0

        return scaled_score

    except Exception as e:
        # Handle potential errors during encoding or similarity calculation
        print(f"Error during semantic similarity calculation: {e}", file=sys.stderr)
        return 0.0


def main():
    if len(sys.argv) != 3:
        print("Usage: python semantic_matcher.py <resume.pdf|.docx> <job.txt>")
        sys.exit(1)

    resume_path, job_path = sys.argv[1], sys.argv[2]

    try:
        model = SentenceTransformer('all-MiniLM-L6-v2')

        # Extract text (function now includes basic cleaning)
        resume_txt = extract_text(resume_path)
        with open(job_path, "r", encoding="utf-8") as f:
            job_txt = f.read()
            # Simple cleaning for job description text as well
            job_txt = re.sub(r'\s+', ' ', job_txt).strip()

        # Handle empty text cases which can cause errors
        if not resume_txt or not job_txt:
             print(f"Error: Empty content found in resume or job description.", file=sys.stderr)
             score = 0.0
        else:
            score = calculate_semantic_similarity(resume_txt, job_txt, model)

        # Output JSON
        print(json.dumps({"Overlap": round(score, 2)}))

    except ValueError as ve:
        print(f"Error processing files: {ve}", file=sys.stderr)
        # Output a score of 0 or error JSON if preferred
        print(json.dumps({"Match Score": 0.0, "error": str(ve)}))
        sys.exit(1)
    except Exception as e:
        # Catch any other unexpected errors (e.g., model loading issues)
        print(f"An unexpected error occurred: {e}", file=sys.stderr)
        # Output a score of 0 or error JSON
        print(json.dumps({"Match Score": 0.0, "error": f"Unexpected error: {e}"}))
        sys.exit(1)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        # print full Python stack trace to stderr (redirected into your Spring logs)
        traceback.print_exc(file=sys.stderr)
        sys.exit(1)