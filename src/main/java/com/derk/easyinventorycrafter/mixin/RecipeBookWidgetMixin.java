package com.derk.easyinventorycrafter.mixin;

import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookWidget.class)
public abstract class RecipeBookWidgetMixin {
	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void derk$spacebarAddsOneSet(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		if (keyCode != InputUtil.GLFW_KEY_SPACE) {
			return;
		}
		// In 1.20.1, just consume the spacebar event to prevent accidental input
		cir.setReturnValue(true);
	}
}
