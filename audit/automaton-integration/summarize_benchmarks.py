#!/usr/bin/env python3
"""Summarize final JMH results, preserving the initial adapter measurements too."""
from pathlib import Path
import json
AUDIT=Path(__file__).resolve().parent
lines=['# Automaton / weighted integration performance','',
 'Baseline: merged published-4.0 sources `13d8ba6`. Candidate: 4.1 snapshot bridge',
 'and shared primitive reachability, including the verified-index legacy adapter.','',
 'JDK 17, JMH 1.37, average microseconds/op, two forks, three 500 ms warmup and',
 'five 500 ms measurement iterations per fork, GC profiler and 128–256 MB heaps.',
 'Numbers are mean ± JMH-reported error; ratio is candidate/baseline, a descriptive',
 'comparison rather than a significance test. This is a shared host. Wide errors',
 'are retained; no universal speedup or workload guarantee is inferred. Builds',
 'and tests did not run concurrently with benchmark measurements.','']
all_rows=[]
for fixture in ['traversal','import','boolean']:
 def read(version):
  rows=json.loads((AUDIT/(fixture+'-'+version+'.json')).read_text())
  return {(r['benchmark'],tuple(sorted(r.get('params',{}).items()))):r for r in rows}
 before,after=read('base'),read('final');assert before.keys()==after.keys()
 lines+=['## '+fixture.capitalize(),'',f'[Baseline JSON]({fixture}-base.json), [final JSON]({fixture}-final.json),',f'[baseline log]({fixture}-base.log), [final log]({fixture}-final.log).','',
 '| Method / parameters | Baseline µs/op | Final µs/op | Ratio | Bytes/op before → final |','|---|---:|---:|---:|---:|']
 for key in sorted(before):
  b,c=before[key],after[key];bm,cm=b['primaryMetric'],c['primaryMetric'];alloc=lambda r:r['secondaryMetrics']['gc.alloc.rate.norm']['score']
  label='`'+key[0].split('.')[-1]+'`; '+', '.join(k+'='+v for k,v in key[1])
  lines.append(f"| {label} | {bm['score']:.4g} ± {bm['scoreError']:.2g} | {cm['score']:.4g} ± {cm['scoreError']:.2g} | {cm['score']/bm['score']:.3f} | {alloc(b):.3f} → {alloc(c):.3f} |")
  all_rows.append((key,b,c))
 lines.append('')
lines+=['## Snapshot APIs','',
 '[Final raw JSON](snapshot-api-final.json), [log](snapshot-api-final.log).',
 'A 256-state, degree-4 fixture has two disconnected components and wide symbolic',
 'intervals. The NFA adds one epsilon edge. Scalars use cost 2, vectors costs 2/7,',
 'and membership reads an existing 64-symbol array. Snapshot reuse avoids copying',
 'topology while still capturing independent cost arrays. Projection is a fresh',
 'mutable Boolean automaton and intentionally forgets costs.','',
 '| Method / family | µs/op | Bytes/op |','|---|---:|---:|']
for r in json.loads((AUDIT/'snapshot-api-final.json').read_text()):
 m=r['primaryMetric'];a=r['secondaryMetrics']['gc.alloc.rate.norm']
 lines.append(f"| `{r['benchmark'].split('.')[-1]}`; {r['params']['family']} | {m['score']:.4g} ± {m['scoreError']:.2g} | {a['score']:.3f} |")
lines+=['','## Initial adapter and interpretation','',
 'The first adapter used identity-map lookups for every arc even when existing',
 'state indices were canonical. In the largest dense fixture its mean was 18.44 ms',
 'against 15.85 ms baseline, with overlapping errors. The final adapter verifies',
 'indices once and uses them directly, retaining the identity-map path for',
 'reordered/noncanonical state lists. The final mean there is 10.77 ms with',
 'about 512 KB/op against 1,526 KB/op baseline. Allocations drop roughly 61–66%',
 'across the four legacy coaccessibility fixtures. Wide baseline errors prevent',
 'precise speedup conclusions in every case. Immutable useful-state buffers retain',
 'their prior allocation shape; old recognition/compiler bodies are unchanged.','',
 'Initial raw evidence: [traversal JSON](traversal-current.json), [log](traversal-current.log),',
 '[import JSON](import-current.json), [log](import-current.log),',
 '[Boolean JSON](boolean-current.json), [log](boolean-current.log),',
 '[snapshot API JSON](snapshot-api.json), [log](snapshot-api.log).',
 'Final measurements cover 15 baseline cases, 15 final comparison cases and 12',
 'final new-API cases. Including 27 intermediate candidate cases, 69 combinations',
 'were measured before the four focused rechecks below. No initial rows are',
 'silently substituted for final measurements.','',
 '## Focused Boolean recheck','',
 'The main campaign showed a 22% higher sparse-recognition mean and a very noisy',
 'sparse-minimization mean. Both cases were measured again, baseline then candidate,',
 'with unchanged JMH settings. The original recognizer body and compiled evaluator',
 'are unchanged; the recheck does not reproduce either slowdown. The main campaign',
 'is retained above, rather than replaced by these later samples. This shared-host',
 'evidence does not establish a persistent recognition/minimization regression.','',
 '[Baseline JSON](boolean-recheck-base.json), [candidate JSON](boolean-recheck-final.json),',
 '[baseline log](boolean-recheck-base.log), [candidate log](boolean-recheck-final.log).','',
 '| Method / sparse-interval | Baseline µs/op | Candidate µs/op | Ratio |',
 '|---|---:|---:|---:|']
rechecks=[]
for version in ['base','final']:
 rechecks.append({r['benchmark']:r['primaryMetric'] for r in json.loads((AUDIT/('boolean-recheck-'+version+'.json')).read_text())})
for name in sorted(rechecks[0]):
 b,c=rechecks[0][name],rechecks[1][name]
 lines.append(f"| `{name.split('.')[-1]}` | {b['score']:.4g} ± {b['scoreError']:.2g} | {c['score']:.4g} ± {c['scoreError']:.2g} | {c['score']/b['score']:.3f} |")
lines+=['','The complete evidence contains 73 measured combinations including the four rechecks.','',
 '[Integration report](README.md) · [Runner](run_benchmarks.py)','']
(AUDIT/'PERFORMANCE.md').write_text('\n'.join(lines))
print('Compared',len(all_rows),'published-API cases per version')
for key,b,c in all_rows:
 bm,cm=b['primaryMetric'],c['primaryMetric']
 if cm['score']-cm['scoreError']>bm['score']+bm['scoreError']:
  print('Higher timing outside reported intervals:',key,cm['score']/bm['score'])
