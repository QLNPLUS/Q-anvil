package com.qanvil.client;

import com.qanvil.QAnvil;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class QAnvilClient {
    private QAnvilClient() {
    }

    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(QAnvil.Q_ANVIL_MENU.get(), QAnvilScreen::new));
    }
}
