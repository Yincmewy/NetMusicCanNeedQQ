package yincmewy.netmusiccanneedqq.mixin;

import com.github.tartaricacid.netmusic.block.BlockMusicPlayer;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yincmewy.netmusiccanneedqq.qq.QqMusicUpdater;

@Mixin(value = BlockMusicPlayer.class, remap = false)
public abstract class BlockMusicPlayerMixin {
    @Redirect(method = "playerMusic", at = @At(value = "INVOKE", target = "Lcom/github/tartaricacid/netmusic/item/ItemMusicCD;getSongInfo(Lnet/minecraft/world/item/ItemStack;)Lcom/github/tartaricacid/netmusic/item/ItemMusicCD$SongInfo;"))
    private static ItemMusicCD.SongInfo netmusiccanneedqq$refreshQqSongStatic(ItemStack stack) {
        return netmusiccanneedqq$refreshQqSong(stack);
    }

    @Redirect(method = "use", require = 0, at = @At(value = "INVOKE", target = "Lcom/github/tartaricacid/netmusic/item/ItemMusicCD;getSongInfo(Lnet/minecraft/world/item/ItemStack;)Lcom/github/tartaricacid/netmusic/item/ItemMusicCD$SongInfo;"))
    private ItemMusicCD.SongInfo netmusiccanneedqq$refreshQqSongDeobf(ItemStack stack) {
        return netmusiccanneedqq$refreshQqSong(stack);
    }

    @Redirect(method = "m_6227_", require = 0, at = @At(value = "INVOKE", target = "Lcom/github/tartaricacid/netmusic/item/ItemMusicCD;getSongInfo(Lnet/minecraft/world/item/ItemStack;)Lcom/github/tartaricacid/netmusic/item/ItemMusicCD$SongInfo;"))
    private ItemMusicCD.SongInfo netmusiccanneedqq$refreshQqSongObf(ItemStack stack) {
        return netmusiccanneedqq$refreshQqSong(stack);
    }

    private static ItemMusicCD.SongInfo netmusiccanneedqq$refreshQqSong(ItemStack stack) {
        ItemMusicCD.SongInfo info = ItemMusicCD.getSongInfo(stack);
        return QqMusicUpdater.refreshIfNeeded(stack, info);
    }
}
