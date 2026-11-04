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

UI modernization beyond branding is **deferred** this pass (BlockUI still powers the majority of screens).

## Minecraft / NeoForge versions

| Line | Minecraft | NeoForge | Notes |
|---|---|---|---|
| Current 1.21.10 jar (`dist/quidoms-*-1.21.10.jar`) | **1.21.10** | **21.10.x** | LDT libs are **baked into the Quidoms jar** (multi-`[[mods]]` / class-merged, not separate downloadable JiJ nested jars). |
| Older 1.21.1 line | 1.21.1 | 21.1.x | Upstream LDT publish line; use matching standalone LDT jars only if you are not using the baked 1.21.10 jar. |

## Install (1.21.10 Quidoms jar)

1. Install **Minecraft 1.21.10** + **NeoForge 21.10.x**.
2. Drop **only** `dist/quidoms-*-1.21.10.jar` into `mods/`.
3. **Do not** also install standalone **BlockUI**, **Structurize**, **Domum Ornamentum**, or **MultiPiston** jars. Those mod ids are already registered from the Quidoms jar; a second copy causes duplicate-mod / classpath conflicts.
4. Optional: JEI / JourneyMap / Dynamic Trees as usual.

### JPMS / NeoForge util package

Quidoms must **not** ship classes under `net.neoforged.*`. The old `INBTSerializable` shim lives only at `com.minecolonies.api.compat.INBTSerializable`. Shipping `net.neoforged.neoforge.common.util` inside the minecolonies jar makes that package export clash with NeoForge (`ResolutionException` … *modules minecolonies and neoforge export package net.neoforged.neoforge.common.util*).

## Build

```bash
./gradlew build
# or, when only refreshing META-INF / JPMS strip on an existing dist jar:
./scripts/repack-meta.sh dist/quidoms-0.0.14-1.21.10.jar
```

Playable 1.21.10 jar: `dist/quidoms-*-1.21.10.jar`.

## Play

Same colony flow as MineColonies: supply camp → Town Hall → huts → builders/warehouse, etc. Raids will not occur. Stock builders with ingredient materials rather than only finished crafted blocks where recipes expand.
