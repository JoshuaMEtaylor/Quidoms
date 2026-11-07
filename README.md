# Quidoms

Colony simulator for **NeoForge**, based on the **full [MineColonies](https://github.com/ldtteam/minecolonies)** codebase (LDT Team).

Repository: https://github.com/JoshuaMEtaylor/Quidoms

## License & attribution

- **License: GPL-3.0** (see [`LICENSE`](LICENSE)) — inherited from MineColonies.
- Upstream: [ldtteam/minecolonies](https://github.com/ldtteam/minecolonies) (`version/1.21`).
- Required LDT libraries (also GPL/LGPL as upstream): **Structurize**, **BlockUI**, **Domum Ornamentum**, **MultiPiston**.

This is **not** a from-scratch stub. Quidoms keeps essentially the whole MineColonies game surface and applies product cuts below.

## Quidoms changes vs upstream MineColonies

1. **No raids** — raid scheduling/tick paths hard-disabled (`RaidManager.canRaid` / `onNightFall`); config default `enablecolonyraids=false`.
2. **Colonist tools** — durability loss disabled; tools forced unbreakable; hut/citizen **tool level gates removed** so any tier works immediately; item attributes/enchants retained.
3. **Ingredient BOM builds** — builder resource lists expand schematic items through recursive crafting recipes into base ingredients (`IngredientBomExpand` → `BuildingResourcesModule`).
4. **Branding** — display name **Quidoms** (mod id remains `minecolonies` for blueprint/save compatibility).

## Minecraft / NeoForge versions

| Line | Minecraft | NeoForge | Notes |
|---|---|---|---|
| Current separate-jars (`dist/*-1.21.10.jar`) | **1.21.10** | **21.10.x** | Quidoms + standalone LDT jars (normal MineColonies-style install). |
| Older 1.21.1 line on `main` | 1.21.1 | 21.1.x | Upstream LDT publish line. |

## Install (1.21.10 separate jars)

1. Install **Minecraft 1.21.10** + **NeoForge 21.10.x**.
2. Drop **all** of these into `mods/` (do not omit deps):
   - `dist/blockui-1.0.212-1.21.10.jar`
   - `dist/structurize-1.0.832-1.21.10.jar`
   - `dist/domum_ornamentum-1.21.10-1.0.193.jar`
   - `dist/multipiston-1.2.58-1.21.10.jar`
   - `dist/quidoms-*-1.21.10.jar`
3. **Do not** use the old fat/JiJ Quidoms jar that baked LDT mods inside it.
4. Optional: JEI / JourneyMap / Dynamic Trees as usual.

### JPMS / NeoForge util package

Quidoms must **not** ship classes under `net.neoforged.*`. The old `INBTSerializable` shim lives only at `com.minecolonies.api.compat.INBTSerializable`.

## Build

```bash
# Package standalone LDT jars (from a known-good 1.21.10 class dump / prior fat jar):
./scripts/package-ldt-standalone.sh /path/to/source.jar
# Build Quidoms against libs/*.jar:
./gradlew build
```

Outputs land in `dist/`. Gradle `runServer` / `runClient` copy the LDT jars into `run/mods/` automatically.

## Play

Same colony flow as MineColonies: supply camp → Town Hall → huts → builders/warehouse, etc. Raids will not occur. Stock builders with ingredient materials rather than only finished crafted blocks where recipes expand.
