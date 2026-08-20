package com.qanvil;

import com.qanvil.block.QAnvilBlock;
import com.qanvil.client.QAnvilClient;
import com.qanvil.menu.QAnvilMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(QAnvil.MOD_ID)
public final class QAnvil {
    public static final String MOD_ID = "qanvil";

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);

    public static final RegistryObject<Block> Q_ANVIL = BLOCKS.register(
            "q_anvil", () -> new QAnvilBlock(BlockBehaviour.Properties.copy(Blocks.ANVIL)));
    public static final RegistryObject<Item> Q_ANVIL_ITEM = ITEMS.register(
            "q_anvil", () -> new BlockItem(Q_ANVIL.get(), new Item.Properties()));
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<QAnvilMenu>> Q_ANVIL_MENU =
            MENUS.register("q_anvil", () -> IForgeMenuType.create(
                    (windowId, inventory, data) -> new QAnvilMenu(
                            windowId, inventory, net.minecraft.world.inventory.ContainerLevelAccess.NULL)));

    public QAnvil() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(QAnvil::addCreativeTabContents);
        modBus.addListener(QAnvilConfig::onConfigLoad);

        QAnvilConfig.register();
        MinecraftForge.EVENT_BUS.register(this);

        DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> modBus.addListener(QAnvilClient::registerScreens));
    }

    private static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(Q_ANVIL_ITEM);
        }
    }
}
