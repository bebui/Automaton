#!/usr/bin/env python3
"""Regenerate the complete public signature index from this checkout's classes."""
from pathlib import Path
import os
import re
import shutil
import subprocess

ROOT = Path(__file__).resolve().parent.parent
CLASSES = ROOT / 'target/classes'
JAVA_HOME = os.environ.get('JAVA_HOME')
JAVAP = str(Path(JAVA_HOME) / 'bin/javap') if JAVA_HOME else shutil.which('javap')

def generate():
    names = [str(p.relative_to(CLASSES).with_suffix('')).replace(os.sep, '.')
             for p in sorted(CLASSES.rglob('*.class')) if p.name != 'package-info.class' and '/internal/' not in p.as_posix()]
    if not names:
        raise SystemExit('Build the library first with Maven and JDK 17.')
    text = subprocess.check_output([JAVAP, '-public', '-classpath', str(CLASSES), *names], text=True)
    parts = text.split('Compiled from ')[1:]
    assert len(parts) == len(names)
    entries = []
    for name, part in zip(names, parts):
        lines = part.splitlines()[1:]
        if not lines or not lines[0].startswith('public '):
            continue
        if re.search(r'\$\d', name):
            continue
        outer = name.split('$', 1)[0]
        source = Path('src/main/java') / (outer.replace('.', '/') + '.java')
        assert (ROOT / source).exists(), source
        short = name.removeprefix('fr.menana.automaton.').replace('$', '.')
        signature = '\n'.join(lines)
        for package in ('fr.menana.automaton.regexp.', 'fr.menana.automaton.', 'java.lang.', 'java.util.', 'java.math.'):
            signature = signature.replace(package, '')
        signature = signature.replace('$', '.')
        entries.append((short, source, signature))
    lines = ['# Complete public API reference', '',
             'Generated from the compiled Java 17 classes in this checkout. Regenerate with',
             '`python3 docs/generate_api_reference.py` after building the library.',
             'This lists declared public constructors, methods, overloads and constants,',
             'including public nested types. Unsupported `internal` implementation helpers are excluded.',
             'Inherited `Object` methods are not repeated.',
             'Java/JDK and library package prefixes are shortened for readability.',
             'Parameter contracts, return/absence conventions and algorithm preconditions',
             'are explained in the [topic guides](README.md) and source Javadoc.', '',
             f'**{len(entries)} public types.**', '', '| Type | Source |', '|---|---|']
    for short, source, signature in entries:
        anchor=short.replace('.', '-').lower()
        lines.append(f'| [{short}](#{anchor}) | [{source.name}](../{source}) |')
    for short, source, signature in entries:
        anchor=short.replace('.', '-').lower()
        lines += ['', f'<a id="{anchor}"></a>', '', f'## `{short}`', '',
                  f'[Source and Javadoc](../{source})', '', '```java', signature, '```']
    lines += ['', '[Documentation index](README.md)']
    destination = ROOT / 'docs/API_REFERENCE.md'
    destination.write_text('\n'.join(lines) + '\n')
    print(f'Generated {len(entries)} public types in {destination.relative_to(ROOT)}')

if __name__ == '__main__':
    generate()
