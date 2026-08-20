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
    private static final String PROTOCOL_VERSION = "1";
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
            int count = Math.max(0, Math.min(999, buffer.readVarInt()));
            ItemStack stack = new ItemStack(item, count);
            stack.setTag(buffer.readNbt());
            return stack;
        }
    }
}
