package yincmewy.netmusiccanneedqq.qq;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class QqDiscNbt {
    public static final String ROOT = "NetMusicCanNeedQQ";
    private static final String PROVIDER = "provider";
    private static final String PROVIDER_QQ = "qq";
    private static final String QQ_INPUT = "qq_input";

    private QqDiscNbt() {
    }

    public static void markQq(ItemStack stack, String input) {
        if (stack.isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag tag = root.contains(ROOT, Tag.TAG_COMPOUND) ? root.getCompound(ROOT) : new CompoundTag();
            tag.putString(PROVIDER, PROVIDER_QQ);
            if (input != null && !input.isBlank()) {
                tag.putString(QQ_INPUT, input);
            }
            root.put(ROOT, tag);
        });
    }

    public static void clear(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            if (root.contains(ROOT, Tag.TAG_COMPOUND)) {
                root.remove(ROOT);
            }
        });
    }

    public static boolean isQqDisc(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(ROOT, Tag.TAG_COMPOUND)) {
            return false;
        }
        CompoundTag tag = root.getCompound(ROOT);
        if (tag.contains(PROVIDER, Tag.TAG_STRING)) {
            return PROVIDER_QQ.equalsIgnoreCase(tag.getString(PROVIDER));
        }
        return tag.contains(QQ_INPUT, Tag.TAG_STRING);
    }

    public static String getQqInput(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(ROOT, Tag.TAG_COMPOUND)) {
            return "";
        }
        CompoundTag tag = root.getCompound(ROOT);
        return tag.getString(QQ_INPUT);
    }
}
