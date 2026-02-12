package yincmewy.netmusiccanneedqq.network;

import com.github.tartaricacid.netmusic.inventory.CDBurnerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import yincmewy.netmusiccanneedqq.qq.QqDiscNbt;

import java.util.function.Supplier;

public class ClearQqDiscMessage {
    public static ClearQqDiscMessage decode(FriendlyByteBuf buf) {
        return new ClearQqDiscMessage();
    }

    public static void encode(ClearQqDiscMessage message, FriendlyByteBuf buf) {
    }

    public static void handle(ClearQqDiscMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
                        QqDiscNbt.clear(stack);
                    }
                }
            });
        }
        context.setPacketHandled(true);
    }
}
