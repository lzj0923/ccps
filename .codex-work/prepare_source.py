from pathlib import Path
from shutil import copy2


work_dir = Path(__file__).parent
candidates = [path for path in work_dir.glob("*.docx") if path.name != "source.docx"]
source = max(candidates, key=lambda candidate: candidate.stat().st_size)
copy2(source, work_dir / "source.docx")
print(source)
