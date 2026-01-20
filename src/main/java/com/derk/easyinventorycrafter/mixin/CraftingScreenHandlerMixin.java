package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import com.derk.easyinventorycrafter.net.NearbyItemsSync;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreenHandler.class)
public class CraftingScreenHandlerMixin implements NearbyCraftingAccess {
	@Shadow
	@Final
	private ScreenHandlerContext context;

	@Shadow
	@Final
	private PlayerEntity player;

	@Override
	public ScreenHandlerContext derk$getContext() {
		return this.context;
	}

	@Override
	public PlayerEntity derk$getPlayer() {
		return this.player;
	}

	@Inject(method = "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/screen/ScreenHandlerContext;)V", at = @At("TAIL"))
	private void derk$sendInitialNearbyItems(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, CallbackInfo ci) {
		if (this.player instanceof ServerPlayerEntity serverPlayer) {
			NearbyItemsSync.sendNearbyItems(serverPlayer);
		}
	}
}
