package com.derk.easyinventorycrafter.client;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig.ConfigData;
import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig.LocateTrailParticle;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class EasyInventoryCrafterConfigScreen extends Screen {
	private final Screen parent;
	private ButtonWidget highlightColorLabelButton;
	private ButtonWidget highlightDurationLabelButton;
	private ButtonWidget nearbyRadiusLabelButton;
	private ButtonWidget highlightOpacityLabelButton;
	private ButtonWidget autoRefreshLabelButton;
	private ButtonWidget highlightEnabledLabelButton;
	private ButtonWidget distanceLabelLabelButton;
	private ButtonWidget snapAimLabelButton;
	private ButtonWidget smokeTrailLabelButton;
	private ButtonWidget trailParticleLabelButton;
	private ButtonWidget panelDefaultLabelButton;
	private TextFieldWidget highlightColorField;
	private TextFieldWidget highlightDurationField;
	private TextFieldWidget nearbyRadiusField;
	private TextFieldWidget highlightOpacityField;
	private TextFieldWidget autoRefreshField;
	private ButtonWidget highlightColorPickerButton;
	private boolean showHighlighter;
	private boolean showDistanceLabel;
	private boolean snapAimToChest;
	private boolean showSmokeTrail;
	private LocateTrailParticle locateTrailParticle;
	private boolean nearbyPanelOpenByDefault;
	private ButtonWidget highlightEnabledButton;
	private ButtonWidget showDistanceLabelButton;
	private ButtonWidget snapAimButton;
	private ButtonWidget smokeTrailButton;
	private ButtonWidget trailParticleButton;
	private ButtonWidget panelDefaultButton;
	private Text errorText = Text.empty();

	public EasyInventoryCrafterConfigScreen(Screen parent) {
		super(Text.of("Bluethooth Chest Settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ConfigData config = EasyInventoryCrafterConfig.snapshot();
		this.showHighlighter = config.showHighlighter;
		this.showDistanceLabel = config.showDistanceLabel;
		this.snapAimToChest = config.snapAimToChest;
		this.showSmokeTrail = config.showLocateTrail;
		this.locateTrailParticle = config.locateTrailParticle;
		this.nearbyPanelOpenByDefault = config.nearbyPanelOpenByDefault;

		int centerX = this.width / 2;
		int labelButtonX = centerX - 168;
		int labelButtonWidth = 126;
		int fieldX = labelButtonX + labelButtonWidth + 10;
		int fieldWidth = 108;
		int startY = 62;
		int rowHeight = 34;

		this.highlightColorLabelButton = this.derk$createLabelButton(labelButtonX, startY, labelButtonWidth, "Highlight Color");
		this.highlightDurationLabelButton = this.derk$createLabelButton(labelButtonX, startY + rowHeight, labelButtonWidth, "Highlight Duration");
		this.nearbyRadiusLabelButton = this.derk$createLabelButton(labelButtonX, startY + rowHeight * 2, labelButtonWidth, "Nearby Distance");
		this.highlightOpacityLabelButton = this.derk$createLabelButton(labelButtonX, startY + rowHeight * 3, labelButtonWidth, "Highlight Opacity");
		this.autoRefreshLabelButton = this.derk$createLabelButton(labelButtonX, startY + rowHeight * 4, labelButtonWidth, "Auto Refresh");
		this.highlightEnabledLabelButton = this.derk$createLabelButton(labelButtonX, startY + rowHeight * 5, labelButtonWidth, "Chest Highlighter");
		this.distanceLabelLabelButton = this.derk$createLabelButton(labelButtonX, startY + rowHeight * 6, labelButtonWidth, "Distance Label");
		this.snapAimLabelButton = this.derk$createLabelButton(labelButtonX, startY + rowHeight * 7, labelButtonWidth, "Snap Aim To Chest");
		this.smokeTrailLabelButton = this.derk$createLabelButton(labelButtonX, startY + rowHeight * 8, labelButtonWidth, "Locate Trail");
		this.trailParticleLabelButton = this.derk$createLabelButton(labelButtonX, startY + rowHeight * 9, labelButtonWidth, "Trail Particle");
		this.panelDefaultLabelButton = this.derk$createLabelButton(labelButtonX, startY + rowHeight * 10, labelButtonWidth, "Panel Default");

		this.highlightColorField = this.derk$createField(fieldX, startY, fieldWidth, String.format(Locale.ROOT, "#%06X", config.highlightColor));
		this.highlightColorField.setPlaceholder(Text.of("#RRGGBB"));
		this.highlightColorPickerButton = this.addDrawableChild(ButtonWidget.builder(Text.of("Pick"), button -> this.client.setScreen(new EasyColorPickerScreen(this, this.derk$parseHexColor(this.highlightColorField.getText()), color -> this.highlightColorField.setText(String.format(Locale.ROOT, "#%06X", color)))))
				.dimensions(fieldX + fieldWidth + 8, startY, 56, 20)
				.build());
		this.highlightDurationField = this.derk$createField(fieldX, startY + rowHeight, fieldWidth, this.derk$formatSeconds(config.highlightDurationTicks));
		this.highlightDurationField.setPlaceholder(Text.of("Seconds"));
		this.nearbyRadiusField = this.derk$createField(fieldX, startY + rowHeight * 2, fieldWidth, Integer.toString(config.nearbyRadius));
		this.nearbyRadiusField.setPlaceholder(Text.of("Blocks"));
		this.highlightOpacityField = this.derk$createField(fieldX, startY + rowHeight * 3, fieldWidth, Integer.toString(config.highlightOpacityPercent));
		this.highlightOpacityField.setPlaceholder(Text.of("0-100"));
		this.autoRefreshField = this.derk$createField(fieldX, startY + rowHeight * 4, fieldWidth, this.derk$formatSeconds(config.autoRefreshTicks));
		this.autoRefreshField.setPlaceholder(Text.of("Seconds"));

		this.highlightEnabledButton = this.addDrawableChild(ButtonWidget.builder(this.derk$getHighlightEnabledText(), button -> {
			this.showHighlighter = !this.showHighlighter;
			button.setMessage(this.derk$getHighlightEnabledText());
		}).dimensions(fieldX, startY + rowHeight * 5, 96, 20).build());

		this.showDistanceLabelButton = this.addDrawableChild(ButtonWidget.builder(this.derk$getDistanceLabelText(), button -> {
			this.showDistanceLabel = !this.showDistanceLabel;
			button.setMessage(this.derk$getDistanceLabelText());
		}).dimensions(fieldX, startY + rowHeight * 6, 96, 20).build());

		this.snapAimButton = this.addDrawableChild(ButtonWidget.builder(this.derk$getSnapAimText(), button -> {
			this.snapAimToChest = !this.snapAimToChest;
			button.setMessage(this.derk$getSnapAimText());
		}).dimensions(fieldX, startY + rowHeight * 7, 96, 20).build());

		this.smokeTrailButton = this.addDrawableChild(ButtonWidget.builder(this.derk$getSmokeTrailText(), button -> {
			this.showSmokeTrail = !this.showSmokeTrail;
			button.setMessage(this.derk$getSmokeTrailText());
		}).dimensions(fieldX, startY + rowHeight * 8, 96, 20).build());

		this.trailParticleButton = this.addDrawableChild(ButtonWidget.builder(this.derk$getTrailParticleText(), button -> {
			this.locateTrailParticle = this.locateTrailParticle.next();
			button.setMessage(this.derk$getTrailParticleText());
		}).dimensions(fieldX, startY + rowHeight * 9, 132, 20).build());

		this.panelDefaultButton = this.addDrawableChild(ButtonWidget.builder(this.derk$getPanelDefaultText(), button -> {
			this.nearbyPanelOpenByDefault = !this.nearbyPanelOpenByDefault;
			button.setMessage(this.derk$getPanelDefaultText());
		}).dimensions(fieldX, startY + rowHeight * 10, 96, 20).build());

		this.addDrawableChild(ButtonWidget.builder(Text.of("Reset Defaults"), button -> this.derk$resetToDefaults())
				.dimensions(centerX - 155, this.height - 52, 100, 20)
				.build());
		this.addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), button -> this.close())
				.dimensions(centerX - 50, this.height - 52, 100, 20)
				.build());
		this.addDrawableChild(ButtonWidget.builder(Text.of("Save"), button -> this.derk$save())
				.dimensions(centerX + 55, this.height - 52, 100, 20)
				.build());
	}

	@Override
	public void close() {
		this.client.setScreen(this.parent);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, this.width, this.height, 0xCC101014);
		int centerX = this.width / 2;
		int panelX = centerX - 190;
		int panelY = 34;
		int panelWidth = 380;
		int panelHeight = 430;
		int labelX = panelX + 18;
		int fieldX = centerX + 56;
		int startY = 62;
		int rowHeight = 34;

		context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xAA191C22);
		this.derk$drawBorder(context, panelX, panelY, panelWidth, panelHeight, 0xFF404652);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 18, 0xFFFFFF);
		if (!this.errorText.getString().isEmpty()) {
			context.drawCenteredTextWithShadow(this.textRenderer, this.errorText, centerX, this.height - 78, 0xFF6B6B);
		}

		super.render(context, mouseX, mouseY, delta);
		this.derk$drawRowDescription(context, labelX, startY, "Pick a color visually or type a hex code.");
		this.derk$drawRowDescription(context, labelX, startY + rowHeight, "How long chest highlights stay visible.");
		this.derk$drawRowDescription(context, labelX, startY + rowHeight * 2, "Search radius around the crafting table.");
		this.derk$drawRowDescription(context, labelX, startY + rowHeight * 3, "Opacity of the filled highlight overlay.");
		this.derk$drawRowDescription(context, labelX, startY + rowHeight * 4, "How often the nearby list refreshes while open.");
		this.derk$drawRowDescription(context, labelX, startY + rowHeight * 5, "Turn world chest highlighting on or off without breaking locate targeting.");
		this.derk$drawRowDescription(context, labelX, startY + rowHeight * 6, "Show the floating distance text above highlights.");
		this.derk$drawRowDescription(context, labelX, startY + rowHeight * 7, "Rotate the camera toward the nearest matching chest on click.");
		this.derk$drawRowDescription(context, labelX, startY + rowHeight * 8, "Show a smooth trail to the located chest while keeping snap aim optional.");
		this.derk$drawRowDescription(context, labelX, startY + rowHeight * 9, "Choose the particle used for the locate trail effect.");
		this.derk$drawRowDescription(context, labelX, startY + rowHeight * 10, "Whether the nearby panel starts opened.");
		this.derk$drawColorPreview(context, fieldX + 172, startY + 2);
	}

	private ButtonWidget derk$createLabelButton(int x, int y, int width, String label) {
		return this.addDrawableChild(ButtonWidget.builder(Text.of(label), button -> {
			// Intentional no-op: this is a visual label button.
		}).dimensions(x, y, width, 20).build());
	}

	private TextFieldWidget derk$createField(int x, int y, int width, String value) {
		TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y, width, 20, Text.empty());
		field.setText(value);
		this.addDrawableChild(field);
		return field;
	}

	private void derk$drawRowDescription(DrawContext context, int labelX, int y, String description) {
		context.drawTextWithShadow(this.textRenderer, Text.of(description), labelX, y + 24, 0xB9C0CB);
	}

	private void derk$drawColorPreview(DrawContext context, int x, int y) {
		try {
			int color = this.derk$parseHexColor(this.highlightColorField.getText());
			context.fill(x, y, x + 20, y + 20, 0xFF000000 | color);
			this.derk$drawBorder(context, x, y, 20, 20, 0xFFFFFFFF);
		} catch (IllegalArgumentException ignored) {
			context.fill(x, y, x + 20, y + 20, 0xFF550000);
			this.derk$drawBorder(context, x, y, 20, 20, 0xFFFF7777);
		}
	}

	private void derk$drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
		context.fill(x, y, x + width, y + 1, color);
		context.fill(x, y + height - 1, x + width, y + height, color);
		context.fill(x, y, x + 1, y + height, color);
		context.fill(x + width - 1, y, x + width, y + height, color);
	}

	private Text derk$getHighlightEnabledText() {
		return Text.of(this.showHighlighter ? "On" : "Off");
	}

	private Text derk$getDistanceLabelText() {
		return Text.of(this.showDistanceLabel ? "On" : "Off");
	}

	private Text derk$getPanelDefaultText() {
		return Text.of(this.nearbyPanelOpenByDefault ? "Open" : "Closed");
	}

	private Text derk$getSnapAimText() {
		return Text.of(this.snapAimToChest ? "On" : "Off");
	}

	private Text derk$getSmokeTrailText() {
		return Text.of(this.showSmokeTrail ? "On" : "Off");
	}

	private Text derk$getTrailParticleText() {
		return Text.of(this.locateTrailParticle.getLabel());
	}

	private void derk$resetToDefaults() {
		ConfigData defaults = ConfigData.defaults();
		this.highlightColorField.setText(String.format(Locale.ROOT, "#%06X", defaults.highlightColor));
		this.highlightDurationField.setText(this.derk$formatSeconds(defaults.highlightDurationTicks));
		this.nearbyRadiusField.setText(Integer.toString(defaults.nearbyRadius));
		this.highlightOpacityField.setText(Integer.toString(defaults.highlightOpacityPercent));
		this.autoRefreshField.setText(this.derk$formatSeconds(defaults.autoRefreshTicks));
		this.showHighlighter = defaults.showHighlighter;
		this.showDistanceLabel = defaults.showDistanceLabel;
		this.snapAimToChest = defaults.snapAimToChest;
		this.showSmokeTrail = defaults.resolveLocateTrail();
		this.locateTrailParticle = defaults.locateTrailParticle;
		this.nearbyPanelOpenByDefault = defaults.nearbyPanelOpenByDefault;
		this.highlightEnabledButton.setMessage(this.derk$getHighlightEnabledText());
		this.showDistanceLabelButton.setMessage(this.derk$getDistanceLabelText());
		this.snapAimButton.setMessage(this.derk$getSnapAimText());
		this.smokeTrailButton.setMessage(this.derk$getSmokeTrailText());
		this.trailParticleButton.setMessage(this.derk$getTrailParticleText());
		this.panelDefaultButton.setMessage(this.derk$getPanelDefaultText());
		this.errorText = Text.empty();
	}

	private void derk$save() {
		try {
			ConfigData updated = EasyInventoryCrafterConfig.snapshot();
			updated.highlightColor = this.derk$parseHexColor(this.highlightColorField.getText());
			updated.highlightDurationTicks = this.derk$parseSecondsToTicks(this.highlightDurationField.getText(), 0.5, 60.0);
			updated.nearbyRadius = this.derk$parseInt(this.nearbyRadiusField.getText(), 1, 64, "Nearby radius");
			updated.highlightOpacityPercent = this.derk$parseInt(this.highlightOpacityField.getText(), 5, 100, "Highlight opacity");
			updated.autoRefreshTicks = this.derk$parseSecondsToTicks(this.autoRefreshField.getText(), 0.25, 30.0);
			updated.showHighlighter = this.showHighlighter;
			updated.showDistanceLabel = this.showDistanceLabel;
			updated.snapAimToChest = this.snapAimToChest;
			updated.showLocateTrail = this.showSmokeTrail;
			updated.showSmokeTrail = null;
			updated.locateTrailParticle = this.locateTrailParticle;
			updated.nearbyPanelOpenByDefault = this.nearbyPanelOpenByDefault;
			EasyInventoryCrafterConfig.update(updated);
			NearbyItemsClientState.requestUpdate();
			this.close();
		} catch (IllegalArgumentException exception) {
			this.errorText = Text.of(exception.getMessage());
		}
	}

	private int derk$parseHexColor(String raw) {
		String normalized = raw.trim();
		if (normalized.startsWith("#")) {
			normalized = normalized.substring(1);
		}
		if (!normalized.matches("[0-9a-fA-F]{6}")) {
			throw new IllegalArgumentException("Highlight color must be a 6-digit hex value.");
		}
		return Integer.parseInt(normalized, 16);
	}

	private int derk$parseSecondsToTicks(String raw, double minSeconds, double maxSeconds) {
		double value;
		try {
			value = Double.parseDouble(raw.trim());
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("Enter a valid number of seconds.");
		}
		if (value < minSeconds || value > maxSeconds) {
			throw new IllegalArgumentException(String.format("Seconds must be between %.2f and %.2f.", minSeconds, maxSeconds));
		}
		return Math.max(1, (int)Math.round(value * 20.0));
	}

	private int derk$parseInt(String raw, int min, int max, String label) {
		int value;
		try {
			value = Integer.parseInt(raw.trim());
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException(label + " must be a whole number.");
		}
		if (value < min || value > max) {
			throw new IllegalArgumentException(label + " must be between " + min + " and " + max + ".");
		}
		return value;
	}

	private String derk$formatSeconds(int ticks) {
		return String.format(java.util.Locale.ROOT, "%.2f", ticks / 20.0);
	}
}