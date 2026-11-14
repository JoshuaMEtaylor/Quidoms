/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.minecolonies.api.colony.ICitizenData
 *  com.minecolonies.api.colony.IColony
 *  com.minecolonies.api.colony.buildings.IBuilding
 *  com.minecolonies.api.colony.buildings.ICommonBuilding
 *  com.minecolonies.api.crafting.ItemStorage
 *  com.minecolonies.api.util.IItemHandlerCapProvider
 *  com.minecolonies.api.util.InventoryUtils
 *  com.minecolonies.api.util.ItemStackUtils
 *  com.minecolonies.api.util.Log
 *  com.minecolonies.core.colony.buildings.AbstractBuilding
 *  com.minecolonies.core.colony.buildings.AbstractBuildingStructureBuilder
 *  com.minecolonies.core.colony.buildings.utils.BuildingBuilderResource
 *  com.minecolonies.core.compatibility.quidcraft.QuidCraftProvisionOutcome
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.neoforged.fml.ModList
 *  net.neoforged.neoforge.items.IItemHandler
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.minecolonies.core.compatibility.quidcraft;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.ICommonBuilding;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.IItemHandlerCapProvider;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.AbstractBuildingStructureBuilder;
import com.minecolonies.core.colony.buildings.utils.BuildingBuilderResource;
import com.minecolonies.core.compatibility.quidcraft.QuidCraftProvisionOutcome;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class QuidCraftMarketBridge {
    public static final String MOD_ID = "quidcraft";
    private static final long COOLDOWN_MS = 3000L;
    private static final Map<String, Long> COOLDOWNS = new ConcurrentHashMap<String, Long>();
    private static Boolean presentCached;
    private static boolean reflectFailed;
    private static Boolean provisionApiCached;

    private QuidCraftMarketBridge() {
    }

    public static boolean isPresent() {
        if (presentCached == null) {
            presentCached = ModList.get().isLoaded(MOD_ID);
        }
        return presentCached != false && !reflectFailed;
    }

    public static boolean tryBuyForBuilding(@NotNull IBuilding iBuilding, @NotNull ItemStack itemStack, int n, boolean bl) {
        return QuidCraftMarketBridge.tryBuyForBuilding(iBuilding, itemStack, n, bl, "", null);
    }

    public static boolean tryBuyForBuilding(@NotNull IBuilding iBuilding, @NotNull ItemStack itemStack, int n, boolean bl, @NotNull String string) {
        return QuidCraftMarketBridge.tryBuyForBuilding(iBuilding, itemStack, n, bl, string, null);
    }

    public static boolean tryBuyForBuilding(@NotNull IBuilding iBuilding, @NotNull ItemStack itemStack, int n, boolean bl, @NotNull String string, @Nullable ICitizenData iCitizenData) {
        if (!QuidCraftMarketBridge.isPresent() || itemStack.isEmpty() || n <= 0 || iBuilding.getColony() == null || iBuilding.getColony().getWorld() == null) {
            return false;
        }
        if (iBuilding.getColony().getWorld().isClientSide()) {
            return false;
        }
        IColony iColony = iBuilding.getColony();
        String string2 = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        String string3 = iColony.getID() + "|" + string2;
        long l = System.currentTimeMillis();
        Long l2 = COOLDOWNS.get(string3);
        if (l2 != null && l2 > l) {
            return false;
        }
        UUID uUID = UUID.randomUUID();
        UUID uUID2 = iColony.getPermissions().getOwner();
        String string4 = iColony.getPermissions().getOwnerName() == null ? "colony-owner" : iColony.getPermissions().getOwnerName();
        String object = iColony.getName() == null ? "colony-" + iColony.getID() : iColony.getName();
        UUID uUID3 = uUID2 == null ? new UUID(0L, 0L) : uUID2;
        try {
            if (!QuidCraftMarketBridge.quidReady()) {
                COOLDOWNS.put(string3, l + 1000L);
                return false;
            }
            Level level = iColony.getWorld();
            BlockPos blockPos = iBuilding.getPosition();
            String string5 = level.dimension().location().toString();
            long infraBudget = QuidCraftMarketBridge.infraBudgetRemaining(object);
            if (infraBudget <= 0L) {
                COOLDOWNS.put(string3, l + 2000L);
                QuidCraftMarketBridge.recordOrder(uUID, iColony.getID(), object, uUID3, string4, string, string2, n, false, 0L, "infra-budget", l);
                return false;
            }
            QuidCraftProvisionOutcome quidCraftProvisionOutcome = QuidCraftMarketBridge.invokeProvision(string2, n, string5, blockPos.getX(), blockPos.getZ(), l, infraBudget);
            if (!quidCraftProvisionOutcome.onCatalogue()) {
                COOLDOWNS.put(string3, l + 2000L);
                return false;
            }
            COOLDOWNS.put(string3, l + 3000L);
            if (quidCraftProvisionOutcome.filled() <= 0) {
                QuidCraftMarketBridge.recordOrder(uUID, iColony.getID(), object, uUID3, string4, string, string2, n, false, 0L, quidCraftProvisionOutcome.detail().isEmpty() ? "no-fill" : quidCraftProvisionOutcome.detail(), l);
                return false;
            }
            long cost = quidCraftProvisionOutcome.notionalCostMinor();
            int n2 = QuidCraftMarketBridge.deposit(iBuilding, iCitizenData, itemStack, quidCraftProvisionOutcome.filled(), bl);
            if (n2 > 0 && cost > 0L && !QuidCraftMarketBridge.chargeInfra(object, cost, uUID3, l)) {
                // Delivered materials but treasury/allotment could not cover — record soft failure for ops.
                QuidCraftMarketBridge.recordOrder(uUID, iColony.getID(), object, uUID3, string4, string, string2, n, true, cost, "delivered-unpaid", l);
                Log.getLogger().warn("Infrastructure budget charge failed after delivering {} x{} to {}", (Object)string2, (Object)n2, object);
            }
            int n3 = InventoryUtils.hasBuildingEnoughElseCount((ICommonBuilding)iBuilding, (ItemStorage)new ItemStorage(itemStack, true, bl), (int)n);
            int n4 = 0;
            if (iCitizenData != null && iCitizenData.getInventory() != null) {
                n4 = InventoryUtils.getItemCountInItemHandler((IItemHandler)iCitizenData.getInventory(), itemStack2 -> ItemStackUtils.compareItemStacksIgnoreStackSize((ItemStack)itemStack2, (ItemStack)itemStack, (boolean)true, (boolean)bl));
            }
            boolean bl2 = n3 + n4 >= n;
            String string6 = (quidCraftProvisionOutcome.detail().isEmpty() ? "provision" : quidCraftProvisionOutcome.detail()) + ";delivered=" + n2 + (bl2 ? ";ok" : ";partial");
            QuidCraftMarketBridge.recordOrder(uUID, iColony.getID(), object, uUID3, string4, string, string2, n, bl2 || n2 > 0, quidCraftProvisionOutcome.notionalCostMinor(), string6, l);
            if (n2 > 0) {
                Log.getLogger().info("QuidCraft provisioned {} x{} for colony {} (delivered={}, enough={})", (Object)string2, (Object)quidCraftProvisionOutcome.filled(), object, (Object)n2, (Object)bl2);
            }
            return bl2;
        }
        catch (ClassNotFoundException | NoSuchMethodException reflectiveOperationException) {
            reflectFailed = true;
            Log.getLogger().warn("QuidCraft present but provision API reflect failed; disabling bridge: " + String.valueOf(reflectiveOperationException));
            return false;
        }
        catch (NoClassDefFoundError noClassDefFoundError) {
            String string7 = String.valueOf(noClassDefFoundError.getMessage());
            if (string7.contains("com.quidcraft.") || string7.contains("QuidApi") || string7.contains("QuidAccess")) {
                reflectFailed = true;
                Log.getLogger().warn("QuidCraft present but provision API class missing; disabling bridge: " + String.valueOf(noClassDefFoundError));
            } else {
                COOLDOWNS.put(string3, l + 3000L);
                Log.getLogger().warn("QuidCraft provision classload glitch (bridge stays enabled): {}", (Object)noClassDefFoundError.toString());
            }
            return false;
        }
        catch (Throwable throwable) {
            COOLDOWNS.put(string3, l + 3000L);
            QuidCraftMarketBridge.recordOrder(uUID, iColony.getID(), object, uUID3, string4, string, string2, n, false, 0L, throwable.toString(), l);
            Log.getLogger().warn("QuidCraft provision buy failed for {}: {}", (Object)string2, (Object)throwable.toString());
            return false;
        }
    }

    public static void tryProvisionNeededResources(@NotNull IBuilding iBuilding, @NotNull String string) {
        QuidCraftMarketBridge.tryProvisionNeededResources(iBuilding, string, null);
    }

    public static void tryProvisionNeededResources(@NotNull IBuilding iBuilding, @NotNull String string, @Nullable ICitizenData iCitizenData) {
        if (!QuidCraftMarketBridge.isPresent() || !(iBuilding instanceof AbstractBuildingStructureBuilder)) {
            return;
        }
        AbstractBuildingStructureBuilder abstractBuildingStructureBuilder = (AbstractBuildingStructureBuilder)iBuilding;
        try {
            Map<String, BuildingBuilderResource> map = abstractBuildingStructureBuilder.getNeededResources();
            if (map == null || map.isEmpty()) {
                return;
            }
            for (BuildingBuilderResource buildingBuilderResource : map.values()) {
                int n;
                if (buildingBuilderResource == null || buildingBuilderResource.getItemStack() == null || buildingBuilderResource.getItemStack().isEmpty() || buildingBuilderResource.getAmount() <= 0) continue;
                ItemStack itemStack = buildingBuilderResource.getItemStack();
                int n2 = InventoryUtils.hasBuildingEnoughElseCount((ICommonBuilding)iBuilding, (ItemStorage)new ItemStorage(itemStack, true, false), (int)buildingBuilderResource.getAmount());
                if (iCitizenData != null && iCitizenData.getInventory() != null) {
                    n2 += InventoryUtils.getItemCountInItemHandler((IItemHandler)iCitizenData.getInventory(), itemStack2 -> ItemStackUtils.compareItemStacksIgnoreStackSize((ItemStack)itemStack2, (ItemStack)itemStack, (boolean)true, (boolean)false));
                }
                if ((n = buildingBuilderResource.getAmount() - n2) <= 0) continue;
                QuidCraftMarketBridge.tryBuyForBuilding(iBuilding, itemStack, n, false, string, iCitizenData);
            }
        }
        catch (Throwable throwable) {
            Log.getLogger().warn("QuidCraft structure provision sweep failed: {}", (Object)throwable.toString());
        }
    }

    private static void recordOrder(UUID uUID, int n, String string, UUID uUID2, String string2, String string3, String string4, int n2, boolean bl, long l, String string5, long l2) {
        try {
            Class<?> clazz = Class.forName("com.quidcraft.api.QuidApi");
            if (!Boolean.TRUE.equals(clazz.getMethod("ready", new Class[0]).invoke(null, new Object[0]))) {
                return;
            }
            Object object = clazz.getMethod("get", new Class[0]).invoke(null, new Object[0]);
            object.getClass().getMethod("recordColonyMarketOrder", UUID.class, Integer.TYPE, String.class, UUID.class, String.class, String.class, String.class, Integer.TYPE, Boolean.TYPE, Long.TYPE, String.class, Long.TYPE).invoke(object, uUID, n, string, uUID2, string2, string3 == null ? "" : string3, string4, n2, bl, l, string5 == null ? "" : string5, l2);
        }
        catch (NoSuchMethodException noSuchMethodException) {
        }
        catch (Throwable throwable) {
            Log.getLogger().debug("Could not record QuidCraft colony order: {}", (Object)throwable.toString());
        }
    }

    private static boolean quidReady() throws Exception {
        Class<?> clazz = Class.forName("com.quidcraft.api.QuidApi");
        return Boolean.TRUE.equals(clazz.getMethod("ready", new Class[0]).invoke(null, new Object[0]));
    }

    private static QuidCraftProvisionOutcome invokeProvision(String string, int n, String string2, int n2, int n3, long l, long maxBudget) throws Exception {
        Class<?> clazz = Class.forName("com.quidcraft.api.QuidApi");
        Object object = clazz.getMethod("get", new Class[0]).invoke(null, new Object[0]);
        if (provisionApiCached == null) {
            try {
                object.getClass().getMethod("provisionMarketBuy", UUID.class, String.class, Integer.TYPE, String.class, Integer.TYPE, Integer.TYPE, Long.TYPE, Long.TYPE);
                provisionApiCached = true;
            }
            catch (NoSuchMethodException noSuchMethodException) {
                try {
                    object.getClass().getMethod("provisionMarketBuy", UUID.class, String.class, Integer.TYPE, String.class, Integer.TYPE, Integer.TYPE, Long.TYPE);
                    provisionApiCached = true;
                } catch (NoSuchMethodException again) {
                    provisionApiCached = false;
                }
            }
        }
        if (!Boolean.TRUE.equals(provisionApiCached)) {
            Log.getLogger().warn("QuidCraft loaded but provisionMarketBuy is missing — need QuidCraft >= 1.0.29 for builder buys.");
            return new QuidCraftProvisionOutcome(false, 0, 0L, "no-provision-api");
        }
        Object object2;
        try {
            object2 = object.getClass().getMethod("provisionMarketBuy", UUID.class, String.class, Integer.TYPE, String.class, Integer.TYPE, Integer.TYPE, Long.TYPE, Long.TYPE)
                .invoke(object, UUID.randomUUID(), string, n, string2, n2, n3, l, maxBudget);
        } catch (NoSuchMethodException noBudget) {
            object2 = object.getClass().getMethod("provisionMarketBuy", UUID.class, String.class, Integer.TYPE, String.class, Integer.TYPE, Integer.TYPE, Long.TYPE)
                .invoke(object, UUID.randomUUID(), string, n, string2, n2, n3, l);
        }
        if (object2 == null) {
            return new QuidCraftProvisionOutcome(false, 0, 0L, "null-result");
        }
        boolean bl = Boolean.TRUE.equals(object2.getClass().getMethod("onCatalogue", new Class[0]).invoke(object2, new Object[0]));
        int n4 = ((Number)object2.getClass().getMethod("filled", new Class[0]).invoke(object2, new Object[0])).intValue();
        long l2 = ((Number)object2.getClass().getMethod("notionalCostMinor", new Class[0]).invoke(object2, new Object[0])).longValue();
        Object object3 = object2.getClass().getMethod("detail", new Class[0]).invoke(object2, new Object[0]);
        return new QuidCraftProvisionOutcome(bl, n4, l2, object3 == null ? "" : object3.toString());
    }

    private static int deposit(@NotNull IBuilding iBuilding, @Nullable ICitizenData iCitizenData, @NotNull ItemStack itemStack, int n, boolean bl) {
        int n2;
        ItemStack itemStack2;
        if (n <= 0) {
            return 0;
        }
        Item item = itemStack.getItem();
        ItemStack itemStack3 = itemStack2 = bl ? itemStack.copyWithCount(n) : new ItemStack((ItemLike)item, n);
        if (itemStack2.isEmpty()) {
            itemStack2 = new ItemStack((ItemLike)item, n);
        }
        int n3 = 0;
        if (iCitizenData != null && iCitizenData.getInventory() != null && !itemStack2.isEmpty()) {
            n2 = itemStack2.getCount();
            itemStack2 = InventoryUtils.addItemStackToItemHandlerWithResult((IItemHandler)iCitizenData.getInventory(), (ItemStack)itemStack2);
            if (itemStack2 == null || itemStack2.isEmpty()) {
                n3 += n2;
                itemStack2 = ItemStack.EMPTY;
            } else {
                n3 += Math.max(0, n2 - itemStack2.getCount());
            }
        }
        while (!itemStack2.isEmpty() && itemStack2.getCount() > 0) {
            int n4;
            ItemStack itemStack4;
            n2 = Math.min(itemStack2.getCount(), Math.max(1, itemStack2.getMaxStackSize()));
            ItemStack itemStack5 = itemStack4 = itemStack2.copyWithCount(n2);
            if (iBuilding instanceof AbstractBuilding) {
                AbstractBuilding abstractBuilding = (AbstractBuilding)iBuilding;
                itemStack5 = abstractBuilding.forceTransferStack(itemStack4, iBuilding.getColony().getWorld());
                if (itemStack5 == null) {
                    itemStack5 = ItemStack.EMPTY;
                }
            } else if (iBuilding.getTileEntity() != null && (itemStack5 = InventoryUtils.addItemStackToProviderWithResult((IItemHandlerCapProvider)iBuilding.getTileEntity(), (ItemStack)itemStack4)) == null) {
                itemStack5 = ItemStack.EMPTY;
            }
            if ((n4 = n2 - (itemStack5.isEmpty() ? 0 : itemStack5.getCount())) <= 0) break;
            n3 += n4;
            itemStack2.shrink(n4);
        }
        return n3;
    }

    private static long infraBudgetRemaining(String kingdomName) {
        try {
            Class<?> clazz = Class.forName("com.quidcraft.api.QuidApi");
            if (!Boolean.TRUE.equals(clazz.getMethod("ready").invoke(null))) {
                return 0L;
            }
            Object access = clazz.getMethod("get").invoke(null);
            Object value = access.getClass().getMethod("infraBudgetRemaining", String.class).invoke(access, kingdomName);
            return value instanceof Number ? ((Number) value).longValue() : 0L;
        } catch (Throwable ignored) {
            return Long.MAX_VALUE / 4L;
        }
    }

    private static boolean chargeInfra(String kingdomName, long amount, UUID actor, long now) {
        try {
            Class<?> clazz = Class.forName("com.quidcraft.api.QuidApi");
            if (!Boolean.TRUE.equals(clazz.getMethod("ready").invoke(null))) {
                return true;
            }
            Object access = clazz.getMethod("get").invoke(null);
            Object result = access.getClass()
                .getMethod("chargeKingdomInfra", UUID.class, String.class, Long.TYPE, UUID.class, Long.TYPE)
                .invoke(access, UUID.randomUUID(), kingdomName, amount, actor, now);
            return result != null && Boolean.TRUE.equals(result.getClass().getMethod("ok").invoke(result));
        } catch (NoSuchMethodException missing) {
            return true;
        } catch (Throwable ex) {
            Log.getLogger().debug("Infra charge failed: {}", ex.toString());
            return false;
        }
    }

}
