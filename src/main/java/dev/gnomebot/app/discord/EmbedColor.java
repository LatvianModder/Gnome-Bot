package dev.gnomebot.app.discord;

import discord4j.rest.util.Color;

import java.util.List;

public interface EmbedColor {
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

	static Color of(Object o) {
		if (o instanceof Color c) {
			return c;
		} else if (o instanceof CharSequence) {
			String s = o.toString().toLowerCase();

			return switch (s) {
				case "gray" -> GRAY;
				case "red" -> RED;
				case "green" -> GREEN;
				case "teal" -> TEAL;
				case "blue" -> BLUE;
				case "black" -> Color.DISCORD_BLACK;
				case "white" -> Color.DISCORD_WHITE;
				case "light_sea_green" -> Color.LIGHT_SEA_GREEN;
				case "medium_sea_green" -> Color.MEDIUM_SEA_GREEN;
				case "summer_sky" -> Color.SUMMER_SKY;
				case "deep_lilac" -> Color.DEEP_LILAC;
				case "ruby" -> Color.RUBY;
				case "moon_yellow" -> Color.MOON_YELLOW;
				case "tahiti_gold" -> Color.TAHITI_GOLD;
				case "cinnabar" -> Color.CINNABAR;
				case "submarine" -> Color.SUBMARINE;
				case "hoki" -> Color.HOKI;
				case "deep_sea" -> Color.DEEP_SEA;
				case "sea_green" -> Color.SEA_GREEN;
				case "endeavour" -> Color.ENDEAVOUR;
				case "vivid_violet" -> Color.VIVID_VIOLET;
				case "jazzberry_jam" -> Color.JAZZBERRY_JAM;
				case "dark_goldenrod" -> Color.DARK_GOLDENROD;
				case "rust" -> Color.RUST;
				case "brown" -> Color.BROWN;
				case "gray_chateau" -> Color.GRAY_CHATEAU;
				case "bismark" -> Color.BISMARK;
				default -> rgb(s);
			};
		} else if (o instanceof List<?> l && l.size() == 4 && l.get(0) instanceof Number n1 && l.get(1) instanceof Number n2 && l.get(2) instanceof Number n3) {
			return rgb(n1.intValue(), n2.intValue(), n3.intValue());
		}

		return GRAY;
	}
}
