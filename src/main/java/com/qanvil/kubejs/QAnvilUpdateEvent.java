package com.qanvil.kubejs;

import com.qanvil.currency.QAnvilCosts;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.item.ItemStackJS;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

public final class QAnvilUpdateEvent extends EventJS {
    private final ServerPlayer player;
    private final ItemStack input;
    private final ItemStack addition;
    private final ItemStack originalOutput;
    private final int vanillaLevelCost;
    private String currencyId;
    private ItemStack output;
    private int cost;
    private int materialCost;
    private boolean materialCostSet;
    private double currencyCost;
    private double healthCost;
    private boolean currencyCostSet;
    private boolean healthCostSet;

    public QAnvilUpdateEvent() {
        this(null, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, 0, "", 0.0D, 0.0D);
    }

    public QAnvilUpdateEvent(QAnvilEventData data) {
        this(data.player, data.input, data.addition, data.originalOutput, data.vanillaLevelCost,
                data.currencyId, data.currencyCost, data.healthCost);
    }

    public QAnvilUpdateEvent(ServerPlayer player, ItemStack input, ItemStack addition,
                             ItemStack originalOutput, int vanillaLevelCost, String currencyId,
                             double currencyCost, double healthCost) {
        this.player = player;
        this.input = input.copy();
        this.addition = addition.copy();
        this.originalOutput = originalOutput.copy();
        this.vanillaLevelCost = vanillaLevelCost;
        this.cost = vanillaLevelCost;
        this.materialCost = 0;
        this.currencyId = currencyId;
        this.output = originalOutput.copy();
        this.currencyCost = currencyCost;
        this.healthCost = healthCost;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public String getPlayerName() {
        return player == null ? "" : player.getGameProfile().getName();
    }

    public ItemStack getInput() {
        return input.copy();
    }

    public ItemStack getLeft() {
        return getInput();
    }

    public ItemStack getAddition() {
        return addition.copy();
    }

    public ItemStack getRight() {
        return getAddition();
    }

    public ItemStack getOriginalOutput() {
        return originalOutput.copy();
    }

    public ItemStack getOutput() {
        return output.copy();
    }

    public void setOutput(Object output) {
        this.output = ItemStackJS.of(output).copy();
    }

    public int getVanillaLevelCost() {
        return vanillaLevelCost;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = Math.max(0, cost);
        if (!currencyCostSet && !healthCostSet) {
            applyLegacyCost(this.cost);
        }
    }

    public int getMaterialCost() {
        return materialCost;
    }

    public void setMaterialCost(int materialCost) {
        this.materialCost = Math.max(0, materialCost);
        this.materialCostSet = true;
    }

    public boolean isMaterialCostSet() {
        return materialCostSet;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(String currencyId) {
        if (currencyId != null && !currencyId.isBlank()) {
            this.currencyId = currencyId;
        }
    }

    public double getCurrencyCost() {
        return currencyCost;
    }

    public void setCurrencyCost(double currencyCost) {
        if (!currencyCostSet && !healthCostSet) {
            this.healthCost = 0.0D;
        }
        this.currencyCost = sanitizeCost(currencyCost);
        this.currencyCostSet = true;
    }

    public double getHealthCost() {
        return healthCost;
    }

    public void setHealthCost(double healthCost) {
        if (!currencyCostSet && !healthCostSet) {
            this.currencyCost = 0.0D;
        }
        this.healthCost = sanitizeCost(healthCost);
        this.healthCostSet = true;
    }

    private void applyLegacyCost(int cost) {
        double amount = cost;
        this.currencyCost = QAnvilCosts.usesCurrency() ? amount : 0.0D;
        this.healthCost = QAnvilCosts.usesHealth() ? amount : 0.0D;
    }

    private static double sanitizeCost(double cost) {
        return Double.isFinite(cost) ? Math.max(0.0D, cost) : 0.0D;
    }
}
