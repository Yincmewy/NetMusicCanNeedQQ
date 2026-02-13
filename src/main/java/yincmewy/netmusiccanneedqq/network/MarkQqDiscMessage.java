package yincmewy.netmusiccanneedqq.network;

import com.github.tartaricacid.netmusic.inventory.CDBurnerMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;
import yincmewy.netmusiccanneedqq.qq.QqDiscNbt;

public record MarkQqDiscMessage(String qqInput) implements CustomPacketPayload {
    public static final Type<MarkQqDiscMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Netmusiccanneedqq.MODID, "mark_qq_disc"));
    public static final StreamCodec<ByteBuf, MarkQqDiscMessage> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, MarkQqDiscMessage::qqInput, MarkQqDiscMessage::new);

    public MarkQqDiscMessage {
        qqInput = qqInput == null ? "" : qqInput;
    }

    public static void handle(MarkQqDiscMessage message, IPayloadContext context) {
        if (!context.flow().isServerbound()) {
            return;
        }
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) {
                return;
            }
            if (sender.containerMenu instanceof CDBurnerMenu menu) {
                ItemStack stack = menu.getInput().getStackInSlot(0);
                if (!stack.isEmpty()) {
                    QqDiscNbt.markQq(stack, message.qqInput());
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
