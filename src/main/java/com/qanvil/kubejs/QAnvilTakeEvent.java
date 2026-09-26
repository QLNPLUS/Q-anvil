package com.qanvil.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.item.ItemStackJS;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class QAnvilTakeEvent extends EventJS {
    public final ServerPlayer player;
    private final ItemStack input;
    private final ItemStack addition;
    private ItemStack output;
    private boolean canceled;

    public QAnvilTakeEvent() {
        this(null, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
    }

    public QAnvilTakeEvent(QAnvilTakeEventData data) {
        this(data.player, data.input, data.addition, data.output);
    }

    private QAnvilTakeEvent(ServerPlayer player, ItemStack input, ItemStack addition, ItemStack output) {
        this.player = player;
        this.input = input.copy();
        this.addition = addition.copy();
        this.output = output.copy();
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

    public ItemStack getOutput() {
        return output.copy();
    }

    public void setOutput(Object output) {
        this.output = ItemStackJS.of(output).copy();
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
