package dev.gnomebot.app.util;

import java.util.regex.Pattern;

public interface Ansi {
	Pattern PATTERN = Pattern.compile("\u001B\\[(?:\\d;)?\\d+[mD]");
	String RESET = "\u001B[0m";

	String LEFT = "\u001B[1000D";
	String BOLD = "\u001B[1m";
	String UNDERLINE = "\u001B[4m";
	String REVERSE = "\u001B[7m";

	String BLACK = "\u001B[0;30m";
	String DARK_RED = "\u001B[0;31m";
	String GREEN = "\u001B[0;32m";
	String ORANGE = "\u001B[0;33m";
	String BLUE = "\u001B[0;34m";
	String PURPLE = "\u001B[0;35m";
	String TEAL = "\u001B[0;36m";
	String GRAY = "\u001B[0;37m";

	String DARK_GRAY = "\u001B[1;30m";
	String RED = "\u001B[1;31m";
	String OLIVE = "\u001B[1;32m";
	String YELLOW = "\u001B[1;33m";
	String LIGHT_BLUE = "\u001B[1;34m";
	String MAGENTA = "\u001B[1;35m";
	String CYAN = "\u001B[1;36m";
	String LIGHT_GRAY = "\u001B[1;37m";
}
