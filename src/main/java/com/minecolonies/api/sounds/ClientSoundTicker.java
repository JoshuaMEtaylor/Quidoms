package com.minecolonies.api.sounds;

import net.minecraft.client.Minecraft;

import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Client-only sound playback. Kept out of {@link SoundManager}'s constant pool so dedicated
 * server classloading does not pull in {@link Minecraft}.
 */
public final class ClientSoundTicker
{
    private ClientSoundTicker() {}

    public static void tick(final Map<UUID, Deque<SoundManager.TimedSound>> soundQueue, final int maxConcurrent)
    {
        if (Minecraft.getInstance().level == null)
        {
            soundQueue.clear();
            return;
        }

        int playedSounds = 1;
        for (final Map.Entry<UUID, Deque<SoundManager.TimedSound>> entries : soundQueue.entrySet())
        {
            if (entries.getValue().isEmpty())
            {
                continue;
            }
            final SoundManager.TimedSound instance = entries.getValue().peek();
            if (instance.timeout <= 0)
            {
                Minecraft.getInstance().player.level().playSound(Minecraft.getInstance().player,
                  instance.pos,
                  instance.soundEvent,
                  instance.source,
                  instance.volume,
                  instance.pitch);
                instance.timeout = instance.length;
                instance.repetitions--;
                if (instance.repetitions < 0)
                {
                    entries.getValue().pop();
                }
            }
            else
            {
                instance.timeout--;
            }
            playedSounds++;
            if (playedSounds >= maxConcurrent)
            {
                break;
            }
        }
    }
}
