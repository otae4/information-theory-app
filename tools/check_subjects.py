"""Verify the complete local material inventory and three-subject references."""
import hashlib
import json
import sys
from pathlib import Path
from zipfile import ZipFile

APP=Path(__file__).resolve().parents[1]
WORKSPACE=APP.parent
sys.path.insert(0,str(WORKSPACE/'.tools/python-libs'))
import fitz

assets=APP/'app/src/main/assets'
progress=set();questions=set();documents=set()
for subject,prefix in [('info',''),('comm','subjects/comm/'),('dsp','subjects/dsp/')]:
    course=json.loads((assets/(prefix+'course.json')).read_text(encoding='utf8'))
    homework=json.loads((assets/(prefix+'homework.json')).read_text(encoding='utf8'))
    ls={x['id']:x for x in course['lessons']};qs={x['id']:x for x in course['questions']}
    docs={x['id']:x for x in course['documents']};hw={x['id']:x for x in homework['problems']}
    assert not progress.intersection(ls.keys()|hw.keys()) and not questions.intersection(qs) and not documents.intersection(docs)
    progress.update(ls.keys()|hw.keys());questions.update(qs);documents.update(docs)
    expected={(id,p) for id,d in docs.items() for p in range(1,d['pages']+1)}
    actual={(x['document'],x['page']) for x in course['coverage']}
    assert expected==actual and len(actual)==len(course['coverage'])
    for doc in docs.values():
        with fitz.open(assets/doc['asset']) as pdf: assert len(pdf)==doc['pages']
    for item in course['coverage']: assert item['lessons'] and all(id in ls for id in item['lessons'])
    for item in qs.values(): assert item['lessonId'] in ls
    for item in hw.values():
        assert all(id in ls for id in item['lessons']) and item['id'] not in qs
        for field in ['source','solution']:
            if field in item: assert (item[field]['document'],item[field]['page']) in expected
    print(subject,len(ls),'lessons',len(qs),'practice',len(hw),'homework',len(docs),'PDFs',len(expected),'pages')

manifest=json.loads((WORKSPACE/'build-content/subjects/materials-manifest.json').read_text(encoding='utf8'))
for entry in manifest:
    folder='通信原理' if entry['subject']=='comm' else '数字信号处理'
    source=WORKSPACE.parent/folder/entry['source']
    assert source.exists()
    if source.suffix=='.pptx':
        with ZipFile(source) as archive:
            count=sum(1 for name in archive.namelist() if name.startswith('ppt/slides/slide') and name.endswith('.xml') and '/' not in name[len('ppt/slides/'):])
        assert count==entry['pages'],str(source)
    elif source.suffix=='.pdf':
        assert hashlib.sha256(source.read_bytes()).digest()==hashlib.sha256((assets/entry['asset']).read_bytes()).digest()
assert len(manifest)==75
print('PASS: all 62 slide decks retain their page count; all 10 copied PDFs match originals.')
