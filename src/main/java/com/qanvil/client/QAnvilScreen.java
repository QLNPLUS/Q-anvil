package com.qanvil.client;

import com.qanvil.QAnvil;
import com.qanvil.menu.QAnvilMenu;
import com.qanvil.currency.QAnvilCosts;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;

public final class QAnvilScreen extends AnvilScreen {
    private static final int AFFORDABLE_COLOR = 0x80FF20;
    private static final int UNAFFORDABLE_COLOR = 0xFF6060;
    private static final int NAME_FIELD_X = 59;
    private static final int NAME_FIELD_Y = 21;
    private static final int NAME_FIELD_WIDTH = 110;
    private static final int NAME_FIELD_HEIGHT = 15;
    private static final ResourceLocation Q_ANVIL_GUI =
            new ResourceLocation(QAnvil.MOD_ID, "textures/gui/q_anvil.png");

    public QAnvilScreen(AnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(Q_ANVIL_GUI, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        QAnvilMenu qAnvilMenu = (QAnvilMenu) this.menu;
        if (qAnvilMenu.getSlot(0).hasItem()) {
            renderActiveNameField(graphics);
        }
    }

    private void renderActiveNameField(GuiGraphics graphics) {
        // The supplied GUI texture uses the red rectangle for the inactive state.
        // Copy the panel material for the active state; the inherited EditBox is
        // rendered later and keeps its text, cursor, and focus border on top.
        graphics.blit(Q_ANVIL_GUI,
                this.leftPos + NAME_FIELD_X,
                this.topPos + NAME_FIELD_Y,
                64, 0,
                NAME_FIELD_WIDTH,
                NAME_FIELD_HEIGHT);
    }

    @Override
    protected void renderErrorIcon(GuiGraphics graphics, int x, int y) {
        QAnvilMenu qAnvilMenu = (QAnvilMenu) this.menu;
        if ((!qAnvilMenu.getSlot(0).hasItem() && !qAnvilMenu.getSlot(1).hasItem())
                || qAnvilMenu.getSlot(qAnvilMenu.getResultSlot()).hasItem()) {
            return;
        }

        graphics.blit(Q_ANVIL_GUI, x + 99, y + 45, this.imageWidth, 0, 28, 21);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        QAnvilMenu qAnvilMenu = (QAnvilMenu) this.menu;
        if (!qAnvilMenu.hasQAnvilResult()) {
            return;
        }

        boolean showCurrency = qAnvilMenu.getCurrencyCost() > 0.0D;
        boolean showHealth = qAnvilMenu.getHealthCost() > 0.0D;
        Component costText;
        if (showCurrency && showHealth) {
            costText = Component.translatable(
                    "gui.qanvil.cost",
                    QAnvilCosts.formatCurrency(qAnvilMenu.getCurrencyCost()),
                    qAnvilMenu.getCurrencyDisplayName(),
                    String.format(java.util.Locale.ROOT, "%.1f", qAnvilMenu.getHealthCost()));
        } else if (showCurrency) {
            costText = Component.translatable(
                    "gui.qanvil.currency_cost",
                    QAnvilCosts.formatCurrency(qAnvilMenu.getCurrencyCost()),
                    qAnvilMenu.getCurrencyDisplayName());
        } else if (showHealth) {
            costText = Component.translatable(
                    "gui.qanvil.health_cost",
                    String.format(java.util.Locale.ROOT, "%.1f", qAnvilMenu.getHealthCost()));
        } else {
            return;
        }

        int color = qAnvilMenu.canAfford(this.minecraft.player)
                ? AFFORDABLE_COLOR
                : UNAFFORDABLE_COLOR;
        int x = Math.max(2, this.imageWidth - 8 - this.font.width(costText));
        graphics.fill(x - 2, 67, this.imageWidth - 8, 79, 1325400064);
        graphics.drawString(this.font, costText, x, 69, color, false);
    }
}
