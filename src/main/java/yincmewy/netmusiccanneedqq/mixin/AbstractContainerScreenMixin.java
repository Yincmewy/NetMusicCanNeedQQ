package yincmewy.netmusiccanneedqq.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yincmewy.netmusiccanneedqq.client.QqSearchState;
import yincmewy.netmusiccanneedqq.client.QqSearchTarget;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "removed", at = @At("HEAD"), cancellable = true)
    private void netmusiccanneedqq$skipCloseOnSearch(CallbackInfo ci) {
        if (QqSearchState.isOpening() && (Object) this instanceof QqSearchTarget) {
            ci.cancel();
        }
    }
}
