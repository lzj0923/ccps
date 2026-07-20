from pathlib import Path
from pypdf import PdfReader

pdf = PdfReader(Path(__file__).with_name("final.pdf"))
for index, page in enumerate(pdf.pages, start=1):
    print(f"===== PAGE {index} =====")
    print(page.extract_text())
