#!/usr/bin/env python3
"""Check that documented snippets match the executable tour, then compile/run it."""
from pathlib import Path
import os
import re
import shutil
import subprocess
import textwrap

ROOT = Path(__file__).resolve().parent.parent
DOCS = ROOT / 'docs'
SOURCE = DOCS / 'examples/AutomatonTour.java'

def regions():
    result, name, lines = {}, None, []
    for line in SOURCE.read_text().splitlines():
        if line.strip().startswith('// BEGIN '):
            name, lines = line.strip().removeprefix('// BEGIN '), []
        elif line.strip().startswith('// END '):
            assert line.strip() == '// END ' + name
            result[name] = textwrap.dedent('\n'.join(lines)).strip()
            name = None
        elif name:
            lines.append(line)
    return result

def java_tool(name):
    java_home = os.environ.get('JAVA_HOME')
    return str(Path(java_home) / 'bin' / name) if java_home else shutil.which(name)

if __name__ == '__main__':
    snippets, used = regions(), set()
    pattern = re.compile(r'<!-- example: (\w+) -->\s*```java\n(.*?)\n```', re.S)
    for page in DOCS.glob('*.md'):
        for name, body in pattern.findall(page.read_text()):
            assert body.strip() == snippets[name], f'Stale example {name} in {page}'
            used.add(name)
    assert used == snippets.keys(), ('Missing documented examples', snippets.keys() - used)
    destination = ROOT / 'target/documentation-examples'
    destination.mkdir(parents=True, exist_ok=True)
    subprocess.run([java_tool('javac'), '--release', '17', '-cp', str(ROOT / 'target/classes'),
                    '-d', str(destination), str(SOURCE)], check=True)
    subprocess.run([java_tool('java'), '-cp', os.pathsep.join([str(ROOT / 'target/classes'), str(destination)]),
                    'AutomatonTour'], check=True)
    print(f'{len(used)} documented snippets match and execute successfully.')
