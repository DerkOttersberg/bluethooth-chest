package com.derk.easyinventorycrafter.client;

public interface NearbyPanelAccess {
	boolean derk$handleScroll(double mouseX, double mouseY, double verticalAmount);

	boolean derk$handleCharTyped(char chr, int modifiers);

	boolean derk$handleKeyPressed(int keyCode, int scanCode, int modifiers);

	boolean derk$handleMouseClick(double mouseX, double mouseY, int button);
}