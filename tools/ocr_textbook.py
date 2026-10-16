"""Local, resumable OCR. Never sends course materials over the network."""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / '.tools/python-libs'))
import fitz
import numpy as np
from rapidocr_onnxruntime import RapidOCR

out = ROOT / 'build-content/textbook-ocr'
out.mkdir(parents=True, exist_ok=True)
doc = fitz.open(next((ROOT / '课件').glob('*.pdf')))
engine = RapidOCR(intra_op_num_threads=4, inter_op_num_threads=2)
for index, page in enumerate(doc):
    target = out / f'{index + 1:03}.json'
    if target.exists():
        continue
    pix = page.get_pixmap(matrix=fitz.Matrix(1.5, 1.5), alpha=False)
    rgb = np.frombuffer(pix.samples, np.uint8).reshape(pix.height, pix.width, 3)
    result, _ = engine(rgb[:, :, ::-1].copy())
    lines = [{'box': row[0], 'text': row[1], 'confidence': float(row[2])} for row in (result or [])]
    target.write_text(json.dumps({'page': index+1, 'lines': lines}, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f'page {index+1}/{len(doc)}: {len(lines)} lines', flush=True)
    if index < 6:
        print('\n'.join(x['text'] for x in lines), flush=True)
print('OCR complete', flush=True)
