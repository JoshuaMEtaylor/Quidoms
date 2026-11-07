package com.ldtteam.structurize.storage;

import com.google.common.collect.ImmutableList;
import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of survival blueprint handlers.
 * <p>
 * Quidoms: {@code getMatchingHandlers} must expose the {@link ClientLevel} descriptor that
 * {@code AbstractBlueprintManipulationWindow} was compiled against. A {@link Level}-only
 * signature caused {@link NoSuchMethodError} on blueprint confirm.
 */
public class SurvivalBlueprintHandlers
{
    private static final Map<String, ISurvivalBlueprintHandler> handlers = new HashMap<>();

    private static ImmutableList<ISurvivalBlueprintHandler> survivalHandlers = ImmutableList.of();

    public static ISurvivalBlueprintHandler getHandler(final String id)
    {
        return handlers.get(id);
    }

    public static List<ISurvivalBlueprintHandler> getHandlers()
    {
        return survivalHandlers;
    }

    public static void registerHandler(final ISurvivalBlueprintHandler handler)
    {
        handlers.put(handler.getId(), handler);
        survivalHandlers = ImmutableList.copyOf(handlers.values());
    }

    /**
     * Matching overload used by Structurize blueprint manipulation UI (ClientLevel descriptor).
     */
    public static List<ISurvivalBlueprintHandler> getMatchingHandlers(
      final Blueprint blueprint,
      final ClientLevel level,
      final Player player,
      final BlockPos pos,
      final RotationMirror rotMir)
    {
        return getMatchingHandlers(blueprint, (Level) level, player, pos, rotMir);
    }

    /**
     * Level overload kept for any Level-typed call sites / binary compat.
     */
    public static List<ISurvivalBlueprintHandler> getMatchingHandlers(
      final Blueprint blueprint,
      final Level level,
      final Player player,
      final BlockPos pos,
      final RotationMirror rotMir)
    {
        final List<ISurvivalBlueprintHandler> matchingHandlers = new ArrayList<>();
        for (final ISurvivalBlueprintHandler handler : handlers.values())
        {
            if (handler.canHandle(blueprint, level, player, pos, rotMir))
            {
                matchingHandlers.add(handler);
            }
        }
        return matchingHandlers;
    }
}
