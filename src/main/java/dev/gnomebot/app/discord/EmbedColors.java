package dev.gnomebot.app.discord;

import discord4j.rest.util.Color;

public interface EmbedColors {
	Color GRAY = Color.of(0x2F3136);
	Color RED = Color.of(0xF04848);
	Color GREEN = Color.of(0x48F048);
	Color TEAL = Color.of(0x47EFC8);
	Color BLUE = Color.of(0x0094FF);

	static Color rgb(String color) {
		return Color.of(Long.decode(color).intValue());
	}

	static Color rgb(int r, int g, int b) {
		return Color.of(r, g, b);
	}
}
