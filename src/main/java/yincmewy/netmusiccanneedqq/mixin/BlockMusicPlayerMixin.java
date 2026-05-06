package yincmewy.netmusiccanneedqq.mixin;

import com.github.tartaricacid.netmusic.block.BlockMusicPlayer;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yincmewy.netmusiccanneedqq.qq.QqMusicUpdater;

@Mixin(value = BlockMusicPlayer.class, remap = false)
public abstract class BlockMusicPlayerMixin {
    @Inject(method = "playerMusic", at = @At("HEAD"), require = 0)
    private static void netmusiccanneedqq$refreshBeforeMusic(Level level, BlockPos pos, boolean signal, CallbackInfo ci) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileEntityMusicPlayer te) {
            ItemStack stack = te.getPlayerInv().getStackInSlot(0);
            if (!stack.isEmpty()) {
                ItemMusicCD.SongInfo info = ItemMusicCD.getSongInfo(stack);
                if (info != null) {
                    QqMusicUpdater.refreshIfNeeded(stack, info);
                }
            }
        }
    }

    @Redirect(method = "playerMusic", at = @At(value = "INVOKE",
            target = "Lcom/github/tartaricacid/netmusic/item/ItemMusicCD;getSongInfo(Lnet/minecraft/world/item/ItemStack;)Lcom/github/tartaricacid/netmusic/item/ItemMusicCD$SongInfo;"),
            require = 0)
    private static ItemMusicCD.SongInfo netmusiccanneedqq$getRefreshedSong(ItemStack stack) {
        ItemMusicCD.SongInfo info = ItemMusicCD.getSongInfo(stack);
        if (info != null) {
            return QqMusicUpdater.refreshIfNeeded(stack, info);
        }
        return info;
    }
}
