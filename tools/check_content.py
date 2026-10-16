"""Checks actual source counts, links, question answers, math examples and TeX rendering inputs."""
import json
import math
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'.tools/python-libs'))
import fitz

assets=ROOT/'InformationTheoryApp/app/src/main/assets'
c=json.loads((assets/'course.json').read_text(encoding='utf8'))
lessons={x['id']:x for x in c['lessons']}
questions={x['id']:x for x in c['questions']}
docs={x['id']:x for x in c['documents']}
assert len(lessons)==len(c['lessons']) and len(questions)==len(c['questions'])
assert len(c['chapters'])==9 and len(docs)==6
expected={(id,page) for id,d in docs.items() for page in range(1,d['pages']+1)}
actual={(x['document'],x['page']) for x in c['coverage']}
assert actual==expected and len(actual)==len(c['coverage'])
for d in docs.values():
    pdf=fitz.open(assets/d['asset'])
    assert len(pdf)==d['pages'],d['id']
for x in c['coverage']:
    assert x['lessons'] and all(id in lessons for id in x['lessons'])
for lesson in lessons.values():
    assert all(id in lessons for id in lesson['prerequisites'])
    assert lesson['id'] not in lesson['prerequisites']
    assert len([q for q in questions.values() if q['lessonId']==lesson['id']])>=2
    assert '完整例题' in lesson['html'] and '为什么成立' in lesson['html']
    assert lesson['html'].count(r'\(')==lesson['html'].count(r'\)')
    for s in lesson['sources']: assert (s['document'],s['page']) in expected
for q in questions.values():
    assert q['lessonId'] in lessons and q['explanation']
    assert q['kind'] in ['choice','number','self']
    if q['kind']=='choice': assert 0<=int(q['answer'])<len(q['options'])
    if q['kind']=='number': assert math.isfinite(float(q['answer'])) and q.get('tolerance',.001)>0
# Verify prerequisite graph is acyclic.
def visit(id,stack):
    assert id not in stack, f'prerequisite cycle: {id}'
    for p in lessons[id]['prerequisites']: visit(p,stack|{id})
for id in lessons: visit(id,set())
def h(p): return -sum(x*math.log2(x) for x in p if x)
assert abs(h([.5,.25,.125,.125])-1.75)<1e-12
assert abs(sum(p*l for p,l in zip([.0625,.1875,.1875,.5625],[3,3,2,1]))-1.6875)<1e-12
assert abs((2/3)*h([.9,.1])+(1/3)*h([.2,.8])-.5533)<1e-4
# GF(2) examples: (7,4), g=x^3+x+1.
def remainder(a,g):
    while a.bit_length()>=g.bit_length(): a ^= g << (a.bit_length()-g.bit_length())
    return a
assert remainder(0b10000001,0b1011)==0
assert remainder(0b1000,0b1011)==0b011
for message in range(16):
    shifted=message<<3
    code=shifted^remainder(shifted,0b1011)
    assert code>>3==message and remainder(code,0b1011)==0
single_syndromes={remainder(1<<j,0b1011) for j in range(7)}
assert len(single_syndromes)==7 and 0 not in single_syndromes
assert (assets/'mathjax/tex-svg-full.js').stat().st_size>1000000
print(f'PASS: {len(lessons)} lessons, {len(questions)} questions, {len(expected)} source pages, source files, prerequisite graph, numerical/coding examples, offline MathJax.')
