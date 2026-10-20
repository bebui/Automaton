#!/usr/bin/env python3
"""Compare compiled baseline/current APIs and bytecode; see README for inputs."""
from pathlib import Path
import argparse
import json
import os
import subprocess

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--baseline', type=Path, required=True, help='merged pre-refactoring classes')
parser.add_argument('--original', type=Path, required=True, help='original 3.0 classes')
args = parser.parse_args()
CURRENT = ROOT / 'target/classes'
BENCH = ROOT / 'benchmarks/target/classes'
JAVA = Path(os.environ['JAVA_HOME']) / 'bin/javap'

def names(directory):
    return [str(p.relative_to(directory).with_suffix('')).replace(os.sep, '.')
            for p in sorted(directory.rglob('*.class')) if p.name != 'package-info.class']

def relocated(name):
    package, _, outer = name.rpartition('.')
    outer = outer.split('$')[0]
    if package == 'fr.menana.automaton' and (ROOT / ('src/main/java/fr/menana/automaton/weighted/' + outer + '.java')).exists():
        return name.replace(package, package + '.weighted', 1)
    if outer in ('CompiledWeightedDfa', 'CompiledMultiWeightedDfa'):
        return name.replace(package, package + '.weighted.compiled', 1)
    if outer == 'WeightMath':
        return name.replace(package, package + '.weighted.internal', 1)
    return name

def normalize(text):
    for package in ('weighted.compiled.', 'weighted.internal.', 'weighted.'):
        text = text.replace('fr.menana.automaton.' + package, 'fr.menana.automaton.')
    return text

def public_signatures(directory, types):
    output = subprocess.check_output([str(JAVA), '-public', '-classpath', str(directory), *types], text=True)
    parts = output.split('Compiled from ')[1:]
    assert len(parts) == len(types)
    result = []
    for part in parts:
        lines = part.splitlines()[1:]
        if lines and lines[0].startswith('public '):
            result.append({normalize(line.strip()) for line in lines if line.strip().startswith('public ')})
        else:
            result.append(set())
    return result

previous = names(args.baseline)
old = public_signatures(args.baseline, previous)
new = public_signatures(CURRENT, [relocated(name) for name in previous])
missing = {name: sorted(a - b) for name, a, b in zip(previous, old, new) if a - b}
assert not missing, missing
original = names(args.original)
original_api = public_signatures(args.original, original)
current_api = public_signatures(CURRENT, original)
removed_original = {name: sorted(a - b) for name, a, b in zip(original, original_api, current_api) if a - b}
assert not removed_original, removed_original
identical, changed = [], []
for p in sorted(args.original.rglob('*.class')):
    rel = p.relative_to(args.original)
    (identical if p.read_bytes() == (CURRENT / rel).read_bytes() else changed).append(str(rel))
assert changed == ['fr/menana/automaton/Alphabet.class', 'fr/menana/automaton/IntervalSet.class'], changed
bench_count = 0
for p in sorted(CURRENT.rglob('*.class')):
    rel = p.relative_to(CURRENT)
    assert p.read_bytes() == (BENCH / rel).read_bytes(), str(rel)
    bench_count += 1
# Check original sources/tests apart from the documented additive changes and package Javadoc.
original_files = subprocess.check_output(['git', 'ls-tree', '-r', '--name-only', 'f753243', 'src/main/java', 'src/test/java'], cwd=ROOT, text=True).splitlines()
source_changes = [name for name in original_files if (ROOT / name).read_bytes() != subprocess.check_output(['git', 'show', 'f753243:' + name], cwd=ROOT)]
assert source_changes == ['src/main/java/fr/menana/automaton/Alphabet.java',
                          'src/main/java/fr/menana/automaton/IntervalSet.java',
                          'src/main/java/fr/menana/automaton/package-info.java'], source_changes
benchmark_files = sorted((ROOT / 'benchmarks/src/main/java').rglob('*.java'))
import re
for p in benchmark_files:
    rel = str(p.relative_to(ROOT))
    before = subprocess.check_output(['git', 'show', 'd9c9225:' + rel], cwd=ROOT, text=True)
    without_imports = lambda text: re.sub(r'^import fr\.menana\.automaton[^\n]*\n', '', text, flags=re.M)
    assert without_imports(before) == without_imports(p.read_text()), rel
result = {
    'baseline_commit': 'd9c9225', 'original_boolean_commit': 'f753243',
    'original_boolean_class_files_byte_identical': len(identical),
    'original_boolean_class_files_changed_by_additive_api': changed,
    'original_boolean_public_types_checked': sum(bool(s) for s in original_api),
    'original_boolean_removed_public_signatures': removed_original,
    'previous_public_types_including_nested_checked_after_relocation': sum(bool(s) for s in old),
    'previous_removed_public_signatures_after_relocation': missing,
    'original_source_and_test_changes': source_changes,
    'tested_and_benchmark_class_files_byte_identical': bench_count,
    'benchmark_source_files_identical_except_library_imports': len(benchmark_files),
}
(ROOT / 'audit/release-4/compatibility.json').write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps(result, indent=2))
