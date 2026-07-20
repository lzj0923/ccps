from pathlib import Path
from docx import Document


work_dir = Path(__file__).parent
source = work_dir / "source.docx"
output_dir = work_dir / "source_images"
output_dir.mkdir(exist_ok=True)

doc = Document(source)
for index, shape in enumerate(doc.inline_shapes, start=1):
    blip = shape._inline.graphic.graphicData.pic.blipFill.blip
    part = doc.part.related_parts[blip.embed]
    suffix = Path(part.partname).suffix
    destination = output_dir / f"image-{index}{suffix}"
    destination.write_bytes(part.blob)
    print(f"{destination} {shape.width}x{shape.height}")
