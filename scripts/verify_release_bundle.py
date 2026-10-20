#!/usr/bin/env python3
"""Check this project's Central bundle, including detached GPG signatures."""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import subprocess
import tempfile
import xml.etree.ElementTree as ET
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
NS = {'m': 'http://maven.apache.org/POM/4.0.0'}


def check(condition, message):
    if not condition:
        raise ValueError(message)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('bundle', type=Path, nargs='?',
                        default=ROOT / 'target/central-publishing/central-bundle.zip')
    parser.add_argument('--expected-signer', help='full public signing-key fingerprint')
    parser.add_argument('--report', type=Path, help='write a JSON verification report')
    args = parser.parse_args()
    project = ET.parse(ROOT / 'pom.xml').getroot()
    coordinates = [project.findtext('m:' + name, namespaces=NS)
                   for name in ('groupId', 'artifactId', 'version')]
    group, artifact, version = coordinates
    check(all(coordinates) and not version.endswith('-SNAPSHOT'), 'Release coordinates required')
    directory = group.replace('.', '/') + '/' + artifact + '/' + version + '/'
    base = artifact + '-' + version
    primary = [base + suffix for suffix in ('.pom', '.jar', '-sources.jar', '-javadoc.jar')]
    digests = ('md5', 'sha1', 'sha256', 'sha512')
    with ZipFile(args.bundle) as bundle:
        names = [entry.filename for entry in bundle.infolist() if not entry.is_dir()]
        check(len(names) == len(set(names)), 'Duplicate archive entries')
        allowed = {directory + name + suffix for name in primary
                   for suffix in ('', '.asc', *('.' + d for d in digests),
                                  *('.asc.' + d for d in digests))}
        check(set(names) <= allowed, 'Unexpected files or coordinates in bundle')
        payload = {name: bundle.read(name) for name in names}
    for name in primary:
        for suffix in ('', '.asc', '.md5', '.sha1'):
            check(directory + name + suffix in payload, 'Missing ' + name + suffix)
    digest_count = 0
    for name, data in payload.items():
        algorithm = name.rsplit('.', 1)[-1]
        if algorithm in digests:
            original = name.rsplit('.', 1)[0]
            check(original in payload, 'Checksum without payload: ' + name)
            expected = hashlib.new(algorithm, payload[original]).hexdigest()
            check(data.decode('ascii').strip().lower() == expected, 'Invalid checksum: ' + name)
            digest_count += 1
    pom = ET.fromstring(payload[directory + base + '.pom'])
    check([pom.findtext('m:' + n, namespaces=NS) for n in ('groupId', 'artifactId', 'version')]
          == coordinates, 'POM coordinate mismatch')
    for field in ('name', 'description', 'url', 'licenses/license/name',
                  'licenses/license/url', 'developers/developer/name',
                  'scm/connection', 'scm/developerConnection', 'scm/url'):
        path = '/'.join('m:' + segment for segment in field.split('/'))
        check(bool((pom.findtext(path, namespaces=NS) or '').strip()), 'Missing POM metadata: ' + field)
    check(not any(d.findtext('m:scope', namespaces=NS) != 'test'
                  for d in pom.findall('m:dependencies/m:dependency', NS)),
          'Unexpected production dependency')
    signers = set()
    artifact_hashes = {}
    with tempfile.TemporaryDirectory(prefix='automaton-release-check-') as temp:
        for name in primary:
            path = Path(temp) / name
            path.write_bytes(payload[directory + name])
            signature = Path(str(path) + '.asc')
            signature.write_bytes(payload[directory + name + '.asc'])
            result = subprocess.run(['gpg', '--batch', '--no-auto-key-retrieve', '--status-fd', '1',
                                     '--verify', str(signature), str(path)], capture_output=True, text=True)
            # VALIDSIG reports the signing subkey first and, when present, its
            # primary-key fingerprint last. Match the public primary identity.
            valid = [fields[11] if len(fields) > 11 else fields[2]
                     for line in result.stdout.splitlines()
                     if line.startswith('[GNUPG:] VALIDSIG ')
                     for fields in [line.split()]]
            invalid = any(line.split()[1] in ('BADSIG', 'ERRSIG', 'EXPSIG', 'EXPKEYSIG', 'REVKEYSIG')
                          for line in result.stdout.splitlines() if line.startswith('[GNUPG:] '))
            check(result.returncode == 0 and len(valid) == 1 and not invalid, 'Invalid GPG signature: ' + name)
            signers.add(valid[0])
            artifact_hashes[name] = hashlib.sha256(path.read_bytes()).hexdigest()
        with ZipFile(Path(temp) / (base + '.jar')) as jar:
            entries = set(jar.namelist())
            for required in ('fr/menana/automaton/Automaton.class',
                             'fr/menana/automaton/weighted/WeightedAutomaton.class',
                             'fr/menana/automaton/weighted/MultiWeightedAutomaton.class',
                             'fr/menana/automaton/weighted/WeightedDfaIndex.class',
                             'fr/menana/automaton/weighted/compiled/CompiledWeightedDfa.class',
                             'fr/menana/automaton/weighted/compiled/CompiledMultiWeightedDfa.class'):
                check(required in entries, 'Missing library class: ' + required)
                magic, minor, major = struct.unpack('>IHH', jar.read(required)[:8])
                check(magic == 0xCAFEBABE and major == 61, 'Expected Java 17 bytecode: ' + required)
            check(not any(n.startswith(('audit/', 'org/openjdk/jmh/', 'org/junit/')) for n in entries),
                  'Development dependencies in main JAR')
        with ZipFile(Path(temp) / (base + '-sources.jar')) as jar:
            check('fr/menana/automaton/weighted/WeightedAutomaton.java' in jar.namelist(),
                  'Weighted sources absent')
        with ZipFile(Path(temp) / (base + '-javadoc.jar')) as jar:
            check('index.html' in jar.namelist(), 'Javadoc index absent')
            check('fr/menana/automaton/weighted/compiled/CompiledMultiWeightedDfa.html'
                  in jar.namelist(), 'Compiled-vector Javadoc absent')
    check(len(signers) == 1, 'Artifacts signed by different keys')
    if args.expected_signer:
        check(signers == {args.expected_signer.replace(' ', '').upper()}, 'Unexpected signing key')
    report = {'coordinates': ':'.join(coordinates), 'bundle': str(args.bundle.resolve()),
              'bundle_sha256': hashlib.sha256(args.bundle.read_bytes()).hexdigest(),
              'signed_artifacts': len(primary), 'checksums_verified': digest_count,
              'signing_fingerprints': sorted(signers), 'artifact_sha256': artifact_hashes,
              'local_validation': 'passed', 'central_server_validation': 'not performed'}
    text = json.dumps(report, indent=2) + '\n'
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(text)
    print(text, end='')


if __name__ == '__main__':
    main()
