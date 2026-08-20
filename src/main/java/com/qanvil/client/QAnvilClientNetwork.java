package com.qanvil.client;

import com.qanvil.network.QAnvilNetwork.QAnvilLargeStackSyncPacket;
import com.qanvil.menu.QAnvilMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class QAnvilClientNetwork {
    private QAnvilClientNetwork() {
    }

    public static void handle(QAnvilLargeStackSyncPacket packet) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !(player.containerMenu instanceof QAnvilMenu menu)
                || menu.containerId != packet.containerId()) {
            return;
        }

        if (packet.slot() == -1) {
            menu.setCarried(packet.stack());
        } else if (packet.slot() >= 0 && packet.slot() < menu.slots.size()) {
            menu.setItem(packet.slot(), packet.stateId(), packet.stack());
        }
    }
}
