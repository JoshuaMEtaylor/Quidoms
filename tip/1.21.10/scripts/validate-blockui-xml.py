#!/usr/bin/env python3
"""Fail if BlockUI XML under minecolonies/gui has duplicate attributes on any element."""
from pathlib import Path
import re, sys
from collections import Counter
from xml.sax import make_parser, SAXParseException
from xml.sax.handler import ContentHandler
from io import StringIO

ROOT = Path(sys.argv[1] if len(sys.argv) > 1 else 'src/main/resources/assets/minecolonies/gui')
attr_pair = re.compile(r'([a-zA-Z_:][\w:.-]*)\s*=\s*"[^"]*"')
paths = sorted(ROOT.rglob('*.xml'))
dup_errors, sax_errors = [], []
for p in paths:
    raw = p.read_text(errors='replace')
    text = re.sub(r'<!--.*?-->', '', raw, flags=re.DOTALL)
    for m in re.finditer(r'<([a-zA-Z][\w:-]*)((?:\s[^<>]*?)?)(/?)>', text, re.DOTALL):
        names = attr_pair.findall(m.group(2) or '')
        dups = sorted(n for n, k in Counter(names).items() if k > 1)
        if dups:
            line = text[:m.start()].count('\n') + 1
            dup_errors.append(f'{p}:{line}: duplicate {dups} on <{m.group(1)}>')
    sax_src = re.sub(r'(\s)text(\s|/|>)', r'\1text="true"\2', text)
    try:
        parser = make_parser()
        parser.setContentHandler(ContentHandler())
        parser.parse(StringIO(sax_src))
    except SAXParseException as e:
        sax_errors.append(f'{p}:{e.getLineNumber()}: {e.getMessage()}')
print(f'Scanned {len(paths)} files under {ROOT}')
for e in dup_errors:
    print(e)
if dup_errors:
    sys.exit(1)
for e in sax_errors:
    print('SAX:', e)
print('VALIDATE_OK')
