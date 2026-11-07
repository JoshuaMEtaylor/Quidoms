#!/usr/bin/env bash
# Build standalone NeoForge 1.21.10 LDT jars from a previously-ported Quidoms fat jar
# (class/resource dump). Output jars are real separate mods — Quidoms must NOT also
# embed these packages.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

SRC_JAR="${1:-}"
if [[ -z "$SRC_JAR" ]]; then
  for c in /tmp/q31.jar dist/quidoms-0.0.31-1.21.10.jar; do
    [[ -f "$c" ]] && SRC_JAR=$c && break
  done
fi
test -n "${SRC_JAR:-}" && test -f "$SRC_JAR"

props() { grep -E "^$1=" gradle.properties | head -1 | cut -d= -f2-; }
BLOCKUI_VER=$(props blockUI_version)
STRUCTURIZE_VER=$(props structurize_version)
DOMUM_VER=$(props domumOrnamentumVersion)
MULTI_VER=$(props multiPistonVersion)
MC_RANGE=$(props minecraft_range)
FORGE_MIN=$(props forgeMinVersion)
FML_RANGE=$(props fml_range)

OUT_DIR=dist
LIBS_DIR=libs
mkdir -p "$OUT_DIR" "$LIBS_DIR"

python3 - "$SRC_JAR" "$BLOCKUI_VER" "$STRUCTURIZE_VER" "$DOMUM_VER" "$MULTI_VER" "$MC_RANGE" "$FORGE_MIN" "$FML_RANGE" "$OUT_DIR" "$LIBS_DIR" <<'PY'
import sys, zipfile, shutil
from pathlib import Path

(src_jar, blockui_ver, structurize_ver, domum_ver, multi_ver,
 mc_range, forge_min, fml_range, out_dir, libs_dir) = sys.argv[1:]
out_dir, libs_dir = Path(out_dir), Path(libs_dir)
stage = Path('/tmp/ldt-standalone-stage')
if stage.exists():
    shutil.rmtree(stage)
stage.mkdir(parents=True)

with zipfile.ZipFile(src_jar) as z:
    names = set(z.namelist())

    def pull(dest: Path, prefixes, exact=()):
        dest.mkdir(parents=True, exist_ok=True)
        count = 0
        for n in names:
            if n.endswith('/'):
                continue
            if any(n == p or n.startswith(p) for p in prefixes) or n in exact:
                target = dest / n
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(z.read(n))
                count += 1
        return count

    specs = [
        dict(
            out=out_dir / f'blockui-{blockui_ver}.jar',
            modid='blockui', version=blockui_ver, name='BlockUI',
            module='blockui',
            desc='BlockUI for NeoForge 1.21.10 (ported; standalone).',
            prefixes=('com/ldtteam/blockui/', 'com/ldtteam/common/',
                      'assets/blockui/', 'data/blockui/'),
            deps=(),
        ),
        dict(
            out=out_dir / f'structurize-{structurize_ver}.jar',
            modid='structurize', version=structurize_ver, name='Structurize',
            module='structurize',
            desc='Structurize for NeoForge 1.21.10 (ported; standalone).',
            prefixes=('com/ldtteam/structurize/', 'assets/structurize/',
                      'data/structurize/'),
            exact=('structurize.png',),
            deps=(('blockui', f'[{blockui_ver}, )'),
                  ('domum_ornamentum', f'[{domum_ver}, )')),
        ),
        dict(
            out=out_dir / f'domum_ornamentum-{domum_ver}.jar',
            modid='domum_ornamentum', version=domum_ver, name='Domum Ornamentum',
            module='domum_ornamentum',
            desc='Domum Ornamentum for NeoForge 1.21.10 (ported; standalone).',
            prefixes=('com/ldtteam/domumornamentum/', 'assets/domum_ornamentum/',
                      'data/domum_ornamentum/'),
            exact=(
                'data/minecraft/tags/block/doors.json',
                'data/minecraft/tags/block/fence_gates.json',
                'data/minecraft/tags/block/fences.json',
                'data/minecraft/tags/block/mineable/axe.json',
                'data/minecraft/tags/block/mineable/hoe.json',
                'data/minecraft/tags/block/mineable/pickaxe.json',
                'data/minecraft/tags/block/slabs.json',
                'data/minecraft/tags/block/stairs.json',
                'data/minecraft/tags/block/trapdoors.json',
                'data/minecraft/tags/block/walls.json',
                'data/minecraft/tags/block/wooden_doors.json',
                'data/minecraft/tags/block/wooden_fences.json',
                'data/minecraft/tags/block/wooden_slabs.json',
                'data/minecraft/tags/block/wooden_stairs.json',
                'data/minecraft/tags/block/wooden_trapdoors.json',
                'data/minecraft/tags/blocks/doors.json',
                'data/minecraft/tags/blocks/fence_gates.json',
                'data/minecraft/tags/blocks/fences.json',
                'data/minecraft/tags/blocks/mineable/axe.json',
                'data/minecraft/tags/blocks/mineable/hoe.json',
                'data/minecraft/tags/blocks/mineable/pickaxe.json',
                'data/minecraft/tags/blocks/slabs.json',
                'data/minecraft/tags/blocks/stairs.json',
                'data/minecraft/tags/blocks/trapdoors.json',
                'data/minecraft/tags/blocks/walls.json',
                'data/minecraft/tags/blocks/wooden_doors.json',
                'data/minecraft/tags/blocks/wooden_fences.json',
                'data/minecraft/tags/blocks/wooden_slabs.json',
                'data/minecraft/tags/blocks/wooden_stairs.json',
                'data/minecraft/tags/blocks/wooden_trapdoors.json',
            ),
            deps=(),
        ),
        dict(
            out=out_dir / f'multipiston-{multi_ver}.jar',
            modid='multipiston', version=multi_ver, name='Multi-Piston',
            module='multipiston',
            desc='Multi-Piston for NeoForge 1.21.10 (ported; standalone).',
            prefixes=('com/ldtteam/multipiston/', 'assets/multipiston/',
                      'data/multipiston/'),
            deps=(('blockui', f'[{blockui_ver}, )'),),
        ),
    ]

    pack = '{\n  "pack": {\n    "description": "%s resources",\n    "min_format": 88,\n    "max_format": 88\n  }\n}\n'

    for spec in specs:
        dest = stage / spec['modid']
        if dest.exists():
            shutil.rmtree(dest)
        n = pull(dest, spec['prefixes'], spec.get('exact', ()))
        if n == 0:
            raise SystemExit(f'no entries for {spec["modid"]}')
        for bad in list(dest.rglob('*')):
            if not bad.is_file():
                continue
            rel = bad.relative_to(dest).as_posix()
            if rel.startswith('net/neoforged/') or bad.name == 'module-info.class':
                bad.unlink()

        meta = dest / 'META-INF'
        meta.mkdir(parents=True, exist_ok=True)
        deps = [('neoforge', f'[{forge_min}, )'), ('minecraft', mc_range), *spec['deps']]
        dep_toml = []
        for mid, vr in deps:
            ordering = 'AFTER' if mid not in ('neoforge', 'minecraft') else 'NONE'
            dep_toml.append(
                f'[[dependencies.{spec["modid"]}]]\n'
                f'    modId="{mid}"\n'
                f'    type="required"\n'
                f'    versionRange="{vr}"\n'
                f'    ordering="{ordering}"\n'
                f'    side="BOTH"\n'
            )
        (meta / 'neoforge.mods.toml').write_text(
            f'modLoader="javafml"\n'
            f'loaderVersion="{fml_range}"\n'
            f'license="GPL-3.0-or-later"\n'
            f'issueTrackerURL="https://github.com/JoshuaMEtaylor/Quidoms/issues"\n\n'
            f'[[mods]]\n'
            f'modId="{spec["modid"]}"\n'
            f'version="{spec["version"]}"\n'
            f'displayName="{spec["name"]}"\n'
            f'authors="LDT Team; Quidoms 1.21.10 port"\n'
            f'description=\'\'\'\n{spec["desc"]}\n\'\'\'\n\n'
            + '\n'.join(dep_toml)
        )
        (dest / 'pack.mcmeta').write_text(pack % spec['name'])
        (meta / 'MANIFEST.MF').write_text(
            'Manifest-Version: 1.0\n'
            f'Implementation-Title: {spec["name"]}\n'
            f'Implementation-Version: {spec["version"]}\n'
            f'Implementation-Vendor: Quidoms/LDT\n'
            f'Automatic-Module-Name: {spec["module"]}\n\n'
        )
        out = Path(spec['out'])
        if out.exists():
            out.unlink()
        with zipfile.ZipFile(out, 'w', compression=zipfile.ZIP_DEFLATED) as zout:
            for path in sorted(dest.rglob('*')):
                if path.is_file():
                    zout.write(path, path.relative_to(dest).as_posix())
        # Also copy into libs/ for Quidoms compile/runtime deps
        shutil.copy2(out, libs_dir / out.name)
        print(f'Wrote {out} ({out.stat().st_size} bytes, {n} entries)')
PY

echo "Standalone LDT jars in $OUT_DIR and $LIBS_DIR"
ls -la "$OUT_DIR"/blockui-*.jar "$OUT_DIR"/structurize-*.jar "$OUT_DIR"/domum_ornamentum-*.jar "$OUT_DIR"/multipiston-*.jar
