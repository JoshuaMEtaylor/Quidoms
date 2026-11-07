package com.minecolonies.api.client.render.modeltype;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;

/**
 * Citizen model (MC 1.21.10 HumanoidRenderState pipeline).
 */
public class CitizenModel<T extends CitizenRenderState> extends HumanoidModel<T>
{
    private static final String RENDER_META_WORKING = "working";

    public static boolean isItApril1st = false;

    public CitizenModel(final ModelPart part)
    {
        super(part, RenderType::entityCutoutNoCull);
    }

    @Override
    public void setupAnim(@NotNull final T state)
    {
        super.setupAnim(state);
        if (body.xRot == 0)
        {
            body.xRot = getActualRotation(state);
        }

        if (head.xRot == 0)
        {
            head.xRot = getActualRotation(state);
        }

        if (state.customTextureUUID != null)
        {
            head.visible = false;
            hat.visible = false;
        }
        else
        {
            head.visible = true;
            hat.visible = true;
        }

        if (isItApril1st)
        {
            switch (state.civilianId % 7)
            {
                case 0 -> leftArm.visible = false;
                case 1 -> rightArm.visible = false;
                case 2 -> body.visible = false;
                case 3 -> head.visible = false;
                case 4 -> hat.visible = false;
                case 5 -> leftLeg.visible = false;
                case 6 -> rightLeg.visible = false;
                default -> {
                }
            }
        }
    }

    public static LayerDefinition createMesh()
    {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    public float getActualRotation(@NotNull final T state)
    {
        return state.bodyRotationOverride;
    }

    public boolean isWorking(final T state)
    {
        return state.metaContains(RENDER_META_WORKING);
    }

    public boolean displayHat(final T state)
    {
        return state.displayHat();
    }
}
