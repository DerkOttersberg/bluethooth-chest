package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import com.derk.easyinventorycrafter.client.NearbyItemsClientState;
import com.derk.easyinventorycrafter.client.NearbyPanelAccess;
import com.derk.easyinventorycrafter.NearbyInventoryScanner.NearbyItemEntry;
import com.derk.easyinventorycrafter.net.ReturnNearbyItemsPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.recipebook.CraftingRecipeBookWidget;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.text.Text;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreen.class)
public abstract class CraftingScreenMixin extends RecipeBookScreen<CraftingScreenHandler> implements NearbyPanelAccess {
	@Unique
	private ButtonWidget derk$nearbyButton;

	@Unique
	private ButtonWidget derk$cancelButton;

	@Unique
	private TextFieldWidget derk$searchField;

	@Unique
	private int derk$scrollOffset;

	@Unique
	private boolean derk$nearbyOpen = true;

	@Unique
	private int derk$lastClickIndex = -1;

	@Unique
	private long derk$lastClickTick = -1000L;

	protected CraftingScreenMixin(CraftingScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, new CraftingRecipeBookWidget(handler), inventory, title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void derk$initNearbyPanel(CallbackInfo ci) {
		NearbyItemsClientState.clear();
		this.derk$nearbyOpen = EasyInventoryCrafterConfig.isNearbyPanelOpenByDefault();
		int buttonX = this.x + this.backgroundWidth + 6;
		int buttonY = this.y + 6;
		ButtonWidget button = ButtonWidget.builder(Text.of("Nearby"), btn -> {
			this.derk$nearbyOpen = !this.derk$nearbyOpen;
			if (this.derk$nearbyOpen) {
				NearbyItemsClientState.requestUpdate();
			}
		})
				.dimensions(buttonX, buttonY, 60, 20)
				.build();
		this.derk$nearbyButton = this.addDrawableChild(button);
		this.derk$cancelButton = this.addDrawableChild(ButtonWidget.builder(Text.of("X"), btn -> ClientPlayNetworking.send(new ReturnNearbyItemsPayload()))
				.dimensions(buttonX + 64, buttonY, 20, 20)
				.tooltip(Tooltip.of(Text.of("Returns nearby items to chest")))
				.build());
		this.derk$searchField = new TextFieldWidget(this.textRenderer, buttonX, buttonY + 24, 84, 14, Text.of(""));
		this.derk$searchField.setMaxLength(50);
		this.derk$searchField.setPlaceholder(Text.of("Search..."));
		this.addDrawableChild(this.derk$searchField);
		this.derk$scrollOffset = 0;
		NearbyItemsClientState.requestUpdate();
	}

	@Inject(method = "drawBackground", at = @At("TAIL"))
	private void derk$drawNearbyPanel(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
		if (this.derk$nearbyButton != null) {
			this.derk$nearbyButton.setX(this.x + this.backgroundWidth + 6);
			this.derk$nearbyButton.setY(this.y + 6);
		}
		if (this.derk$cancelButton != null) {
			this.derk$cancelButton.setX(this.x + this.backgroundWidth + 70);
			this.derk$cancelButton.setY(this.y + 6);
		}
		if (this.derk$searchField != null) {
			this.derk$searchField.setX(this.x + this.backgroundWidth + 6);
			this.derk$searchField.setY(this.y + 30);
			this.derk$searchField.setVisible(this.derk$nearbyOpen);
		}
		if (!this.derk$nearbyOpen) {
			return;
		}

		List<NearbyItemEntry> entries = derk$getFilteredEntries();
		if (entries.isEmpty()) {
			return;
		}

		int panelX = this.x + this.backgroundWidth + 6;
		int panelY = this.y + 48;
		int columns = 4;
		int rows = 6;
		int slotSize = 21;
		int panelWidth = columns * slotSize + 6;
		int panelHeight = rows * slotSize + 16;
		context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x88000000);
		context.drawTextWithShadow(this.textRenderer, Text.of("Nearby"), panelX + 4, panelY + 4, 0xFFFFFF);

		int startX = panelX + 3;
		int startY = panelY + 14;
		int maxItems = columns * rows;
		int totalRows = (int)Math.ceil(entries.size() / (double)columns);
		int maxScroll = Math.max(0, totalRows - rows);
		this.derk$scrollOffset = Math.max(0, Math.min(this.derk$scrollOffset, maxScroll));
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < columns; col++) {
				int slotX = startX + col * slotSize;
				int slotY = startY + row * slotSize;
				context.fill(slotX, slotY, slotX + 18, slotY + 18, 0x55000000);
				context.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0x2A000000);
				context.fill(slotX, slotY, slotX + 18, slotY + 1, 0x66FFFFFF);
				context.fill(slotX, slotY, slotX + 1, slotY + 18, 0x66FFFFFF);
				context.fill(slotX, slotY + 17, slotX + 18, slotY + 18, 0x33000000);
				context.fill(slotX + 17, slotY, slotX + 18, slotY + 18, 0x33000000);
			}
		}
		int startIndex = this.derk$scrollOffset * columns;
		int endIndex = Math.min(entries.size(), startIndex + maxItems);
		for (int index = startIndex; index < endIndex; index++) {
			int gridIndex = index - startIndex;
			int col = gridIndex % columns;
			int row = gridIndex / columns;
			int itemX = startX + col * slotSize + 2;
			int itemY = startY + row * slotSize + 1;
			NearbyItemEntry entry = entries.get(index);
			context.drawItem(entry.stack(), itemX, itemY);
			String count = derk$formatCount(entry.count());
			context.drawStackOverlay(this.textRenderer, entry.stack(), itemX, itemY, count);
		}

		int hoveredIndex = derk$getHoveredIndex(mouseX, mouseY, entries.size(), panelX, panelY);
		if (hoveredIndex >= 0) {
			NearbyItemEntry entry = entries.get(hoveredIndex);
			context.drawItemTooltip(this.textRenderer, entry.stack(), mouseX, mouseY);
		}

		derk$renderClickPulse(context, entries.size(), panelX, panelY);
	}

	@Override
	public boolean derk$handleMouseClick(Click click, boolean doubleClick) {
		if (!this.derk$nearbyOpen || click.button() != 0) {
			return false;
		}
		double mouseX = click.x();
		double mouseY = click.y();
		int panelX = this.x + this.backgroundWidth + 6;
		int panelY = this.y + 48;
		int columns = 4;
		int rows = 6;
		int slotSize = 21;
		int panelWidth = columns * slotSize + 6;
		int panelHeight = rows * slotSize + 16;
		if (mouseX < panelX || mouseX > panelX + panelWidth || mouseY < panelY || mouseY > panelY + panelHeight) {
			return false;
		}
		List<NearbyItemEntry> entries = derk$getFilteredEntries();
		if (entries.isEmpty()) {
			return false;
		}
		int index = derk$getHoveredIndex(mouseX, mouseY, entries.size(), panelX, panelY);
		if (index < 0 || index >= entries.size()) {
			return false;
		}
		NearbyItemEntry entry = entries.get(index);
		NearbyItemsClientState.requestHighlightAndAim(entry.stack());
		this.derk$lastClickIndex = index;
		this.derk$lastClickTick = MinecraftClient.getInstance().world == null ? 0L : MinecraftClient.getInstance().world.getTime();
		SoundManager soundManager = MinecraftClient.getInstance().getSoundManager();
		soundManager.play(net.minecraft.client.sound.PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0f));
		this.close();
		return true;
	}

	@Unique
	private int derk$getHoveredIndex(double mouseX, double mouseY, int totalEntries, int panelX, int panelY) {
		int columns = 4;
		int rows = 6;
		int slotSize = 21;
		int startX = panelX + 3;
		int startY = panelY + 14;
		int relX = (int)mouseX - startX;
		int relY = (int)mouseY - startY;
		if (relX < 0 || relY < 0) {
			return -1;
		}
		int col = relX / slotSize;
		int row = relY / slotSize;
		if (col < 0 || col >= columns || row < 0 || row >= rows) {
			return -1;
		}
		int slotX = startX + col * slotSize;
		int slotY = startY + row * slotSize;
		if (mouseX > slotX + 18 || mouseY > slotY + 18) {
			return -1;
		}
		int index = (this.derk$scrollOffset + row) * columns + col;
		if (index < 0 || index >= totalEntries) {
			return -1;
		}
		return index;
	}

	@Unique
	private void derk$renderClickPulse(DrawContext context, int totalEntries, int panelX, int panelY) {
		if (!EasyInventoryCrafterConfig.isHighlightEnabled()) {
			return;
		}
		if (this.derk$lastClickIndex < 0 || this.derk$lastClickIndex >= totalEntries) {
			return;
		}
		long now = MinecraftClient.getInstance().world == null ? 0L : MinecraftClient.getInstance().world.getTime();
		long age = now - this.derk$lastClickTick;
		if (age < 0 || age > 6) {
			return;
		}
		int columns = 4;
		int rows = 6;
		int slotSize = 21;
		int localIndex = this.derk$lastClickIndex - this.derk$scrollOffset * columns;
		if (localIndex < 0 || localIndex >= columns * rows) {
			return;
		}
		int col = localIndex % columns;
		int row = localIndex / columns;
		int startX = panelX + 3;
		int startY = panelY + 14;
		int slotX = startX + col * slotSize;
		int slotY = startY + row * slotSize;
		float t = age / 6.0f;
		int alpha = MathHelper.clamp((int)(160 * (1.0f - t)), 0, 160);
		int color = (alpha << 24) | EasyInventoryCrafterConfig.getHighlightColor();
		context.fill(slotX, slotY, slotX + 18, slotY + 18, color);
	}

	@Override
	public boolean derk$handleScroll(double mouseX, double mouseY, double verticalAmount) {
		if (!this.derk$nearbyOpen) {
			return false;
		}
		int panelX = this.x + this.backgroundWidth + 6;
		int panelY = this.y + 48;
		int columns = 4;
		int rows = 6;
		int slotSize = 21;
		int panelWidth = columns * slotSize + 6;
		int panelHeight = rows * slotSize + 16;
		if (mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= panelY && mouseY <= panelY + panelHeight) {
			int delta = verticalAmount > 0 ? -1 : (verticalAmount < 0 ? 1 : 0);
			this.derk$scrollOffset += delta;
			return true;
		}
		return false;
	}

	@Override
	public boolean derk$handleCharTyped(net.minecraft.client.input.CharInput input) {
		if (!this.derk$nearbyOpen) {
			return false;
		}
		if (this.derk$searchField != null && this.derk$searchField.charTyped(input)) {
			this.derk$scrollOffset = 0;
			return true;
		}
		return false;
	}

	@Override
	public boolean derk$handleKeyPressed(net.minecraft.client.input.KeyInput input) {
		if (!this.derk$nearbyOpen) {
			return false;
		}
		if (this.derk$searchField != null && this.derk$searchField.keyPressed(input)) {
			this.derk$scrollOffset = 0;
			return true;
		}
		return false;
	}

	@Unique
	private List<NearbyItemEntry> derk$getFilteredEntries() {
		List<NearbyItemEntry> entries = NearbyItemsClientState.getEntries();
		String query = this.derk$searchField == null ? "" : this.derk$searchField.getText().trim().toLowerCase(java.util.Locale.ROOT);
		List<NearbyItemEntry> filtered = new ArrayList<>();
		for (NearbyItemEntry entry : entries) {
			String name = entry.stack().getName().getString().toLowerCase(java.util.Locale.ROOT);
			if (query.isEmpty() || name.contains(query)) {
				filtered.add(entry);
			}
		}
		filtered.sort(Comparator
				.comparingInt((NearbyItemEntry e) -> derk$getCategoryRank(e.stack()))
				.thenComparing(e -> e.stack().getName().getString(), String.CASE_INSENSITIVE_ORDER));
		return filtered;
	}

	@Unique
	private int derk$getCategoryRank(ItemStack stack) {
		if (stack.isIn(ItemTags.LOGS) || stack.isIn(ItemTags.LOGS_THAT_BURN) || stack.isIn(ItemTags.PLANKS)) {
			return 0;
		}
		if (stack.isIn(ItemTags.COAL_ORES)
				|| stack.isIn(ItemTags.IRON_ORES)
				|| stack.isIn(ItemTags.COPPER_ORES)
				|| stack.isIn(ItemTags.GOLD_ORES)
				|| stack.isIn(ItemTags.REDSTONE_ORES)
				|| stack.isIn(ItemTags.LAPIS_ORES)
				|| stack.isIn(ItemTags.DIAMOND_ORES)
				|| stack.isIn(ItemTags.EMERALD_ORES)) {
			return 1;
		}
		if (stack.get(DataComponentTypes.FOOD) != null) {
			return 2;
		}
		return 3;
	}

	private static String derk$formatCount(int count) {
		if (count < 1000) {
			return String.valueOf(count);
		}
		if (count < 1000000) {
			return (count / 1000) + "k";
		}
		if (count < 1000000000) {
			return (count / 1000000) + "M";
		}
		return (count / 1000000000) + "B";
	}
}
