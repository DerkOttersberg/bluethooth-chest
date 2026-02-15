package com.derk.easyinventorycrafter.mixin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import com.derk.easyinventorycrafter.NearbyInventoryScanner.NearbyItemEntry;
import com.derk.easyinventorycrafter.client.NearbyItemsClientState;
import com.derk.easyinventorycrafter.client.NearbyPanelAccess;
import com.derk.easyinventorycrafter.util.FormatUtils;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreen.class)
public abstract class CraftingScreenMixin extends HandledScreen<CraftingScreenHandler>
        implements NearbyPanelAccess {
    @Unique private static final int PANEL_COLUMNS = 4;
    @Unique private static final int PANEL_ROWS = 6;
    @Unique private static final int SLOT_SIZE = 21;
    @Unique private static final int SLOT_INNER = 18;
    @Unique private static final int PANEL_MARGIN = 6;
    @Unique private static final int PANEL_HEADER = 14;
    @Unique private static final int CLICK_PULSE_TICKS = 6;

    @Unique private ButtonWidget derk$nearbyButton;

    @Unique private TextFieldWidget derk$searchField;

    @Unique private int derk$scrollOffset;

    @Unique private boolean derk$nearbyOpen = true;

    @Unique private int derk$lastClickIndex = -1;

    @Unique private long derk$lastClickTick = -1000L;

    protected CraftingScreenMixin(
            CraftingScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void derk$initNearbyPanel(CallbackInfo ci) {
        NearbyItemsClientState.clear();
        int buttonX = this.x + this.backgroundWidth + 6;
        int buttonY = this.y + 6;
        ButtonWidget button =
                ButtonWidget.builder(
                                Text.of("Nearby"),
                                btn -> {
                                    this.derk$nearbyOpen = !this.derk$nearbyOpen;
                                    if (this.derk$nearbyOpen) {
                                        NearbyItemsClientState.requestUpdate();
                                    }
                                })
                        .dimensions(buttonX, buttonY, 60, 20)
                        .build();
        this.derk$nearbyButton = this.addDrawableChild(button);
        this.derk$searchField =
                new TextFieldWidget(this.textRenderer, buttonX, buttonY + 24, 84, 14, Text.of(""));
        this.derk$searchField.setMaxLength(50);
        this.derk$searchField.setPlaceholder(Text.of("Search..."));
        this.addDrawableChild(this.derk$searchField);
        this.derk$scrollOffset = 0;
        NearbyItemsClientState.requestUpdate();
    }

    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void derk$drawNearbyPanel(
            DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (this.derk$nearbyButton != null) {
            this.derk$nearbyButton.setX(this.x + this.backgroundWidth + 6);
            this.derk$nearbyButton.setY(this.y + 6);
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

        int panelX = this.x + this.backgroundWidth + PANEL_MARGIN;
        int panelY = this.y + 48;
        int panelWidth = PANEL_COLUMNS * SLOT_SIZE + PANEL_MARGIN;
        int panelHeight = PANEL_ROWS * SLOT_SIZE + 16;
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x88000000);
        context.drawTextWithShadow(
                this.textRenderer, Text.of("Nearby"), panelX + 4, panelY + 4, 0xFFFFFF);

        int startX = panelX + 3;
        int startY = panelY + PANEL_HEADER;
        int maxItems = PANEL_COLUMNS * PANEL_ROWS;
        int totalRows = (int) Math.ceil(entries.size() / (double) PANEL_COLUMNS);
        int maxScroll = Math.max(0, totalRows - PANEL_ROWS);
        this.derk$scrollOffset = Math.max(0, Math.min(this.derk$scrollOffset, maxScroll));
        for (int row = 0; row < PANEL_ROWS; row++) {
            for (int col = 0; col < PANEL_COLUMNS; col++) {
                int slotX = startX + col * SLOT_SIZE;
                int slotY = startY + row * SLOT_SIZE;
                context.fill(slotX, slotY, slotX + SLOT_INNER, slotY + SLOT_INNER, 0x55000000);
                context.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0x2A000000);
                context.fill(slotX, slotY, slotX + SLOT_INNER, slotY + 1, 0x66FFFFFF);
                context.fill(slotX, slotY, slotX + 1, slotY + SLOT_INNER, 0x66FFFFFF);
                context.fill(slotX, slotY + 17, slotX + SLOT_INNER, slotY + SLOT_INNER, 0x33000000);
                context.fill(slotX + 17, slotY, slotX + SLOT_INNER, slotY + SLOT_INNER, 0x33000000);
            }
        }
        int startIndex = this.derk$scrollOffset * PANEL_COLUMNS;
        int endIndex = Math.min(entries.size(), startIndex + maxItems);
        for (int index = startIndex; index < endIndex; index++) {
            int gridIndex = index - startIndex;
            int col = gridIndex % PANEL_COLUMNS;
            int row = gridIndex / PANEL_COLUMNS;
            int itemX = startX + col * SLOT_SIZE + 2;
            int itemY = startY + row * SLOT_SIZE + 1;
            NearbyItemEntry entry = entries.get(index);
            context.drawItem(entry.stack(), itemX, itemY);
            String count = FormatUtils.formatCount(entry.count());
            context.drawItemInSlot(this.textRenderer, entry.stack(), itemX, itemY, count);
        }

        int hoveredIndex = derk$getHoveredIndex(mouseX, mouseY, entries.size(), panelX, panelY);
        if (hoveredIndex >= 0) {
            NearbyItemEntry entry = entries.get(hoveredIndex);
            context.drawItemTooltip(this.textRenderer, entry.stack(), mouseX, mouseY);
        }

        derk$renderClickPulse(context, entries.size(), panelX, panelY);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (derk$handleCharTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean derk$handleMouseClick(double mouseX, double mouseY, int button) {
        if (!this.derk$nearbyOpen || button != 0) {
            return false;
        }
        int panelX = this.x + this.backgroundWidth + PANEL_MARGIN;
        int panelY = this.y + 48;
        int panelWidth = PANEL_COLUMNS * SLOT_SIZE + PANEL_MARGIN;
        int panelHeight = PANEL_ROWS * SLOT_SIZE + 16;
        if (mouseX < panelX
                || mouseX > panelX + panelWidth
                || mouseY < panelY
                || mouseY > panelY + panelHeight) {
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
        NearbyItemsClientState.requestHighlight(entry.stack());
        this.derk$lastClickIndex = index;
        this.derk$lastClickTick =
                MinecraftClient.getInstance().world == null
                        ? 0L
                        : MinecraftClient.getInstance().world.getTime();
        MinecraftClient.getInstance()
                .getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        return true;
    }

    @Unique
    private int derk$getHoveredIndex(
            double mouseX, double mouseY, int totalEntries, int panelX, int panelY) {
        int startX = panelX + 3;
        int startY = panelY + PANEL_HEADER;
        int relX = (int) mouseX - startX;
        int relY = (int) mouseY - startY;
        if (relX < 0 || relY < 0) {
            return -1;
        }
        int col = relX / SLOT_SIZE;
        int row = relY / SLOT_SIZE;
        if (col < 0 || col >= PANEL_COLUMNS || row < 0 || row >= PANEL_ROWS) {
            return -1;
        }
        int slotX = startX + col * SLOT_SIZE;
        int slotY = startY + row * SLOT_SIZE;
        if (mouseX > slotX + SLOT_INNER || mouseY > slotY + SLOT_INNER) {
            return -1;
        }
        int index = (this.derk$scrollOffset + row) * PANEL_COLUMNS + col;
        if (index < 0 || index >= totalEntries) {
            return -1;
        }
        return index;
    }

    @Unique
    private void derk$renderClickPulse(
            DrawContext context, int totalEntries, int panelX, int panelY) {
        if (this.derk$lastClickIndex < 0 || this.derk$lastClickIndex >= totalEntries) {
            return;
        }
        long now =
                MinecraftClient.getInstance().world == null
                        ? 0L
                        : MinecraftClient.getInstance().world.getTime();
        long age = now - this.derk$lastClickTick;
        if (age < 0 || age > CLICK_PULSE_TICKS) {
            return;
        }
        int localIndex = this.derk$lastClickIndex - this.derk$scrollOffset * PANEL_COLUMNS;
        if (localIndex < 0 || localIndex >= PANEL_COLUMNS * PANEL_ROWS) {
            return;
        }
        int col = localIndex % PANEL_COLUMNS;
        int row = localIndex / PANEL_COLUMNS;
        int startX = panelX + 3;
        int startY = panelY + PANEL_HEADER;
        int slotX = startX + col * SLOT_SIZE;
        int slotY = startY + row * SLOT_SIZE;
        float t = age / (float) CLICK_PULSE_TICKS;
        int alpha = MathHelper.clamp((int) (160 * (1.0f - t)), 0, 160);
        int color = (alpha << 24) | 0x00FFD966;
        context.fill(slotX, slotY, slotX + SLOT_INNER, slotY + SLOT_INNER, color);
    }

    @Override
    public boolean derk$handleScroll(double mouseX, double mouseY, double verticalAmount) {
        if (!this.derk$nearbyOpen) {
            return false;
        }
        int panelX = this.x + this.backgroundWidth + PANEL_MARGIN;
        int panelY = this.y + 48;
        int panelWidth = PANEL_COLUMNS * SLOT_SIZE + PANEL_MARGIN;
        int panelHeight = PANEL_ROWS * SLOT_SIZE + 16;
        if (mouseX >= panelX
                && mouseX <= panelX + panelWidth
                && mouseY >= panelY
                && mouseY <= panelY + panelHeight) {
            int delta = verticalAmount > 0 ? -1 : (verticalAmount < 0 ? 1 : 0);
            this.derk$scrollOffset += delta;
            return true;
        }
        return false;
    }

    @Override
    public boolean derk$handleCharTyped(char chr, int modifiers) {
        if (!this.derk$nearbyOpen) {
            return false;
        }
        if (this.derk$searchField != null && this.derk$searchField.charTyped(chr, modifiers)) {
            this.derk$scrollOffset = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean derk$handleKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.derk$nearbyOpen) {
            return false;
        }
        if (this.derk$searchField != null
                && this.derk$searchField.keyPressed(keyCode, scanCode, modifiers)) {
            this.derk$scrollOffset = 0;
            return true;
        }
        return false;
    }

    @Unique
    private List<NearbyItemEntry> derk$getFilteredEntries() {
        List<NearbyItemEntry> entries = NearbyItemsClientState.getEntries();
        String query =
                this.derk$searchField == null
                        ? ""
                        : this.derk$searchField.getText().trim().toLowerCase(Locale.ROOT);
        List<NearbyItemEntry> filtered = new ArrayList<>();
        for (NearbyItemEntry entry : entries) {
            String name = entry.stack().getName().getString().toLowerCase(Locale.ROOT);
            if (query.isEmpty() || name.contains(query)) {
                filtered.add(entry);
            }
        }
        filtered.sort(
                Comparator.comparingInt((NearbyItemEntry e) -> derk$getCategoryRank(e.stack()))
                        .thenComparing(
                                e -> e.stack().getName().getString(),
                                String.CASE_INSENSITIVE_ORDER));
        return filtered;
    }

    @Unique
    private int derk$getCategoryRank(ItemStack stack) {
        if (stack.isIn(ItemTags.LOGS)
                || stack.isIn(ItemTags.LOGS_THAT_BURN)
                || stack.isIn(ItemTags.PLANKS)) {
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
        if (stack.getItem().getFoodComponent() != null) {
            return 2;
        }
        return 3;
    }
}
