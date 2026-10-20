#!/usr/bin/env python3
"""Check additive 4.0 API compatibility and execute an unrecompiled 4.0 client."""
from pathlib import Path
import argparse,json,os,subprocess,tempfile
ROOT=Path(__file__).resolve().parents[2]
AUDIT=Path(__file__).resolve().parent
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--baseline-classes',type=Path,required=True)
args=parser.parse_args()
current=ROOT/'target/classes';baseline=args.baseline_classes.resolve()
java=Path(os.environ['JAVA_HOME'])/'bin'
names=[str(p.relative_to(baseline).with_suffix('')).replace(os.sep,'.') for p in sorted((baseline/'fr/menana/automaton').rglob('*.class')) if p.name!='package-info.class' and '/internal/' not in p.as_posix()]
def signatures(directory):
 output=subprocess.check_output([str(java/'javap'),'-public','-classpath',str(directory),*names],text=True)
 parts=output.split('Compiled from ')[1:];assert len(parts)==len(names)
 return {name:{line.strip() for line in part.splitlines()[1:] if line.strip().startswith('public ')} if part.splitlines()[1].startswith('public ') else set() for name,part in zip(names,parts)}
before,after=signatures(baseline),signatures(current)
missing={name:sorted(lines-after[name]) for name,lines in before.items() if lines-after[name]}
assert not missing,missing
matched=0
for p in current.rglob('*.class'):
 rel=p.relative_to(current);assert p.read_bytes()==(ROOT/'benchmarks/target/classes'/rel).read_bytes(),rel
 matched+=1
boolean_identical=[];boolean_changed=[]
for p in (baseline/'fr/menana/automaton').rglob('*.class'):
 rel=p.relative_to(baseline);outer=p.name.split('$')[0].removesuffix('.class')
 path=str(Path('src/main/java')/rel.parent/(outer+'.java'))
 if subprocess.run(['git','cat-file','-e','f753243:'+path],cwd=ROOT,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode==0:
  (boolean_identical if p.read_bytes()==(current/rel).read_bytes() else boolean_changed).append(str(rel))
assert boolean_changed==['fr/menana/automaton/Automaton.class'],boolean_changed
old_source=subprocess.check_output(['git','show','13d8ba6:src/main/java/fr/menana/automaton/Automaton.java'],cwd=ROOT,text=True)
new_source=(ROOT/'src/main/java/fr/menana/automaton/Automaton.java').read_text()
def body(text,signature):
 at=text.index(signature);start=text.index('{',at);depth=0
 for i in range(start,len(text)):
  if text[i]=='{':depth+=1
  if text[i]=='}':depth-=1
  if depth==0:return text[start:i+1]
assert body(old_source,'public boolean run(int... word)')==body(new_source,'public boolean run(int... word)')
for name in ['CompiledDfa','State','Transition']:
 old=subprocess.check_output(['git','show','13d8ba6:src/main/java/fr/menana/automaton/'+name+'.java'],cwd=ROOT)
 assert old==(ROOT/('src/main/java/fr/menana/automaton/'+name+'.java')).read_bytes(),name
with tempfile.TemporaryDirectory(prefix='automaton-legacy-client-') as temp:
 directory=Path(temp)
 source=subprocess.check_output(['git','show','13d8ba6:docs/examples/AutomatonTour.java'],cwd=ROOT)
 (directory/'AutomatonTour.java').write_bytes(source)
 subprocess.run([str(java/'javac'),'--release','17','-cp',str(baseline),'-d',temp,str(directory/'AutomatonTour.java')],check=True)
 output=subprocess.check_output([str(java/'java'),'-cp',temp+os.pathsep+str(current),'AutomatonTour'],text=True)
 (AUDIT/'legacy-client.log').write_text(output)
 assert 'passed: 11' in output,output
result={'baseline':'13d8ba6','previous_supported_public_types_checked':sum(bool(lines) for lines in before.values()),'removed_public_signatures':missing,'original_boolean_class_files_unchanged_from_4_0':len(boolean_identical),'original_boolean_class_files_changed':boolean_changed,'original_recognizer_method_body_unchanged':True,'compiled_dfa_state_transition_sources_unchanged':True,'tested_and_benchmark_class_files_byte_identical':matched,'unrecompiled_4_0_client_scenarios_passed':11}
(AUDIT/'compatibility.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2))
