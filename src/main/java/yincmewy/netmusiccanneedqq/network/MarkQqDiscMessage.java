package yincmewy.netmusiccanneedqq.network;

import com.github.tartaricacid.netmusic.inventory.CDBurnerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import yincmewy.netmusiccanneedqq.config.QualityLevel;
import yincmewy.netmusiccanneedqq.qq.QqDiscNbt;
import yincmewy.netmusiccanneedqq.qq.QqMusicUpdater;

import java.util.function.Supplier;

public class MarkQqDiscMessage {
    private final String qqInput;
    private final QualityLevel quality;

    public MarkQqDiscMessage(String qqInput, QualityLevel quality) {
        this.qqInput = qqInput;
        this.quality = quality != null ? quality : QualityLevel.HIGH;
    }

    public static MarkQqDiscMessage decode(FriendlyByteBuf buf) {
        String input = buf.readUtf(32767);
        QualityLevel quality;
        try {
            quality = buf.readEnum(QualityLevel.class);
        } catch (Exception e) {
            quality = QualityLevel.HIGH;
        }
        return new MarkQqDiscMessage(input, quality);
    }

    public static void encode(MarkQqDiscMessage message, FriendlyByteBuf buf) {
        buf.writeUtf(message.qqInput == null ? "" : message.qqInput);
        buf.writeEnum(message.quality);
    }

    public static void handle(MarkQqDiscMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender == null) {
                    return;
                }
                if (sender.containerMenu instanceof CDBurnerMenu menu) {
                    ItemStack stack = menu.getInput().getStackInSlot(0);
                    if (!stack.isEmpty()) {
                        QqDiscNbt.markQq(stack, message.qqInput, message.quality);
                        QqMusicUpdater.prefetch(message.qqInput, message.quality);
                    }
                }
            });
        }
        context.setPacketHandled(true);
    }
}
