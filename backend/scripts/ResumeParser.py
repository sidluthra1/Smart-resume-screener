import json
import sys
import os
import shutil
import time
from pathlib import Path
from reportlab.lib.pagesizes import LETTER
import docx

try:
    from docx2pdf import convert
except ImportError:
    convert = None

from openai import OpenAI

# initialize OpenAI client (make sure OPENAI_API_KEY is set in your env)
client = OpenAI()


def openAIParser(src: Path):
    pdf_path = ensure_pdf(src)
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

def _convert_docx_to_pdf_text(src: Path, dst: Path) -> None:
    """
    Fallback: extract text from .docx and render to PDF via reportlab
    """
    doc = docx.Document(str(src))
    text = "\n".join(p.text for p in doc.paragraphs)

    c = canvas.Canvas(str(dst), pagesize=LETTER)
    width, height = LETTER
    margin, line_height = 72, 12
    y = height - margin
    for line in text.splitlines():
        if y < margin:
            c.showPage()
            y = height - margin
        c.drawString(margin, y, line)
        y -= line_height
    c.save()


def ensure_pdf(src: Path) -> Path:
    """
    Ensure we have a PDF; for .docx use either docx2pdf (if on Windows) or fallback.
    """
    src = src.expanduser().resolve()
    suffix = src.suffix.lower()
    dst = src.with_suffix(".pdf")

    if suffix == ".pdf":
        return src

    if suffix == ".docx":
        try:
            from docx2pdf import convert as convert_win
        except ImportError:
            convert_win = None

        if convert_win:
            convert_win(str(src), str(dst))
        else:
            _convert_docx_to_pdf_text(src, dst)
        if not dst.exists():
            raise RuntimeError(f"Conversion failed, no output at: {dst}")
        return dst

    raise ValueError(f"Unsupported extension '{src.suffix}'; only .docx or .pdf allowed")


def main():
    if len(sys.argv) != 2:
        print("Usage: ResumeParser.py <path-to-resume.pdf|.docx>", file=sys.stderr)
        sys.exit(1)
    src = Path(sys.argv[1])
    if not src.exists():
        print(f"No such file: {src}", file=sys.stderr)
        sys.exit(1)
    openAIParser(src)

if __name__ == "__main__":
    main()


