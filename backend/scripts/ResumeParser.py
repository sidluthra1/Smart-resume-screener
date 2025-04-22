import json
import sys
import os
import shutil
import time
from pathlib import Path

try:
    from docx2pdf import convert
except ImportError:
    convert = None

from openai import OpenAI

# initialize OpenAI client (make sure OPENAI_API_KEY is set in your env)
client = OpenAI()


def openAIParser(src: Path):
    pdf_path = ifPDFConvert(src)
    # upload file for vector search
    with open(pdf_path, "rb") as f:
        up = client.files.create(file=f, purpose="assistants")
    file_id = up.id

    # create a new vector store and attach the file
    vs = client.vector_stores.create(name="my_resume_store")
    vs_id = vs.id
    client.vector_stores.files.create(vector_store_id=vs_id, file_id=file_id)

    while True:
        entries = client.vector_stores.files.list(vector_store_id=vs_id).data
        if entries and entries[0].status == "completed":
            break
        current = entries[0].status if entries else "no entries yet"
        time.sleep(1)

    response = client.responses.create(
        model="o4-mini",  # or another model you prefer
        input="Please parse the resume for all the information available from the attached PDF and return EXACTLY the JSON via the function. The summary property should include 1 summarizing sentence about the candidate on the resume. For any properties that are not available in the resume, please just respond with N/A.",
        tools=[
            {
                "type": "file_search",
                "vector_store_ids": [vs_id]
            }
        ],
        text={
            "format": {
                "type": "json_schema",
                "name": "resume_parser_schema",
                "schema": {
                    "type": "object",
                    "properties": {
                        "email": {
                            "type": "string",
                            "description": "The email address of the individual."
                        },
                        "phone_number": {
                            "type": "string",
                            "description": "The phone number of the individual."
                        },
                        "summary": {
                            "type": "string",
                            "description": "A one sentence summary of the individual."
                        },
                        "skills": {
                            "type": "string",
                            "description": "Comma-separated list of skills."
                        },
                        "work_experience": {
                            "type": "string",
                            "description": "Description of work experience in the format 'Role at Company (month/year started - present or month/year finished)'."
                        },
                        "education": {
                            "type": "string",
                            "description": "Description of education in the format 'Degree in Major, School (Year)'."
                        }
                    },
                    "required": [
                        "email",
                        "phone_number",
                        "summary",
                        "skills",
                        "work_experience",
                        "education"
                    ],
                    "additionalProperties": False
                }
            }
        }

    )
    # 4) Pull out the arguments and print
    resume_parser_schema = json.loads(response.output_text)
    print(json.dumps(resume_parser_schema))

def convert_to_pdf(src: Path, dst: Path = None) -> Path:
    """
    Converts a .docx to .pdf if needed, otherwise returns the .pdf path.
    """
    src = src.expanduser().resolve()
    if not src.exists():
        raise FileNotFoundError(f"No such file: {src}")
    if src.suffix.lower() != ".docx":
        return src  # already PDF
    if convert is None:
        raise RuntimeError("docx2pdf not installed; cannot convert .docx to .pdf")

    dst = dst.expanduser().resolve() if dst else src.with_suffix(".pdf")
    convert(str(src), str(dst))
    if not dst.exists():
        raise RuntimeError(f"Conversion failed, no output at: {dst}")
    return dst

def ifPDFConvert(src: Path) -> Path:
    """
    Ensure we have a PDF on disk; convert .docx → .pdf if necessary.
    """
    return convert_to_pdf(src) if src.suffix.lower() == ".docx" else src

def main():
    if len(sys.argv) != 2:
        print("Usage: ResumeParser.py <path-to-resume.pdf|.docx>", file=sys.stderr)
        sys.exit(1)
    src = Path(sys.argv[1])
    openAIParser(src)

# ---------- CLI --------------------------------------------------------
if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.stderr.write("Usage: ResumeParser.py <path-to-resume.pdf|.docx>\n")
        sys.exit(1)

    input_path = Path(sys.argv[1])
    if not input_path.exists():
        sys.stderr.write(f"No such file: {input_path}\n")
        sys.exit(1)

    pdf = convert_to_pdf(input_path)
    openAIParser(pdf)


