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
    private static final int NAME_FIELD_Y = 20;
    private static final int NAME_FIELD_WIDTH = 110;
    private static final int NAME_FIELD_HEIGHT = 16;
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_ART_OFFSET_X = QAnvilMenu.GUI_ART_OFFSET_X;
    private static final int ERROR_ICON_U = PANEL_ART_OFFSET_X + PANEL_WIDTH;
    private static final ResourceLocation Q_ANVIL_GUI =
            new ResourceLocation(QAnvil.MOD_ID, "textures/gui/q_anvil.png");
    private static final ResourceLocation Q_ANVIL_INPUT_ACTIVE =
            new ResourceLocation(QAnvil.MOD_ID, "textures/gui/q_anvil_input_active.png");
    private static final ResourceLocation Q_ANVIL_INPUT_INACTIVE =
            new ResourceLocation(QAnvil.MOD_ID, "textures/gui/q_anvil_input_inactive.png");

    public QAnvilScreen(AnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // The base texture stores every panel element PANEL_ART_OFFSET_X pixels to the
        // right, which leaves room for the armor column on the left edge of the panel.
        // Drawing the art that far left keeps the vanilla anvil slot, label, and name
        // field coordinates unchanged. The error icon sits right of the panel art, so
        // it is blitted separately and stays out of this call.
        graphics.blit(Q_ANVIL_GUI,
                this.leftPos - PANEL_ART_OFFSET_X,
                this.topPos,
                0, 0,
                PANEL_ART_OFFSET_X + PANEL_WIDTH,
                this.imageHeight);

        QAnvilMenu qAnvilMenu = (QAnvilMenu) this.menu;
        ResourceLocation inputTexture = qAnvilMenu.getSlot(0).hasItem()
                ? Q_ANVIL_INPUT_ACTIVE
                : Q_ANVIL_INPUT_INACTIVE;
        graphics.blit(inputTexture,
                this.leftPos + NAME_FIELD_X,
                this.topPos + NAME_FIELD_Y,
                0, 0,
                NAME_FIELD_WIDTH,
                NAME_FIELD_HEIGHT,
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

        graphics.blit(Q_ANVIL_GUI, x + 99, y + 45, ERROR_ICON_U, 0, 28, 21);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        QAnvilMenu qAnvilMenu = (QAnvilMenu) this.menu;
        String customTitle = qAnvilMenu.getPromptText();
        Component title = hasCustomTitle(customTitle)
                ? Component.literal(customTitle)
                : this.title;
        graphics.drawString(this.font, title, this.titleLabelX, this.titleLabelY, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

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

    private static boolean hasCustomTitle(String text) {
        if (text == null || text.isEmpty()
                || "undefined".equalsIgnoreCase(text) || "null".equalsIgnoreCase(text)) {
            return false;
        }
        return true;
    }
}
