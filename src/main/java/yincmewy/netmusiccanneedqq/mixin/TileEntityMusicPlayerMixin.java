package yincmewy.netmusiccanneedqq.mixin;

import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yincmewy.netmusiccanneedqq.Netmusiccanneedqq;
import yincmewy.netmusiccanneedqq.network.NetMusicCanNeedQQNetwork;
import yincmewy.netmusiccanneedqq.network.QqLyricMessage;
import yincmewy.netmusiccanneedqq.qq.QqDiscNbt;
import yincmewy.netmusiccanneedqq.qq.QqMusicUpdater;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@Mixin(value = TileEntityMusicPlayer.class, remap = false)
public abstract class TileEntityMusicPlayerMixin {
    @Shadow
    @Final
    private ItemStackHandler playerInv;

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lcom/github/tartaricacid/netmusic/item/ItemMusicCD;getSongInfo(Lnet/minecraft/world/item/ItemStack;)Lcom/github/tartaricacid/netmusic/item/ItemMusicCD$SongInfo;"))
    private static ItemMusicCD.SongInfo netmusiccanneedqq$refreshAndGetSong(ItemStack stack) {
        ItemMusicCD.SongInfo info = ItemMusicCD.getSongInfo(stack);
        if (info != null) {
            return QqMusicUpdater.refreshIfNeeded(stack, info);
        }
        return info;
    }

    /**
     * 在setPlayToClient方法执行时，如果是QQ音乐唱片，发送songmid到客户端用于获取歌词
     */
    @Inject(method = "setPlayToClient", at = @At("HEAD"))
    private void netmusiccanneedqq$sendQqLyric(ItemMusicCD.SongInfo info, CallbackInfo ci) {
        TileEntityMusicPlayer self = (TileEntityMusicPlayer) (Object) this;
        if (!(self.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        ItemStack disc = playerInv.getStackInSlot(0);
        if (disc.isEmpty() || !QqDiscNbt.isQqDisc(disc)) {
            return;
        }

        String qqInput = QqDiscNbt.getQqInput(disc);
        if (qqInput == null || qqInput.isBlank()) {
            return;
        }

        // 从qqInput中提取songmid
        String songmid = extractSongmid(qqInput);
        if (songmid == null || songmid.isBlank()) {
            return;
        }

        BlockPos pos = self.getBlockPos();
        Netmusiccanneedqq.LOGGER.info("Sending QQ lyric songmid: {} for pos: {}", songmid, pos);

        // 发送给附近的玩家
        QqLyricMessage msg = new QqLyricMessage(pos, songmid);
        for (ServerPlayer player : serverLevel.players()) {
            if (player.blockPosition().closerThan(pos, 96)) {
                NetMusicCanNeedQQNetwork.sendToPlayer(player, msg);
            }
        }
    }

    /**
     * 从qqInput中提取songmid
     * qqInput可能是：
     * - 纯songmid（如 003aAYrm3GE0Ac）
     * - QQ音乐URL（如 https://y.qq.com/n/ryqq/songDetail/003aAYrm3GE0Ac）
     */
    private String extractSongmid(String qqInput) {
        if (qqInput == null || qqInput.isBlank()) {
            return null;
        }
        qqInput = qqInput.trim();

        // 如果是纯songmid（10-16位字母数字）
        if (qqInput.matches("^[0-9a-zA-Z]{10,16}$")) {
            return qqInput;
        }

        // 如果是URL，尝试提取songDetail后面的songmid
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("songDetail/([0-9a-zA-Z]+)").matcher(qqInput);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // 如果是其他URL格式，尝试提取id参数
        matcher = java.util.regex.Pattern.compile("id=([0-9a-zA-Z]+)").matcher(qqInput);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // 如果都不匹配，返回原始输入（可能是songmid）
        return qqInput;
    }
}
