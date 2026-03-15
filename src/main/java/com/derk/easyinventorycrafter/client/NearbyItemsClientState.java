package com.derk.easyinventorycrafter.client;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import com.derk.easyinventorycrafter.NearbyInventoryScanner.NearbyItemEntry;
import java.util.Collections;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig.LocateTrailParticle;
import com.derk.easyinventorycrafter.net.NearbyItemsPayload;
import com.derk.easyinventorycrafter.net.RequestNearbyItemsPayload;
import com.derk.easyinventorycrafter.net.NearbyHighlightRequestPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

public final class NearbyItemsClientState {
	private static final double SMOKE_TRAIL_MIN_DISTANCE_SQUARED = 81.0;
	private static final int SMOKE_TRAIL_TICKS = 14;
	private static final int SMOKE_PARTICLES_PER_TICK = 10;
	private static List<NearbyItemEntry> entries = Collections.emptyList();
	private static List<BlockPos> highlightPositions = Collections.emptyList();
	private static int highlightTicks;
	private static int highlightTotalTicks;
	private static boolean locateFeedbackPending;
	private static Vec3d smokeTrailStart = Vec3d.ZERO;
	private static Vec3d smokeTrailTarget = Vec3d.ZERO;
	private static int smokeTrailTicks;
	private static double smokeTrailProgress;

	private NearbyItemsClientState() {
	}

	public static List<NearbyItemEntry> getEntries() {
		return entries;
	}

	public static void clear() {
		entries = Collections.emptyList();
		highlightPositions = Collections.emptyList();
		highlightTicks = 0;
		highlightTotalTicks = 0;
		locateFeedbackPending = false;
		smokeTrailTicks = 0;
		smokeTrailProgress = 0.0;
		smokeTrailStart = Vec3d.ZERO;
		smokeTrailTarget = Vec3d.ZERO;
	}

	public static void requestUpdate() {
		if (ClientPlayNetworking.canSend(RequestNearbyItemsPayload.ID)) {
			ClientPlayNetworking.send(new RequestNearbyItemsPayload());
		}
	}

	public static void applyPayload(NearbyItemsPayload payload) {
		entries = payload.entries();
		MinecraftClient client = MinecraftClient.getInstance();
		client.execute(() -> {
			if (client.currentScreen instanceof RecipeBookProvider recipeBookProvider) {
				recipeBookProvider.refreshRecipeBook();
			}
		});
	}

	public static void requestHighlight(ItemStack stack) {
		requestHighlight(stack, false);
	}

	public static void requestHighlightAndAim(ItemStack stack) {
		requestHighlight(stack, true);
	}

	private static void requestHighlight(ItemStack stack, boolean aimAfterResponse) {
		if (stack == null || stack.isEmpty()) {
			return;
		}
		locateFeedbackPending = aimAfterResponse;
		if (ClientPlayNetworking.canSend(NearbyHighlightRequestPayload.ID)) {
			ClientPlayNetworking.send(new NearbyHighlightRequestPayload(stack.copyWithCount(1)));
		}
	}

	public static void setHighlight(List<BlockPos> positions, int ticks) {
		highlightPositions = positions == null ? Collections.emptyList() : positions;
		highlightTicks = ticks;
		highlightTotalTicks = ticks;
		if (locateFeedbackPending) {
			locateFeedbackPending = false;
			Vec3d target = derk$getNearestHighlightCenter();
			if (target != null) {
				if (EasyInventoryCrafterConfig.isSnapAimEnabled()) {
					derk$aimAtTarget(target);
				}
				if (EasyInventoryCrafterConfig.isLocateTrailEnabled()) {
					derk$startSmokeTrail(target);
				}
			}
		}
	}

	public static List<BlockPos> getHighlightPositions() {
		return highlightPositions;
	}

	public static boolean hasHighlight() {
		return EasyInventoryCrafterConfig.isHighlightEnabled() && !highlightPositions.isEmpty() && highlightTicks > 0;
	}

	public static float getHighlightAlpha() {
		if (highlightTotalTicks <= 0) {
			return 1.0f;
		}
		float progress = 1.0f - (highlightTicks / (float)highlightTotalTicks);
		float alpha = (float)Math.sin(Math.PI * progress);
		return Math.max(0.0f, Math.min(1.0f, alpha));
	}

	public static void tickHighlight(MinecraftClient client) {
		derk$tickSmokeTrail(client);
		if (highlightPositions.isEmpty() || highlightTicks <= 0 || client.world == null) {
			if (highlightTicks <= 0) {
				highlightPositions = Collections.emptyList();
				highlightTotalTicks = 0;
			}
			return;
		}
		highlightTicks--;
		if (highlightTicks <= 0) {
			highlightPositions = Collections.emptyList();
			highlightTotalTicks = 0;
			return;
		}
	}

	private static Vec3d derk$getNearestHighlightCenter() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.world == null || highlightPositions.isEmpty()) {
			return null;
		}

		Vec3d eyePos = client.player.getEyePos();
		BlockPos nearestPos = null;
		double nearestDistance = Double.MAX_VALUE;
		for (BlockPos pos : highlightPositions) {
			double distance = eyePos.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearestPos = pos;
			}
		}

		if (nearestPos == null) {
			return null;
		}

		return new Vec3d(nearestPos.getX() + 0.5, nearestPos.getY() + 0.5, nearestPos.getZ() + 0.5);
	}

	private static void derk$aimAtTarget(Vec3d target) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return;
		}

		Vec3d eyePos = client.player.getEyePos();
		Vec3d delta = target.subtract(eyePos);
		double horizontalDistance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
		float yaw = (float)(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
		float pitch = (float)(-Math.toDegrees(Math.atan2(delta.y, horizontalDistance)));
		client.player.setYaw(yaw);
		client.player.setPitch(pitch);
	}

	private static void derk$startSmokeTrail(Vec3d target) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.world == null) {
			return;
		}

		Vec3d eyePos = client.player.getEyePos();
		if (eyePos.squaredDistanceTo(target) < SMOKE_TRAIL_MIN_DISTANCE_SQUARED) {
			return;
		}

		smokeTrailStart = eyePos;
		smokeTrailTarget = target;
		smokeTrailTicks = SMOKE_TRAIL_TICKS;
		smokeTrailProgress = 0.0;
	}

	private static void derk$tickSmokeTrail(MinecraftClient client) {
		if (smokeTrailTicks <= 0 || client.world == null) {
			if (smokeTrailTicks <= 0) {
				smokeTrailProgress = 0.0;
			}
			return;
		}

		double nextProgress = derk$smoothProgress((SMOKE_TRAIL_TICKS - smokeTrailTicks + 1) / (double)SMOKE_TRAIL_TICKS);
		Vec3d delta = smokeTrailTarget.subtract(smokeTrailStart);
		for (int i = 0; i < SMOKE_PARTICLES_PER_TICK; i++) {
			double particleProgress = smokeTrailProgress + (nextProgress - smokeTrailProgress) * (i / (double)Math.max(1, SMOKE_PARTICLES_PER_TICK - 1));
			Vec3d position = smokeTrailStart.add(delta.multiply(particleProgress));
			LocateTrailParticle particle = EasyInventoryCrafterConfig.getLocateTrailParticle();
			double jitter = particle == LocateTrailParticle.END_ROD ? 0.03 : 0.08;
			double verticalVelocity = particle == LocateTrailParticle.END_ROD ? 0.0 : 0.01;
			double jitterX = (client.world.random.nextDouble() - 0.5) * jitter;
			double jitterY = (client.world.random.nextDouble() - 0.5) * jitter;
			double jitterZ = (client.world.random.nextDouble() - 0.5) * jitter;
			client.particleManager.addParticle(derk$getTrailParticleEffect(particle), position.x + jitterX, position.y + jitterY, position.z + jitterZ, 0.0, verticalVelocity, 0.0);
		}

		smokeTrailProgress = nextProgress;
		smokeTrailTicks--;
		if (smokeTrailTicks <= 0) {
			smokeTrailProgress = 0.0;
		}
	}

	private static double derk$smoothProgress(double progress) {
		double clamped = Math.max(0.0, Math.min(1.0, progress));
		return clamped * clamped * (3.0 - 2.0 * clamped);
	}

	private static ParticleEffect derk$getTrailParticleEffect(LocateTrailParticle particle) {
		return switch (particle) {
			case WATER_EVAPORATION -> ParticleTypes.CLOUD;
			case SMOKE -> ParticleTypes.SMOKE;
			case END_ROD -> ParticleTypes.END_ROD;
		};
	}
}
