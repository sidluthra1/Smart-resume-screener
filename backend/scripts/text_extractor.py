import pdfplumber
import docx
from pathlib import Path


def extract_text(path: str) -> str:
    p = Path(path)
    if p.suffix.lower() == '.pdf':
        with pdfplumber.open(path) as pdf:
            return "\n".join(page.extract_text() or "" for page in pdf.pages)
    elif p.suffix.lower() in ('.docx',):
        doc = docx.Document(path)
        return "\n".join(para.text for para in doc.paragraphs)
    elif p.suffix.lower() == '.txt':
        return p.read_text(encoding='utf-8')
    else:
        raise ValueError(f"Unsupported file type: {p.suffix}")


if __name__ == "__main__":
    import sys
    import json

    text = extract_text(sys.argv[1])
    print(json.dumps({"text": text}))
