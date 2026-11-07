package com.minecolonies.api.sounds;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Queue of client-side sounds. Playback is invoked reflectively so this class's constant pool
 * never references {@code net.minecraft.client.*} (NeoForge no longer strips {@code @OnlyIn}).
 */
public class SoundManager
{
    private static final int MAX_CONCURRENT_SOUNDS = 5;
    private static final Map<UUID, Deque<TimedSound>> soundQueue = new HashMap<>();
    private static Method clientTick;

    public static void tick()
    {
        if (soundQueue.isEmpty())
        {
            return;
        }

        if (FMLEnvironment.getDist() != Dist.CLIENT)
        {
            soundQueue.clear();
            return;
        }

        try
        {
            if (clientTick == null)
            {
                clientTick = Class.forName("com.minecolonies.api.sounds.ClientSoundTicker")
                  .getMethod("tick", Map.class, int.class);
            }
            clientTick.invoke(null, soundQueue, MAX_CONCURRENT_SOUNDS);
        }
        catch (ReflectiveOperationException ex)
        {
            soundQueue.clear();
        }
    }

    public static void addToQueue(
      final UUID uuid,
      final SoundEvent soundEvent,
      final SoundSource source,
      final int repetitions,
      final int length,
      final BlockPos pos,
      final float volume,
      final float pitch)
    {
        final Deque<TimedSound> queue = soundQueue.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        queue.add(new TimedSound(soundEvent, source, repetitions, length, pos, volume, pitch));
    }

    public static class TimedSound
    {
        final SoundEvent  soundEvent;
        final SoundSource source;
        final int         length;
        final BlockPos    pos;
        final float       volume;
        final float       pitch;

        int repetitions;
        int timeout = 0;

        public TimedSound(final SoundEvent soundEvent, final SoundSource source, final int repetitions, final int length, final BlockPos pos, final float volume, final float pitch)
        {
            this.soundEvent = soundEvent;
            this.source = source;
            this.repetitions = repetitions;
            this.length = length;
            this.pos = pos;
            this.volume = volume;
            this.pitch = pitch;
        }
    }
}
