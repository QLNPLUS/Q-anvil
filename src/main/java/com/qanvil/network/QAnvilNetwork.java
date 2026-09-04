package com.qanvil.network;

import com.qanvil.QAnvil;
import com.qanvil.client.QAnvilClientNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class QAnvilNetwork {
    private static final String PROTOCOL_VERSION = "3";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(QAnvil.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);
    private static int nextMessageId;

    private QAnvilNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(nextMessageId++, QAnvilLargeStackSyncPacket.class,
                QAnvilLargeStackSyncPacket::encode,
                QAnvilLargeStackSyncPacket::decode,
                QAnvilLargeStackSyncPacket::handle);
        CHANNEL.registerMessage(nextMessageId++, QAnvilPromptTextSyncPacket.class,
                QAnvilPromptTextSyncPacket::encode,
                QAnvilPromptTextSyncPacket::decode,
                QAnvilPromptTextSyncPacket::handle);
        CHANNEL.registerMessage(nextMessageId++, QAnvilCurrencyInfoSyncPacket.class,
                QAnvilCurrencyInfoSyncPacket::encode,
                QAnvilCurrencyInfoSyncPacket::decode,
                QAnvilCurrencyInfoSyncPacket::handle);
    }

    public static void sendLargeStack(ServerPlayer player, int containerId, int stateId,
                                      int slot, ItemStack stack) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new QAnvilLargeStackSyncPacket(containerId, stateId, slot, stack));
    }

    public static final class QAnvilLargeStackSyncPacket {
        private final int containerId;
        private final int stateId;
        private final int slot;
        private final ItemStack stack;

        public QAnvilLargeStackSyncPacket(int containerId, int stateId, int slot, ItemStack stack) {
            this.containerId = containerId;
            this.stateId = stateId;
            this.slot = slot;
            this.stack = stack.copy();
        }

        private static void encode(QAnvilLargeStackSyncPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.containerId);
            buffer.writeVarInt(packet.stateId);
            buffer.writeVarInt(packet.slot);
            writeStack(buffer, packet.stack);
        }

        private static QAnvilLargeStackSyncPacket decode(FriendlyByteBuf buffer) {
            return new QAnvilLargeStackSyncPacket(
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    readStack(buffer));
        }

        private static void handle(QAnvilLargeStackSyncPacket packet,
                                   Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT, () -> () -> QAnvilClientNetwork.handle(packet)));
            context.setPacketHandled(true);
        }

        public int containerId() {
            return containerId;
        }

        public int stateId() {
            return stateId;
        }

        public int slot() {
            return slot;
        }

        public ItemStack stack() {
            return stack.copy();
        }

        private static void writeStack(FriendlyByteBuf buffer, ItemStack stack) {
            if (stack.isEmpty()) {
                buffer.writeBoolean(false);
                return;
            }

            buffer.writeBoolean(true);
            buffer.writeVarInt(BuiltInRegistries.ITEM.getId(stack.getItem()));
            buffer.writeVarInt(stack.getCount());
            buffer.writeNbt(stack.getTag());
        }

        private static ItemStack readStack(FriendlyByteBuf buffer) {
            if (!buffer.readBoolean()) {
                return ItemStack.EMPTY;
            }

            Item item = BuiltInRegistries.ITEM.byId(buffer.readVarInt());
            int count = Math.max(0, Math.min(9999, buffer.readVarInt()));
            ItemStack stack = new ItemStack(item, count);
            stack.setTag(buffer.readNbt());
            return stack;
        }
    }

    public static void sendPromptText(ServerPlayer player, int containerId, int stateId, String text) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new QAnvilPromptTextSyncPacket(containerId, stateId, text));
    }

    public static final class QAnvilPromptTextSyncPacket {
        private final int containerId;
        private final int stateId;
        private final String text;

        public QAnvilPromptTextSyncPacket(int containerId, int stateId, String text) {
            this.containerId = containerId;
            this.stateId = stateId;
            this.text = text == null ? "" : text;
        }

        private static void encode(QAnvilPromptTextSyncPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.containerId);
            buffer.writeVarInt(packet.stateId);
            buffer.writeUtf(packet.text, 256);
        }

        private static QAnvilPromptTextSyncPacket decode(FriendlyByteBuf buffer) {
            return new QAnvilPromptTextSyncPacket(
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readUtf(256));
        }

        private static void handle(QAnvilPromptTextSyncPacket packet,
                                   Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT, () -> () -> QAnvilClientNetwork.handle(packet)));
            context.setPacketHandled(true);
        }

        public int containerId() {
            return containerId;
        }

        public int stateId() {
            return stateId;
        }

        public String text() {
            return text;
        }
    }

    public static void sendCurrencyInfo(ServerPlayer player, int containerId, int stateId,
                                        String currencyId, String displayName) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new QAnvilCurrencyInfoSyncPacket(containerId, stateId, currencyId, displayName));
    }

    public static final class QAnvilCurrencyInfoSyncPacket {
        private final int containerId;
        private final int stateId;
        private final String currencyId;
        private final String displayName;

        public QAnvilCurrencyInfoSyncPacket(int containerId, int stateId,
                                            String currencyId, String displayName) {
            this.containerId = containerId;
            this.stateId = stateId;
            this.currencyId = currencyId == null ? "" : currencyId;
            this.displayName = displayName == null ? this.currencyId : displayName;
        }

        private static void encode(QAnvilCurrencyInfoSyncPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.containerId);
            buffer.writeVarInt(packet.stateId);
            buffer.writeUtf(packet.currencyId, 64);
            buffer.writeUtf(packet.displayName, 256);
        }

        private static QAnvilCurrencyInfoSyncPacket decode(FriendlyByteBuf buffer) {
            return new QAnvilCurrencyInfoSyncPacket(
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readUtf(64),
                    buffer.readUtf(256));
        }

        private static void handle(QAnvilCurrencyInfoSyncPacket packet,
                                   Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT, () -> () -> QAnvilClientNetwork.handle(packet)));
            context.setPacketHandled(true);
        }

        public int containerId() {
            return containerId;
        }

        public int stateId() {
            return stateId;
        }

        public String currencyId() {
            return currencyId;
        }

        public String displayName() {
            return displayName;
        }
    }
}
