# Quidoms

Clean-room colony management mod for **Minecraft 1.21.10 / NeoForge 21.10.x**.

Inspired by MineColonies concepts — **not** a port. No Structurize/BlockUI/Domum. Economics stay in QuidCraft.

Repository: https://github.com/JoshuaMEtaylor/Quidoms

## Status

v1 vertical slice in progress: colony create/claim, citizens + housing, builder + ingredient BOM schematics, unbreakable colonist tools, modern UI. No raids.

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
