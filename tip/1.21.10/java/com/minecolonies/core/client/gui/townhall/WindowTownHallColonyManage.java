package com.minecolonies.core.client.gui.townhall;

import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.controls.TextField;
import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.minecolonies.core.compatibility.quidcraft.QuidCraftKingdomBridge;
import com.minecolonies.core.network.messages.client.VanillaParticleMessage;
import com.minecolonies.core.network.messages.server.CreateColonyMessage;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;

import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;
import static com.minecolonies.api.util.constant.TranslationConstants.*;
import static com.minecolonies.api.util.constant.WindowConstants.*;

/**
 * Tip UI to found a new colony — shows founding price and always reports send errors.
 */
public class WindowTownHallColonyManage extends AbstractWindowSkeleton
{
    private static final String BUTTON_CREATE = "create";

    private final BlockPos pos;
    private final boolean reactivate;

    public WindowTownHallColonyManage(final BlockPos pos, final String closestName, final int closestDistance, final String preName, final boolean reactivate)
    {
        super(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "gui/townhall/windowcolonymanagement.xml"));
        this.pos = pos;
        this.reactivate = reactivate;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));

        registerButton(BUTTON_CANCEL, this::close);
        registerButton(BUTTON_CREATE, this::onCreate);

        this.findPaneOfTypeByID("colonyname", TextField.class).setText(preName.isEmpty() ? Component.translatable(DEFAULT_COLONY_NAME, mc.player.getName()).getString() : preName);
        this.findPaneOfTypeByID("text1", Text.class).setText(Component.translatable("com.minecolonies.core.settlementcovenant1", Math.max(13, Minecraft.getInstance().level.getGameTime() / TICKS_SECOND / 60 / 100)));
        if (closestDistance < 1000)
        {
            this.findPaneOfTypeByID("text3", Text.class).setText(Component.translatable("com.minecolonies.core.settlementcovenant3.hasclose", Component.literal(closestName).withStyle(ChatFormatting.RED), Component.literal(String.valueOf(closestDistance)).withStyle(ChatFormatting.RED)));
        }
        else
        {
            this.findPaneOfTypeByID("text3", Text.class).setText(Component.translatable("com.minecolonies.core.settlementcovenant3.noclose"));
        }

        final Text costPane = this.findPaneOfTypeByID("foundingcost", Text.class);
        if (costPane != null)
        {
            costPane.setText(Component.translatable(
              FOUNDING_COST_LABEL,
              QuidCraftKingdomBridge.formatFoundingCostPretty()).withStyle(ChatFormatting.DARK_GREEN));
        }
    }

    public void onCreate()
    {
        try
        {
            final String colonyName = this.findPaneOfTypeByID("colonyname", TextField.class).getText();
            if (colonyName == null || colonyName.trim().isEmpty())
            {
                mc.player.displayClientMessage(Component.literal("Enter a kingdom name before founding."), true);
                return;
            }

            try
            {
                // Tip 1.21.10 uses FLAME (DRAGON_BREATH is no longer SimpleParticleType).
                new VanillaParticleMessage(pos.getX(), pos.getY(), pos.getZ(), ParticleTypes.FLAME).onExecute(null, mc.player);
                Minecraft.getInstance().level.playSound(Minecraft.getInstance().player, Minecraft.getInstance().player.blockPosition(),
                  SoundEvents.CAMPFIRE_CRACKLE, SoundSource.AMBIENT, 2.5f, 0.8f);
            }
            catch (final Throwable ignored)
            {
                // Cosmetics must never block founding.
            }

            final BlockEntity entity = Minecraft.getInstance().level.getBlockEntity(pos);
            if (!(entity instanceof final TileEntityColonyBuilding hut))
            {
                mc.player.displayClientMessage(Component.literal("Town hall block is missing — cannot found here."), true);
                return;
            }

            // Prefer hut pack; selectedPack is often null after supply-camp placement and NPEs the click.
            String packName = "";
            if (hut.getStructurePack() != null)
            {
                packName = hut.getStructurePack().getName();
            }
            else if (!reactivate && StructurePacks.selectedPack != null)
            {
                packName = StructurePacks.selectedPack.getName();
            }

            String pathName = hut.getBlueprintPath();
            if (pathName == null)
            {
                pathName = "";
            }

            mc.player.displayClientMessage(
              Component.literal("Founding \"" + colonyName.trim() + "\" (fee " + QuidCraftKingdomBridge.formatFoundingCostPretty() + ")…"),
              true);

            new CreateColonyMessage(pos, reactivate, colonyName.trim(), packName, pathName).sendToServer();
            close();
        }
        catch (final Throwable ex)
        {
            System.err.println("Founding create click failed: " + ex);
            ex.printStackTrace();
            if (mc.player != null)
            {
                mc.player.displayClientMessage(Component.literal("Could not send founding request: " + ex.getClass().getSimpleName()), false);
            }
        }
    }
}
