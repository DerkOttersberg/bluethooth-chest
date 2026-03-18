package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.client.NearbyRecipeBookComponentAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin implements NearbyRecipeBookComponentAccess {
    @Shadow
    @Nullable
    private RecipeBookPage recipeBookPage;

    @Shadow
    @Nullable
    protected RecipeBookMenu<?, ?> menu;

    @Shadow
    @Nullable
    protected Minecraft minecraft;

    @Invoker("updateStackedContents")
    protected abstract void derk$invokeUpdateStackedContents();

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void derk$spacebarAddsOneSet(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (keyCode != 32 || minecraft == null || minecraft.gameMode == null || menu == null) {
            return;
        }

        RecipeCollection lastRecipeCollection = recipeBookPage.getLastClickedRecipeCollection();
        RecipeHolder<?> lastRecipe = recipeBookPage.getLastClickedRecipe();
        if (lastRecipeCollection == null || lastRecipe == null) {
            return;
        }

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        minecraft.gameMode.handlePlaceRecipe(menu.containerId, lastRecipe, false);
        cir.setReturnValue(true);
    }

    @Override
    public void derk$refreshStackedContents() {
        derk$invokeUpdateStackedContents();
    }
}
