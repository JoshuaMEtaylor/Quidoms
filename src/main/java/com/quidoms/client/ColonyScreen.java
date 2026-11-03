package com.quidoms.client;

import java.util.List;

import com.quidoms.network.ColonyActionC2S;
import com.quidoms.network.OpenColonyScreenS2C;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Modern colony overview — flat panels inspired by QuidCraft chrome, not BlockUI.
 */
public class ColonyScreen extends Screen {
    private static final int SCRIM = 0x99101216;
    private static final int PANEL = 0xF01B2027;
    private static final int RAISED = 0xF0262C34;
    private static final int BORDER = 0xFF3E4652;
    private static final int ACCENT = 0xFF1FA971;
    private static final int TEXT = 0xFFF4F1EA;
    private static final int MUTED = 0xFFB7B1A8;

    private final OpenColonyScreenS2C data;
    private String tab = "overview";

    public ColonyScreen(OpenColonyScreenS2C data) {
        super(Component.translatable("quidoms.screen.colony"));
        this.data = data;
    }

    @Override
    protected void init() {
        int panelX = (width - 320) / 2;
        int panelY = (height - 220) / 2;
        addRenderableWidget(Button.builder(Component.translatable("quidoms.tab.overview"), b -> tab = "overview")
                .bounds(panelX + 8, panelY + 28, 72, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("quidoms.tab.citizens"), b -> tab = "citizens")
                .bounds(panelX + 84, panelY + 28, 72, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("quidoms.tab.buildings"), b -> tab = "buildings")
                .bounds(panelX + 160, panelY + 28, 72, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("quidoms.tab.build"), b -> tab = "build")
                .bounds(panelX + 236, panelY + 28, 72, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("quidoms.action.start_pavilion"), b -> startBuild())
                .bounds(panelX + 8, panelY + 190, 140, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("quidoms.action.refresh"), b -> refresh())
                .bounds(panelX + 156, panelY + 190, 72, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("quidoms.action.close"), b -> onClose())
                .bounds(panelX + 236, panelY + 190, 72, 20).build());
    }

    private void startBuild() {
        BlockPos origin = minecraft != null && minecraft.player != null
                ? minecraft.player.blockPosition()
                : BlockPos.ZERO;
        send(new ColonyActionC2S(data.colonyId(), "start_build", "quidoms:wood_pavilion", origin));
    }

    private void refresh() {
        send(new ColonyActionC2S(data.colonyId(), "refresh", "", BlockPos.ZERO));
    }

    private static void send(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, SCRIM);
        int panelX = (width - 320) / 2;
        int panelY = (height - 220) / 2;
        fillPanel(graphics, panelX, panelY, 320, 220, PANEL);
        graphics.fill(panelX, panelY, panelX + 320, panelY + 3, ACCENT);
        graphics.drawString(font, Component.literal("Quidoms"), panelX + 10, panelY + 8, ACCENT, false);
        graphics.drawString(font, Component.literal(data.name()), panelX + 80, panelY + 8, TEXT, false);

        int contentY = panelY + 54;
        fillPanel(graphics, panelX + 8, contentY, 304, 128, RAISED);

        switch (tab) {
            case "citizens" -> drawLines(graphics, data.citizenLines(), panelX + 14, contentY + 8);
            case "buildings" -> drawLines(graphics, data.buildingLines(), panelX + 14, contentY + 8);
            case "build" -> {
                drawLines(graphics, data.workOrderLines(), panelX + 14, contentY + 8);
                graphics.drawString(font, Component.translatable("quidoms.label.materials"), panelX + 14, contentY + 64, MUTED, false);
                drawLines(graphics, data.bomLines(), panelX + 14, contentY + 76);
            }
            default -> {
                graphics.drawString(font, Component.translatable("quidoms.label.citizens", data.citizenCount(), data.housingCap()),
                        panelX + 14, contentY + 10, TEXT, false);
                graphics.drawString(font, Component.translatable("quidoms.label.buildings", data.buildingLines().size()),
                        panelX + 14, contentY + 24, TEXT, false);
                graphics.drawString(font, Component.translatable("quidoms.label.orders", data.workOrderLines().size()),
                        panelX + 14, contentY + 38, TEXT, false);
                graphics.drawString(font, Component.translatable("quidoms.label.hint"),
                        panelX + 14, contentY + 60, MUTED, false);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawLines(GuiGraphics graphics, List<String> lines, int x, int y) {
        int row = 0;
        for (String line : lines) {
            if (row >= 8) {
                break;
            }
            graphics.drawString(font, Component.literal(line), x, y + row * 12, TEXT, false);
            row++;
        }
        if (lines.isEmpty()) {
            graphics.drawString(font, Component.translatable("quidoms.label.empty"), x, y, MUTED, false);
        }
    }

    private static void fillPanel(GuiGraphics graphics, int x, int y, int w, int h, int fill) {
        graphics.fill(x, y, x + w, y + h, fill);
        graphics.fill(x, y, x + w, y + 1, BORDER);
        graphics.fill(x, y + h - 1, x + w, y + h, BORDER);
        graphics.fill(x, y, x + 1, y + h, BORDER);
        graphics.fill(x + w - 1, y, x + w, y + h, BORDER);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
