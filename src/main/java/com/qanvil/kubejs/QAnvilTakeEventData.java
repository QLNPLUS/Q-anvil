package com.qanvil.kubejs;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class QAnvilTakeEventData {
    public final ServerPlayer player;
    public final ItemStack input;
    public final ItemStack addition;
    public ItemStack output;

    public QAnvilTakeEventData(ServerPlayer player, ItemStack input, ItemStack addition, ItemStack output) {
        this.player = player;
        this.input = input.copy();
        this.addition = addition.copy();
        this.output = output.copy();
    }
}
