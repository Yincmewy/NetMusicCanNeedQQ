package yincmewy.netmusiccanneedqq.item;

import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;

public class PhoneMenu extends AbstractContainerMenu {
    public static final int CD_SLOT_X = 62;
    public static final int CD_SLOT_Y = 146;
    public static final int INV_X = 29;
    public static final int INV_Y = 182;
    public static final int HOTBAR_Y = 240;

    private final Container inputContainer = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };

    public PhoneMenu(int containerId, Inventory playerInv) {
        super(Netmusiccanneedqq.PHONE_MENU.get(), containerId);

        addSlot(new Slot(inputContainer, 0, CD_SLOT_X, CD_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ItemMusicCD || isPlaylistItem(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, INV_X + col * 18, HOTBAR_Y));
        }
    }

    public Container getInputContainer() {
        return inputContainer;
    }

    public ItemStack getInputStack() {
        return inputContainer.getItem(0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index == 0) {
            if (!moveItemStackTo(stack, 1, 37, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            clearContainer(player, inputContainer);
        }
    }

    private static boolean isPlaylistItem(ItemStack stack) {
        try {
            return Class.forName("com.gly091020.item.NetMusicListItem").isInstance(stack.getItem());
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
