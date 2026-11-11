package com.minecolonies.core.commands;

import net.minecraft.network.chat.ClickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Small utility class to run an executable on a chat click event.
 * Implements ClickEvent (1.21.10+ interface) and triggers runnables when action() is queried.
 */
public class ClickEventWithExecutable implements ClickEvent
{
    private Runnable[] actions;

    public ClickEventWithExecutable(@NotNull final Runnable... actions)
    {
        this.actions = actions;
    }

    @Override
    @NotNull
    public Action action()
    {
        if (actions != null)
        {
            for (Runnable r : actions)
            {
                r.run();
            }
            actions = null;
        }
        return Action.RUN_COMMAND;
    }
}
