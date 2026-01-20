package com.derk.easyinventorycrafter.client;

import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.Click;

public interface NearbyPanelAccess {
	boolean derk$handleScroll(double mouseX, double mouseY, double verticalAmount);

	boolean derk$handleCharTyped(CharInput input);

	boolean derk$handleKeyPressed(KeyInput input);

	boolean derk$handleMouseClick(Click click, boolean doubleClick);
}