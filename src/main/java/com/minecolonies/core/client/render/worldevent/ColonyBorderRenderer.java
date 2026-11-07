package com.minecolonies.core.client.render.worldevent;

/**
 * Colony border overlay — VertexBuffer API removed in 1.21.10 / new GPU buffer stack.
 * Visuals temporarily disabled pending GpuBuffer rewrite.
 */
public final class ColonyBorderRenderer
{
    private ColonyBorderRenderer() {}

    static void render(final WorldEventContext ctx)
    {
        // no-op until GpuBuffer port
    }

    static void cleanup()
    {
        // no-op
    }
}
