# Quidoms

Clean-room colony management mod for **Minecraft 1.21.10 / NeoForge 21.10.x**.

Inspired by MineColonies concepts — **not** a port. No Structurize / BlockUI / Domum Ornamemtum. Economics stay in QuidCraft.

Repository: https://github.com/JoshuaMEtaylor/Quidoms

## Install (play)

1. Install **Minecraft 1.21.10** with **NeoForge 21.10.64+** (same game version).
2. Drop the playable jar from [`dist/quidoms-1.1.0.jar`](dist/quidoms-1.1.0.jar) into your `mods/` folder.
3. Launch the NeoForge 1.21.10 profile. Quidoms should appear in the mods list and creative tab **Quidoms**.

## Design rules

- **Colonist tools**: Any tool or weapon on a colonist is made unbreakable and stays that way. No citizen-level gate on tool tiers; mining, damage, and enchantments match the item a player would use.
- **Ingredient builds**: Build cost is the recursive crafting BOM of the schematic palette — not exotic pre-crafted blocks. The builder pulls stock from the warehouse, crafts intermediates when needed, and places target blocks.
- **No raids**: Raid/invasion systems are out of scope (and not stubbed into the tick loop).
- **UI**: Vanilla/NeoForge screens only — no BlockUI / Structurize / LDT stack.

## Requirements (dev)

- JDK 21
- NeoForge `21.10.64` (or newer 21.10.x)
- Minecraft `1.21.10`

## Build / run

```bash
./gradlew build
./gradlew runClient
```

The release jar is written to `build/libs/quidoms-<version>.jar` and mirrored to `dist/` for downloads.

## Found a colony (vertical slice)

1. Creative tab **Quidoms** → place **Town Hall** to found a colony (chunk claim + owner).
2. Place **House** blocks to raise citizen capacity; citizens spawn to fill beds.
3. Place a **Builder Hut** (auto-assigns an unemployed citizen as builder).
4. Place a **Warehouse**, open it, and stock **ingredient** materials (e.g. oak logs for pavilion/cabin — planks are crafted by the builder from logs when recipes allow; cobble/gravel for the path).
5. Open the Town Hall UI → **Build** → start **Pavilion**, **Cabin**, or **Path**. Stand where you want the origin.
6. The builder walks the schematic, crafts/converts from warehouse stock, and places blocks until the order completes.

## Schematics included

| Id | Description |
| --- | --- |
| `quidoms:wood_pavilion` | Small wood roof pavilion |
| `quidoms:wood_cabin` | Simple oak cabin |
| `quidoms:stone_path` | Cobble + gravel path strip |

## Crash notes (1.0.0 → 1.1.0)

Immediate client crash causes fixed in 1.1.0:

1. **`Block id not set` / unbound block holders** — NeoForge 1.21.10 requires `BlockBehaviour.Properties.setId(...)` via `DeferredRegister.Blocks#registerBlock` (and matching item helpers). Plain `BLOCKS.register("name", () -> new Block(props))` hard-crashes during `RegisterEvent`.
2. Missing `assets/quidoms/items/*.json` item model defs (1.21.2+ item model system).
3. Incomplete `neoforge.mods.toml` (`modLoader` / `loaderVersion`).
4. Client-only menu class referenced from common registration; moved to `com.quidoms.menu`.
5. Earlier missing `com.quidoms.build` package (reconstructed).

Verified locally: `./gradlew runClient` reaches `Quidoms loaded` + `Quidoms client ready` without a mod-loading crash.

## License

All Rights Reserved.
