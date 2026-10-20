#!/usr/bin/env python3
"""Run the same targeted JMH fixtures against pre/post package-refactoring JARs."""
import argparse
import json
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
AUDIT = Path(__file__).resolve().parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--baseline-jar', type=Path, required=True)
args = parser.parse_args()
java = str(Path(os.environ['JAVA_HOME']) / 'bin/java')
fixtures = [
    ('multi', r'audit.MultiCompiledBench.(vectorBuffered|reweightVector|compileVector|scalarizeCompiled)$', [], 12),
    ('weighted', r'audit.WeightedBench.(construction|union|product)$', [], 3),
    ('scalar', r'audit.ScaleBench.recognizeCompiled$', ['-p', 'states=128', '-p', 'degree=2,16', '-p', 'width=1000000'], 2),
    ('boolean', r'audit.Baseline.(run|compiledRun|compile|minimize)$', ['-p', 'family=sparse-interval,dense-interval'], 8),
]
for version, jar in [('base', args.baseline_jar.resolve()), ('current', ROOT / 'benchmarks/target/benchmarks.jar')]:
    for fixture, pattern, extra, expected in fixtures:
        name = fixture + '-' + version
        cmd = [java, '-jar', str(jar), pattern, '-prof', 'gc', '-rf', 'json', '-rff', str(AUDIT / (name + '.json')), '-foe', 'true', *extra]
        print('START', name, flush=True)
        with (AUDIT / (name + '.log')).open('w') as log:
            log.write('Command: ' + repr(cmd) + '\n'); log.flush()
            subprocess.run(cmd, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)
        rows = json.loads((AUDIT / (name + '.json')).read_text())
        assert len(rows) == expected, (name, len(rows))
        assert all(len(row['primaryMetric']['rawData']) == 2 and all(len(fork) == 5 for fork in row['primaryMetric']['rawData']) for row in rows)
        print('DONE', name, len(rows), flush=True)
