#!/usr/bin/env bash
# Repackage dist jar with Quidoms META-INF (mods.toml / AT / pack.mcmeta / logo)
# and strip any accidental net.neoforged.* class packages / module-info (JPMS).
# Use when vendor/ is unavailable but a compiled dist jar exists.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
JAR="${1:-dist/quidoms-0.0.18-1.21.10.jar}"
test -f "$JAR"

STAGE=$(mktemp -d)
FULL=$(mktemp -d)
trap 'rm -rf "$STAGE" "$FULL"' EXIT

python3 - "$ROOT" "$STAGE" <<'PY'
import re, sys
from pathlib import Path
root, stage = map(Path, sys.argv[1:3])
props = {}
for line in (root / "gradle.properties").read_text().splitlines():
    line = line.strip()
    if not line or line.startswith("#") or "=" not in line:
        continue
    k, v = line.split("=", 1)
    props[k.strip()] = v.strip()
props.setdefault("version", props.get("mod_version", ""))
toml = (root / "src/main/resources/META-INF/neoforge.mods.toml").read_text()
def repl(m):
    k = m.group(1)
    if k not in props:
        raise SystemExit(f"missing ${{{k}}}")
    return props[k]
out = re.sub(r"\$\{([^}]+)\}", repl, toml)
meta = stage / "META-INF"
meta.mkdir(parents=True)
(meta / "neoforge.mods.toml").write_text(out)
PY

cp src/main/resources/META-INF/accesstransformer.cfg "$STAGE/META-INF/"
cp src/main/resources/pack.mcmeta "$STAGE/"
cp src/main/resources/minecolonies_logo.png "$STAGE/" 2>/dev/null || cp minecolonies_logo.png "$STAGE/"
MOD_VER=$(grep '^mod_version=' gradle.properties | cut -d= -f2)
cat > "$STAGE/META-INF/MANIFEST.MF" <<EOF
Manifest-Version: 1.0
Implementation-Title: Quidoms
Implementation-Version: ${MOD_VER}
Implementation-Vendor: JoshuaMEtaylor
Automatic-Module-Name: minecolonies

EOF

(
  cd "$FULL"
  jar xf "$ROOT/$JAR"
  # Strip NeoForge packages and modular metadata that would clash with NeoForge JPMS exports.
  rm -rf net/neoforged
  find . -name 'module-info.class' -delete
  rm -rf META-INF
  mkdir -p META-INF
  # Keep jarjar dir empty only if needed by tooling; do not nest NeoForge jars.
  cp -a "$STAGE/META-INF/." META-INF/
  cp "$STAGE/pack.mcmeta" .
  cp "$STAGE/minecolonies_logo.png" .
  jar cfm "$ROOT/$JAR" META-INF/MANIFEST.MF .
)

python3 - "$ROOT/$JAR" <<'PY'
import sys, zipfile
jar = sys.argv[1]
with zipfile.ZipFile(jar) as z:
    bad = [n for n in z.namelist()
           if (n.startswith('net/neoforged/') and n.endswith('.class'))
           or n.endswith('module-info.class')]
    if bad:
        raise SystemExit(f'JPMS strip failed, still present: {bad}')
    if 'com/minecolonies/api/compat/INBTSerializable.class' not in z.namelist():
        raise SystemExit('missing com.minecolonies.api.compat.INBTSerializable shim')
    mf = z.read('META-INF/MANIFEST.MF').decode()
    if 'Automatic-Module-Name: minecolonies' not in mf:
        raise SystemExit('MANIFEST missing Automatic-Module-Name: minecolonies')
print(f'Repacked OK: {jar}')
PY

unzip -l "$JAR" | rg 'neoforge.mods.toml|pack.mcmeta|accesstransformer|minecolonies_logo|INBTSerializable|MANIFEST' || true
