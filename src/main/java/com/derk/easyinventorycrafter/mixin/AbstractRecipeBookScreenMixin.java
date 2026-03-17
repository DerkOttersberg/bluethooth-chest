package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.client.NearbyPanelAccess;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractRecipeBookScreen.class)
public class AbstractRecipeBookScreenMixin {
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void derk$handleCharTyped(CharacterEvent input, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof NearbyPanelAccess access) {
            if (access.derk$handleCharTyped(input)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void derk$handleKeyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof NearbyPanelAccess access) {
            if (access.derk$handleKeyPressed(input)) {
                cir.setReturnValue(true);
            }
        }
    }
}
