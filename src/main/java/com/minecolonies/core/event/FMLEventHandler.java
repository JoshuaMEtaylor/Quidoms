package com.minecolonies.core.event;

import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.core.datalistener.*;
import com.minecolonies.core.entity.pathfinding.Pathfinding;
import com.minecolonies.core.util.BackUpHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.minecraft.resources.ResourceLocation;
import com.minecolonies.api.util.constant.Constants;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Event handler used to catch various forge events.
 */
public class FMLEventHandler
{
    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Pre event)
    {
        IColonyManager.getInstance().onServerTick(event);
        DataPackSyncEventHandler.ServerEvents.load(event.getServer());
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Pre event)
    {
        IColonyManager.getInstance().onClientTick(event);
    }

    @SubscribeEvent
    public static void onPlayerLogin(@NotNull final PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer)
        {
            // This automatically reloads the owner of the colony if failed.
            IColonyManager.getInstance().getIColonyByOwner(event.getEntity().level(), event.getEntity());
            //ColonyManager.syncAllColoniesAchievements();
        }
    }

    @SubscribeEvent
    public static void onAddReloadListenerEvent(@NotNull final AddServerReloadListenersEvent event)
    {
        event.addListener(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "crafter_recipes"), new CrafterRecipeListener());
        event.addListener(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "research"), new ResearchListener());
        event.addListener(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "custom_visitor"), new CustomVisitorListener());
        event.addListener(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "citizen_names"), new CitizenNameListener());
        event.addListener(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "quests"), new QuestJsonListener());
        event.addListener(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "item_nbt"), new ItemNbtListener());
        event.addListener(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "study_items"), StudyItemListener.INSTANCE);
        event.addListener(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "diseases"), new DiseasesListener());
        event.addListener(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "recruitment_items"), new RecruitmentItemsListener());
    }

    @SubscribeEvent
    public static void onServerStarted(@NotNull final ServerStartedEvent event)
    {
        BackUpHelper.loadMissingColonies();
    }

    @SubscribeEvent
    public static void onWorldTick(final LevelTickEvent.Pre event)
    {
        IColonyManager.getInstance().onWorldTick(event);
    }

    @SubscribeEvent
    public static void onServerAboutToStart(@NotNull final ServerAboutToStartEvent event)
    {
        IColonyManager.getInstance().getRecipeManager().reset();
    }

    @SubscribeEvent
    public static void onServerStopped(@NotNull final ServerStoppingEvent event)
    {
        Pathfinding.shutdown();
        DataPackSyncEventHandler.ServerEvents.reset();
    }
}
