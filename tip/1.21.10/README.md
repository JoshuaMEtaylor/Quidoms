# Tip 1.21.10 SavedData (SavedDataType + load-guard)

These sources are the **NeoForge 1.21.10** tip-facing classes used in the tip fat jar
(`quidoms-0.0.xx-1.21.10.jar`):

- SavedData: `SavedDataType` + `ColonySaveDataLoadGuard` (not 1.21.1 `SavedData.Factory`)
- `CreateColonyMessage`: `ServerPlayer.level()` + `ServerLevel.getRespawnData()` (not `serverLevel()` / `getSharedSpawnPos()`)
- Founding UI: settlement covenant shows founding fee; insufficient funds opens cant-create warning

The main Quidoms tree stays on 1.21.1 for CI; tip classes are compiled here with gradle javac
against a tip base jar + NeoForge 21.10 merged minecraft slice.

## Compile

From repo root:

```bash
./gradlew -p tip/1.21.10 compileTipClasses \
  -PtipBaseJar=/path/to/quidoms-0.0.31-1.21.10.jar \
  -PneoforgeMergedJar=/path/to/neoforge-21.10.64-merged.jar \
  -PoutDir=/tmp/tip-classes \
  --no-configuration-cache
```

Do **not** ASM-patch tip classes. Install only gradle-javac output into the tip jar.
