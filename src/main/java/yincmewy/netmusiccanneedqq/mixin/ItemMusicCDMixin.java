package yincmewy.netmusiccanneedqq.mixin;

import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yincmewy.netmusiccanneedqq.qq.QqMusicUpdater;

@Mixin(value = ItemMusicCD.class, remap = false)
public abstract class ItemMusicCDMixin {
    @Inject(method = "getSongInfo", at = @At("RETURN"), cancellable = true)
    private static void netmusiccanneedqq$refreshSongInfo(ItemStack stack, CallbackInfoReturnable<ItemMusicCD.SongInfo> cir) {
        ItemMusicCD.SongInfo info = cir.getReturnValue();
        if (info == null) {
            return;
        }
        cir.setReturnValue(QqMusicUpdater.refreshIfNeeded(stack, info));
    }
}
