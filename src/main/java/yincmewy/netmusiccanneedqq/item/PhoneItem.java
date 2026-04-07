package yincmewy.netmusiccanneedqq.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PhoneItem extends Item {
    public PhoneItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(this::createMenu,
                    Component.translatable("item.netmusiccanneedqq.phone"));
            serverPlayer.openMenu(provider);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PhoneMenu(containerId, inventory);
    }
}
