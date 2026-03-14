package com.derk.easyinventorycrafter.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.recipe.NetworkRecipeId;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookWidget.class)
public abstract class RecipeBookWidgetMixin {
	@Shadow
	@Nullable
	private RecipeResultCollection selectedRecipeResults;

	@Shadow
	@Nullable
	private NetworkRecipeId selectedRecipe;

	@Invoker("select")
	protected abstract boolean derk$invokeSelect(RecipeResultCollection results, NetworkRecipeId recipeId, boolean craftAll);

	@Invoker("refreshInputs")
	protected abstract void derk$invokeRefreshInputs();

	@Inject(method = "refresh", at = @At("HEAD"))
	private void derk$refreshInputsForNearbyPayload(CallbackInfo ci) {
		this.derk$invokeRefreshInputs();
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void derk$spacebarAddsOneSet(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
		if (input.getKeycode() != InputUtil.GLFW_KEY_SPACE) {
			return;
		}
		if (this.selectedRecipeResults == null || this.selectedRecipe == null) {
			return;
		}
		ClickableWidget.playClickSound(MinecraftClient.getInstance().getSoundManager());
		cir.setReturnValue(this.derk$invokeSelect(this.selectedRecipeResults, this.selectedRecipe, false));
	}
}
