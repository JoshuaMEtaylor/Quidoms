# Tip 1.21.10 SavedData (SavedDataType + load-guard)

These sources are the **NeoForge 1.21.10** versions of colony save-data used in the tip fat jar
(`quidoms-0.0.xx-1.21.10.jar`). They use `SavedDataType` (not 1.21.1 `SavedData.Factory`).

The main Quidoms tree stays on 1.21.1 for CI; tip classes are compiled here with gradle javac
against a tip base jar + NeoForge 21.10 merged minecraft slice.

## Compile

From repo root:

```bash
./gradlew -p tip/1.21.10 compileTipSavedata \
  -PtipBaseJar=/path/to/quidoms-0.0.31-1.21.10.jar \
  -PneoforgeMergedJar=/path/to/neoforge-21.10.64-merged.jar \
  -PoutDir=/tmp/tip-savedata-classes \
  --no-configuration-cache
```

Do **not** ASM-patch tip `IServerColonySaveData` / `ServerColonySaveData`. Install only
gradle-javac output into the tip jar.
