#!/usr/bin/env python3
"""Recheck the product's noisy mean with longer iterations and reversed run order."""
from pathlib import Path
import argparse,json,os,subprocess
ROOT=Path(__file__).resolve().parents[2]
AUDIT=Path(__file__).resolve().parent
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--baseline-jar',type=Path,required=True)
args=parser.parse_args()
for version,jar in [('current',ROOT/'benchmarks/target/benchmarks.jar'),('base',args.baseline_jar.resolve())]:
    name='product-followup-'+version
    cmd=[str(Path(os.environ['JAVA_HOME'])/'bin/java'),'-jar',str(jar),r'audit.WeightedBench.product$',
         '-wi','5','-w','1s','-i','5','-r','1s','-f','2','-prof','gc','-foe','true',
         '-rf','json','-rff',str(AUDIT/(name+'.json'))]
    print('START',name,flush=True)
    with (AUDIT/(name+'.log')).open('w') as log:
        log.write('Command: '+repr(cmd)+'\n');log.flush()
        subprocess.run(cmd,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,check=True)
    rows=json.loads((AUDIT/(name+'.json')).read_text())
    assert len(rows)==1 and len(rows[0]['primaryMetric']['rawData'])==2
    print('DONE',name,flush=True)
