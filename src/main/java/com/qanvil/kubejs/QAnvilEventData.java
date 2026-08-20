package com.qanvil.kubejs;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class QAnvilEventData {
    public final ServerPlayer player;
    public final ItemStack input;
    public final ItemStack addition;
    public final ItemStack originalOutput;
    public final int vanillaLevelCost;
    public ItemStack output;
    public int cost;
    public int materialCost;
    public boolean materialCostSet;
    public String currencyId;
    public double currencyCost;
    public double healthCost;

    public QAnvilEventData(ServerPlayer player, ItemStack input, ItemStack addition, ItemStack originalOutput,
                           int vanillaLevelCost, String currencyId, double currencyCost, double healthCost) {
        this.player = player;
        this.input = input.copy();
        this.addition = addition.copy();
        this.originalOutput = originalOutput.copy();
        this.vanillaLevelCost = vanillaLevelCost;
        this.cost = vanillaLevelCost;
        this.materialCost = 0;
        this.materialCostSet = false;
        this.output = originalOutput.copy();
        this.currencyId = currencyId;
        this.currencyCost = currencyCost;
        this.healthCost = healthCost;
    }
}
