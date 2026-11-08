package com.minecolonies.core.client.gui.containers;

import com.minecolonies.api.colony.ICitizen;
import com.minecolonies.api.inventory.container.ContainerCitizenInventory;
import com.minecolonies.api.util.constant.Constants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Citizen inventory container screen — includes a 3D entity preview (vanilla player body).
 */
public class WindowCitizenInventory extends AbstractContainerScreen<ContainerCitizenInventory>
{
    /**
     * Texture res loc.
     */
    private static final ResourceLocation TEXT = new ResourceLocation(Constants.MOD_ID, "textures/gui/citizen_container.png");

    /**
     * Offset inside the texture to use.
     */
    private static final int TEXTURE_OFFSET = 130;

    /**
     * Offset of each slot.
     */
    private static final int SLOT_OFFSET = 18;

    /**
     * Size of the custom texture.
     */
    private static final int TEXTURE_SIZE = 350;

    /**
     * Offet of the screen for the texture.
     */
    private static final int TEXTURE_HEIGHT = 96;

    /**
     * General y offset.
     */
    private static final int Y_OFFSET = 114;

    /**
     * Amount of slots each row.
     */
    private static final int SLOTS_EACH_ROW = 9;

    /**
     * Preview box inside the citizen panel (matches texture blit at 172,22 size 49x72).
     */
    private static final int PREVIEW_X = 172;
    private static final int PREVIEW_Y = 22;
    private static final int PREVIEW_W = 49;
    private static final int PREVIEW_H = 72;
    private static final int PREVIEW_SCALE = 30;

    /**
     * Current active citizen inventory window
     */
    public static WindowCitizenInventory activeCitizenInventory = null;

    /**
     * Citizen of this UI
     */
    private ICitizen citizenData;

    /**
     * window height is calculated with these values; the more rows, the heigher
     */
    private final int inventoryRows;

    public WindowCitizenInventory(final ContainerCitizenInventory container, final Inventory playerInventory, final Component iTextComponent)
    {
        super(container, playerInventory, iTextComponent);
        this.inventoryRows = (container.getItems().size() - 36) / 9;

        this.imageHeight = Y_OFFSET + Math.min(SLOTS_EACH_ROW, this.inventoryRows) * SLOT_OFFSET;
        this.imageWidth = 245;
        activeCitizenInventory = this;
        citizenData = container.getCitizenData();
    }

    @Override
    public void render(@NotNull final GuiGraphics stack, int x, int y, float z)
    {
        super.render(stack, x, y, z);
        this.renderTooltip(stack, x, y);
    }

    /**
     * Draw the foreground layer for the GuiContainer (everything in front of the items)
     */
    @Override
    protected void renderLabels(@NotNull final GuiGraphics stack, final int mouseX, final int mouseY)
    {
        stack.drawString(this.font, this.menu.getDisplayName(), 80, 9, 4210752, false);
        stack.drawString(this.font, this.playerInventoryTitle.getString(), 8, 25 + this.inventoryRows * SLOT_OFFSET, 4210752, false);
    }

    /**
     * Draws the background layer of this container (behind the items).
     */
    @Override
    protected void renderBg(@NotNull final GuiGraphics stack, float partialTicks, int mouseX, int mouseY)
    {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        stack.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXT, i, j, 0f, 0f, this.imageWidth, 10 + this.inventoryRows * SLOT_OFFSET + 12, TEXTURE_SIZE, TEXTURE_SIZE);

        stack.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXT, i, j + 10 + this.inventoryRows * SLOT_OFFSET + 12, 0f, (float) (TEXTURE_OFFSET), this.imageWidth, TEXTURE_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

        stack.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXT, i + PREVIEW_X, j + PREVIEW_Y, 0f, 227f, PREVIEW_W, PREVIEW_H, TEXTURE_SIZE, TEXTURE_SIZE);

        for (int index = 0; index < 4; index++)
        {
            stack.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXT, i + 222, j + 22 + index * 18, 0f, 300f, 18, 18, TEXTURE_SIZE, TEXTURE_SIZE);
        }

        this.menu.getEntity().ifPresent(entity -> {
            if (entity instanceof LivingEntity living)
            {
                // 1.21.10 InventoryScreen API: scissor rect + scale + mouse follow.
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                  stack,
                  i + PREVIEW_X,
                  j + PREVIEW_Y,
                  i + PREVIEW_X + PREVIEW_W,
                  j + PREVIEW_Y + PREVIEW_H,
                  PREVIEW_SCALE,
                  0.0625F,
                  (float) mouseX,
                  (float) mouseY,
                  living);
            }
        });
    }

    @Override
    public void onClose()
    {
        activeCitizenInventory = null;
        super.onClose();
    }

    /**
     * Get the citizen for this UI
     *
     * @return
     */
    public ICitizen getCitizenData()
    {
        return citizenData;
    }
}
