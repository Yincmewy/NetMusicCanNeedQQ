package yincmewy.netmusiccanneedqq.mixin;

import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yincmewy.netmusiccanneedqq.qq.QqMusicUpdater;

@Mixin(value = TileEntityMusicPlayer.class, remap = false)
public abstract class TileEntityMusicPlayerMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lcom/github/tartaricacid/netmusic/item/ItemMusicCD;getSongInfo(Lnet/minecraft/world/item/ItemStack;)Lcom/github/tartaricacid/netmusic/item/ItemMusicCD$SongInfo;"))
    private static ItemMusicCD.SongInfo netmusiccanneedqq$refreshQqSong(ItemStack stack) {
        ItemMusicCD.SongInfo info = ItemMusicCD.getSongInfo(stack);
        return QqMusicUpdater.refreshIfNeeded(stack, info);
    }
}
