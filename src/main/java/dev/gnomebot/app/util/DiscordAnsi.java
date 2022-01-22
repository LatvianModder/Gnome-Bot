package dev.gnomebot.app.util;

public interface DiscordAnsi {
	String RESET = "\u001B[0m";

	String BLACK = "\u001B[0;30m";
	String RED = "\u001B[0;31m";
	String GREEN = "\u001B[0;32m";
	String ORANGE = "\u001B[0;33m";
	String BLUE = "\u001B[0;34m";
	String MAGENTA = "\u001B[0;35m";
	String CYAN = "\u001B[0;36m";
	String INVERTED = "\u001B[0;37m";

	String[] COLORS = {
			BLACK,
			RED,
			GREEN,
			ORANGE,
			BLUE,
			MAGENTA,
			CYAN,
			INVERTED
	};

	String DARK_CYAN_BG = "\u001B[0;40m";
	String ORANGE_BG = "\u001B[0;41m";
	String GRAY_1_BG = "\u001B[0;42m";
	String GRAY_2_BG = "\u001B[0;43m";
	String GRAY_3_BG = "\u001B[0;44m";
	String BLURPLE_BG = "\u001B[0;45m";
	String GRAY_4_BG = "\u001B[0;46m";
	String WHITE_BG = "\u001B[0;47m";

	static String progressBar(float progress, String text) {
		if (text.length() > 41) {
			text = text.substring(0, 41);
		}

		StringBuilder sb = new StringBuilder(" ".repeat(41));
		int start = (41 - text.length()) / 2;
		sb.replace(start, start + text.length(), text);

		if (progress < 0F) {
			sb.insert(0, ORANGE_BG);
		} else if (progress > 0F) {
			sb.insert(Utils.ceil(Math.min(progress * 41F, 41F)), DARK_CYAN_BG);
			sb.insert(0, BLURPLE_BG);
		} else {
			sb.insert(0, DARK_CYAN_BG);
		}

		sb.append(RESET);
		return sb.toString();
	}
}
