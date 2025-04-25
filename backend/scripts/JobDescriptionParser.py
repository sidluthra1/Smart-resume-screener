import json
import sys
import os
import shutil
import time
from pathlib import Path
from docx2pdf import convert as _convert_docx
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import LETTER
from openai import OpenAI
client = OpenAI()


def openAIParser(src: Path):
    pdf_path = convert_to_pdf_general(src)
    with open(pdf_path, "rb") as f:
        up = client.files.create(file=f, purpose="assistants")
    file_id = up.id

    # create a new vector store and attach the file
    vs = client.vector_stores.create(name="my_job_description_store")
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
        input="Please parse the job for all the information available from the attached PDF and return EXACTLY the JSON via the function. The summary property should include 1 summarizing sentence about the job. You are also to interperit the Job Category as well. For the skills section, please limit to 15 skills max. You may only add 5 interperited skills IF there is a explicit skills section and there should be a couple soft skills inlcuded. For any properties that are not available in the resume, please just respond with N/A.",
        tools=[
            {
                "type": "file_search",
                "vector_store_ids": [vs_id]
            }
        ],
        text={
            "format": {
                "type": "json_schema",
                "name": "job_description_parser_schema",
                "schema": {
                    "type": "object",
                    "properties": {
                        "Job Category": {
                            "type": "string",
                            "description": "Generic Job Category(e.g. Developer, Engineer, Project Manager, etc.)"
                        },
                        "Location": {
                            "type": "string",
                            "description": "Extract the location of the job in this format: City, State. If outside the U.S., mention country as well."
                        },
                        "Job Description": {
                            "type": "string",
                            "description": "Provide a 2-3 sentence summary of the job description"
                        },
                        "skills": {
                            "type": "array",
                            "items": {"type":"string"},
                            "description": "Comma-separated list of skills. Interperit these if not listed explicitly in the file, if already listed, no need to interperit."
                        },
                        "Requirements": {
                            "type": "array",
                            "items": {"type":"string"},
                            "description": "List requirements found within the file. Interperit these even if not listed explicitly in the file"
                        },
                        "Responsibilities": {
                            "type": "array",
                            "items": {"type":"string"},
                            "description": "List responsibilities of the job description. Interperit these even if not listed explicitly in the file"
                        },
                    },
                    "required": [
                        "Job Category",
                        "Location",
                        "Job Description",
                        "skills",
                        "Requirements",
                        "Responsibilities"
                    ],
                    "additionalProperties": False
                }
            }
        }

    )
    # 4) Pull out the arguments and print
    job_description_parser_schema = json.loads(response.output_text)
    print(json.dumps(job_description_parser_schema))

def _convert_txt_to_pdf(txt_path: Path, pdf_path: Path) -> None:
    """
    Helper: render each line of the .txt onto a LETTER page in pdf_path.
    """
    c = canvas.Canvas(str(pdf_path), pagesize=LETTER)
    width, height = LETTER
    margin, line_height = 72, 12
    y = height - margin

    with txt_path.open("r", encoding="utf-8") as f:
        for line in f:
            if y < margin:
                c.showPage()
                y = height - margin
            c.drawString(margin, y, line.rstrip("\n"))
            y -= line_height

    c.save()

def convert_to_pdf_general(src: Path) -> Path:
    """
    Public: ensure you end up with a PDF.
    - .docx → .pdf
    - .txt  → .pdf
    - .pdf  → no‑op
    """
    src = src.expanduser().resolve()
    suffix = src.suffix.lower()
    dst = src.with_suffix(".pdf")

    if suffix == ".docx":
        _convert_docx(str(src), str(dst))
        return dst

    if suffix == ".txt":
        _convert_txt_to_pdf(src, dst)
        return dst

    if suffix == ".pdf":
        return src

    raise ValueError(f"Unsupported extension '{src.suffix}'; only .docx, .txt or .pdf are allowed")

def main():
    if len(sys.argv) != 2:
        print("Usage: ResumeParser.py <path-to-resume.pdf|.docx>", file=sys.stderr)
        sys.exit(1)
    src = Path(sys.argv[1])
    openAIParser(src)

# ---------- CLI --------------------------------------------------------
if __name__ == "__main__":
    main()

