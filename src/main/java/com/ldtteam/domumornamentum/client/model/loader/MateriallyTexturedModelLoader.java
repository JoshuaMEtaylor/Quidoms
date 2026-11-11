package com.ldtteam.domumornamentum.client.model.loader;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.ldtteam.domumornamentum.client.model.MateriallyTexturedUnbakedModel;
import com.ldtteam.domumornamentum.util.Constants;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

/**
 * Registers {@code domum_ornamentum:materially_textured} for NeoForge 1.21.10
 * ({@link ModelEvent.RegisterLoaders} / {@link UnbakedModelLoader}).
 */
@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class MateriallyTexturedModelLoader implements UnbakedModelLoader<MateriallyTexturedUnbakedModel>
{
    public static final MateriallyTexturedModelLoader INSTANCE = new MateriallyTexturedModelLoader();

    private MateriallyTexturedModelLoader()
    {
    }

    @SubscribeEvent
    public static void onRegisterLoaders(final ModelEvent.RegisterLoaders event)
    {
        event.register(Constants.MATERIALLY_TEXTURED_MODEL_LOADER, INSTANCE);
    }

    @Override
    public MateriallyTexturedUnbakedModel read(final JsonObject jsonObject, final JsonDeserializationContext context) throws JsonParseException
    {
        if (!jsonObject.has("parent"))
        {
            throw new JsonParseException("materially_textured model requires a \"parent\" field");
        }
        final ResourceLocation parent = ResourceLocation.parse(jsonObject.get("parent").getAsString());
        return new MateriallyTexturedUnbakedModel(parent);
    }
}
