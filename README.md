# Quidoms

Colony simulator for **NeoForge / Minecraft 1.21.1**, based on the **full [MineColonies](https://github.com/ldtteam/minecolonies)** codebase (LDT Team).

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

## Minecraft version note

Upstream LDT NeoForge line for this tree is **1.21.1** (`neo 21.1.x`), not 1.21.10. Structurize / BlockUI / Domum do not yet publish a 1.21.10 line usable as hard deps. Quidoms therefore ships the **real full mod on 1.21.1**. A later pass can port the LDT stack to 1.21.10 when those libs exist.

## Install

1. Install **Minecraft 1.21.1** + **NeoForge 21.1.x**.
2. Install matching **Structurize**, **BlockUI**, **Domum Ornamentum**, and **MultiPiston** (versions from `gradle.properties`).
3. Drop `dist/quidoms-*.jar` (or the built `minecolonies-*-1.21.1.jar`) into `mods/`.

## Build

```bash
./gradlew build
```

Playable jar: `build/libs/minecolonies-*-1.21.1.jar` (also mirrored under `dist/`).

## Play

Same colony flow as MineColonies: supply camp → Town Hall → huts → builders/warehouse, etc. Raids will not occur. Stock builders with ingredient materials rather than only finished crafted blocks where recipes expand.
