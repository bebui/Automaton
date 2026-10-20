#!/usr/bin/env python3
"""Compare published-API fixtures and measure the new snapshot entry points."""
from pathlib import Path
import argparse,json,os,subprocess
ROOT=Path(__file__).resolve().parents[2]
AUDIT=Path(__file__).resolve().parent
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--baseline-jar',type=Path,required=True)
parser.add_argument('--candidate-only',action='store_true')
parser.add_argument('--candidate-label',default='current')
args=parser.parse_args()
java=str(Path(os.environ['JAVA_HOME'])/'bin/java')
fixtures=[
 ('traversal',r'audit.GraphBridgeBench.(legacyCoaccessible|graphUseful)$',[],8),
 ('import',r'audit.GraphBridgeBench.importScalar$',['-p','states=256','-p','degree=4'],1),
 ('boolean',r'audit.Baseline.(run|compiledRun|minimize)$',['-p','family=sparse-interval,dense-interval'],6),
]
versions=[] if args.candidate_only else [('base',args.baseline_jar.resolve())]
versions.append((args.candidate_label,ROOT/'benchmarks/target/benchmarks.jar'))
for version,jar in versions:
 for fixture,pattern,extra,count in fixtures:
  name=fixture+'-'+version
  cmd=[java,'-jar',str(jar),pattern,'-prof','gc','-foe','true','-rf','json','-rff',str(AUDIT/(name+'.json')),*extra]
  print('START',name,flush=True)
  with (AUDIT/(name+'.log')).open('w') as log:
   log.write('Command: '+repr(cmd)+'\n');log.flush()
   subprocess.run(cmd,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,check=True)
  rows=json.loads((AUDIT/(name+'.json')).read_text());assert len(rows)==count,(name,len(rows))
  assert all(len(r['primaryMetric']['rawData'])==2 and all(len(f)==5 for f in r['primaryMetric']['rawData']) for r in rows)
  print('DONE',name,count,flush=True)
name='snapshot-api'+('' if args.candidate_label=='current' else '-'+args.candidate_label)
cmd=[java,'-jar',str(ROOT/'benchmarks/target/benchmarks.jar'),r'audit.SnapshotApiBench.*','-prof','gc','-foe','true','-rf','json','-rff',str(AUDIT/(name+'.json'))]
print('START',name,flush=True)
with (AUDIT/(name+'.log')).open('w') as log:
 log.write('Command: '+repr(cmd)+'\n');log.flush()
 subprocess.run(cmd,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,check=True)
rows=json.loads((AUDIT/(name+'.json')).read_text());assert len(rows)==12,len(rows)
assert all(len(r['primaryMetric']['rawData'])==2 and all(len(f)==5 for f in r['primaryMetric']['rawData']) for r in rows)
print('DONE',name,12,flush=True)
