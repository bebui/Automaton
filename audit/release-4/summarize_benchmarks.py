#!/usr/bin/env python3
"""Generate a compact comparison while retaining all raw JMH samples/errors."""
from pathlib import Path
import json

AUDIT = Path(__file__).resolve().parent
fixtures = ['multi', 'weighted', 'scalar', 'boolean']
lines = [
    '# Package-refactoring performance comparison', '',
    'Baseline: merged PR #8 (`d9c9225`). Candidate: 4.0.0 package organization and',
    'the extracted weight-independent interval index. Both snapshots use the same',
    'fixture/method bodies, with only weighted package imports adapted.', '',
    'JDK 17, JMH 1.37, average microseconds/op, two forks, three 500 ms warmup and',
    'five 500 ms measurement iterations per fork, GC profiler, 128–256 MB heaps.',
    'Each row reports the JMH mean ± reported error. Ratio is candidate/baseline;',
    'it is descriptive, not a significance test. Allocations are profiler bytes/op.',
    'A small fractional allocation on recognition reflects profiler/background',
    'overhead; these APIs do not allocate work/result arrays for existing inputs.', '',
    'The machine is shared. No build or other campaign was run concurrently by this',
    'task. Overlapping uncertainty does not establish a speedup or regression.',
    'These targeted 25 cases per version assess this refactoring; earlier algorithm',
    'benchmarks remain historical evidence rather than a repeated complete campaign.', '',
]
rows_all = []
for fixture in fixtures:
    def read(version):
        data = json.loads((AUDIT / (fixture + '-' + version + '.json')).read_text())
        return {(r['benchmark'], tuple(sorted(r.get('params', {}).items()))): r for r in data}
    before, after = read('base'), read('current')
    assert before.keys() == after.keys()
    lines += ['## ' + fixture.capitalize(), '',
              f'[Baseline JSON]({fixture}-base.json), [candidate JSON]({fixture}-current.json),',
              f'[baseline log]({fixture}-base.log), [candidate log]({fixture}-current.log).', '',
              '| Method / parameters | Baseline µs/op | 4.0 µs/op | Ratio | Bytes/op before → 4.0 |',
              '|---|---:|---:|---:|---:|']
    for key in sorted(before):
        b, a = before[key], after[key]
        bm, am = b['primaryMetric'], a['primaryMetric']
        name = key[0].split('.')[-1]
        params = ', '.join(k + '=' + v for k, v in key[1])
        label = '`' + name + '`' + ('; ' + params if params else '')
        alloc = lambda r: r['secondaryMetrics']['gc.alloc.rate.norm']['score']
        lines.append(f"| {label} | {bm['score']:.4g} ± {bm['scoreError']:.2g} | {am['score']:.4g} ± {am['scoreError']:.2g} | {am['score']/bm['score']:.3f} | {alloc(b):.3f} → {alloc(a):.3f} |")
        rows_all.append((key, b, a))
    lines.append('')
followup = [json.loads((AUDIT / ('product-followup-' + v + '.json')).read_text())[0] for v in ['base', 'current']]
b, a = followup
bm, am = b['primaryMetric'], a['primaryMetric']
alloc = lambda r: r['secondaryMetrics']['gc.alloc.rate.norm']['score']
lines += ['## Focused product follow-up', '',
          'The initial product means differed by about 15%, with overlapping error',
          'intervals. A follow-up reverses the run order (candidate, then baseline)',
          'and uses two forks, five 1 s warmups and five 1 s measurements per fork.',
          'It finds essentially equal mean times and allocations after longer warmup:', '',
          '| Method | Baseline µs/op | 4.0 µs/op | Ratio | Bytes/op before → 4.0 |',
          '|---|---:|---:|---:|---:|',
          f"| `product` | {bm['score']:.4g} ± {bm['scoreError']:.2g} | {am['score']:.4g} ± {am['scoreError']:.2g} | {am['score']/bm['score']:.3f} | {alloc(b):.3f} → {alloc(a):.3f} |", '',
          '[Baseline JSON](product-followup-base.json), [candidate JSON](product-followup-current.json),',
          '[baseline log](product-followup-base.log), [candidate log](product-followup-current.log),',
          '[follow-up runner](run_product_followup.py). These longer measurements do',
          'not confirm the initial apparent slowdown; the original rows remain above.', '',
          '## Interpretation', '',
          'The extraction primarily changes setup ownership and reuse: vector reweighting',
          'creates only a small evaluator instead of a scalar projection and scalar',
          'compiled wrapper. Shared-index scalarization also avoids rebuilding lookup',
          'arrays; graph and vector stores remain shared. Compare allocation columns',
          'for these setup operations separately from recognition timing.', '',
          'Original Boolean recognition/compiler algorithm bodies are unchanged; the',
          'Boolean rows measure non-regression on sparse/dense interval fixtures.',
          'Interval copying is exercised by weighted construction, union and product.',
          'Scalar recognition uses two outgoing degrees, and direct vector evaluation',
          'covers 2, 8 and 32 dimensions. No broader workload guarantee is inferred.', '',
          '[Refactoring report](README.md) · [Environment](environment.json) · [Runner](run_benchmarks.py)', '']
(AUDIT / 'PERFORMANCE.md').write_text('\n'.join(lines))
print('Compared',len(rows_all),'cases per version')
for key,b,a in rows_all:
    bm,am=b['primaryMetric'],a['primaryMetric']
    if am['score']-am['scoreError'] > bm['score']+bm['scoreError']:
        print('Higher candidate timing outside reported intervals:',key,am['score']/bm['score'])
