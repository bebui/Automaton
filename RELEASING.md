# Preparing and publishing a Maven Central release

The published release is `fr.menana:automaton:4.1.0`, with source tag `v4.1.0`.
Before preparing another release, select and review a new non-SNAPSHOT POM
version. Never redeploy the published version. The verifier rejects development snapshots.

Release preparation creates a signed Central bundle locally. Uploading and publishing are subsequent
actions. A normal library build needs neither signing keys nor Central credentials.

## Requirements

Use Java 17 and Maven 3. Check `java -version` and `mvn -version`; set `JAVA_HOME`
and put its `bin` directory first on `PATH` if several JDKs are installed.
Python 3 and GnuPG are needed for the local verification command below.

Signing uses your existing GPG key through `gpg-agent`. Set
`AUTOMATON_SIGNING_KEY` to its **full public fingerprint** and make sure the key's
public part is available on a key server accepted by Central. For signing continuity,
the published 4.0 release used the same publisher key as the published 3.0.0 artifacts.
Do not put private keys, passwords or tokens in this repository or command-line
properties. Maven batch mode uses the agent's cached passphrase; unlock the key
locally first if necessary.

Uploading requires the Portal user-token credentials in `~/.m2/settings.xml`,
under server ID `central`, and permission to publish the `fr.menana` namespace.
Credential values are not required for the local bundle-only mode.

## Create the signed bundle without uploading

From the reviewed source checkout:

```bash
mvn -B -P release -Dcentral.skipUpload=true \
  "-Dgpg.keyname=$AUTOMATON_SIGNING_KEY" clean deploy
python3 scripts/verify_release_bundle.py \
  --expected-signer "$AUTOMATON_SIGNING_KEY" \
  --report target/release-validation.json
python3 docs/check_examples.py
```

The release profile runs the tests, builds the binary/source/Javadoc JARs and
signs those three files and the POM. The Central plugin generates the Maven-layout
ZIP and its checksums. `central.skipUpload=true` stops before upload; it is wired
to the plugin's `skipUpload` option, available starting in version 0.12.0.
`skipPublishing` is different: it excludes artifacts and does not prepare a bundle.

The `deploy` lifecycle also installs the artifacts into your **local** Maven
repository. This does not publish them remotely. Outputs are ignored by Git:

| File | Purpose |
|---|---|
| `target/central-publishing/central-bundle.zip` | Signed bundle for Portal upload |
| `target/release-validation.json` | Local signature, checksum, metadata and JAR-content checks |
| `target/automaton-VERSION.jar` | Java 17 binary library |
| `target/automaton-VERSION-sources.jar` | Source attachment |
| `target/automaton-VERSION-javadoc.jar` | API documentation attachment |

Archive the verified ZIP and report outside `target` before another clean build:
`mvn clean` removes the preparation outputs. Keep the bundle SHA-256 with that copy.

The verifier checks all provided checksums and four detached signatures using
the local public keyring, without automatic key retrieval. It also checks release
coordinates, required POM metadata, the absence of production dependencies,
the weighted/compiled classes and their Java 17 bytecode, sources and Javadoc.
It does not authenticate to Central or replace Central's server-side validation.

## Upload for Central validation

After reviewing the local report, upload the **verified ZIP** from the
[Central Portal deployments page](https://central.sonatype.com/publishing/deployments).
Wait for server validation and inspect any reported errors. The same ZIP can be
reviewed and uploaded without rebuilding or changing its signatures.

Alternatively, to rebuild, sign and upload through Maven:

```bash
mvn -B -P release -Dcentral.skipUpload=false \
  "-Dgpg.keyname=$AUTOMATON_SIGNING_KEY" clean deploy
```

`autoPublish` remains `false`: this uploads for validation and leaves publication
to the Portal's explicit **Publish** action. An upload ID or successful local
build does not mean the artifact is publicly available.

## Final publication and release records

Publish the validated deployment in the Portal when the release is approved.
Tag the reviewed release source with its version tag (for example `v4.1.0`) and record which source commit and
bundle SHA-256 were published. Push the tag as part of that release action.
Confirm the POM and three JARs can be resolved from Maven Central before changing
the README, changelog and installation guides from “prepared” to “published”.
Releases on Central are immutable; a correction requires a new version.

Official references: [Central Maven plugin and bundle-only mode](https://central.sonatype.org/publish/publish-portal-maven/),
[Central artifact requirements](https://central.sonatype.org/publish/requirements/),
[Maven GPG signing](https://maven.apache.org/plugins/maven-gpg-plugin/sign-mojo.html).
