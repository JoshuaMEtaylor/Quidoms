# Quidoms

Clean-room colony management mod for **Minecraft 1.21.10 / NeoForge 21.10.x**.

Inspired by MineColonies concepts — **not** a port. No Structurize/BlockUI/Domum. Economics stay in QuidCraft.

Repository: https://github.com/JoshuaMEtaylor/Quidoms

## Status

v1 vertical slice in progress: colony create/claim, citizens + housing, builder + ingredient BOM schematics, unbreakable colonist tools, modern UI. No raids.

## Design rules

- **Colonist tools**: Any tool or weapon on a colonist is made unbreakable and stays that way. No citizen-level gate on tool tiers; mining, damage, and enchantments match the item a player would use.
- **Ingredient builds**: Build cost is the recursive crafting BOM of the schematic palette — not exotic pre-crafted blocks. The builder consumes ingredients and places target blocks.
- **No raids**: Raid/invasion systems are out of scope for v1 (and not stubbed into the tick loop).
- **UI**: Vanilla/NeoForge screens only — no BlockUI / Structurize / LDT stack. Economics stay in QuidCraft.

## Requirements

- JDK 21
- NeoForge `21.10.64` (or newer 21.10.x)
- Minecraft `1.21.10`

## Build / run

```bash
./gradlew build
./gradlew runClient
```

## Found a colony

1. Creative tab **Quidoms** → place **Town Hall** to found a colony (chunk claim + owner).
2. Place **House** blocks to raise citizen capacity; citizens spawn to fill beds.
3. Place **Builder Hut** (auto-assigns an unemployed citizen as builder) and **Warehouse**.
4. Open the Town Hall UI to view citizens, buildings, and start a build order.
5. Supply **ingredient** materials (recipe BOM, not crafted target blocks). The builder consumes ingredients and places schematic blocks.

## License

All Rights Reserved.
