package dev.gnomebot.app.util;

import dev.gnomebot.app.App;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class Table {
	public static class Cell {
		private String value = "";
		private String unformattedValue = "";
		private char padding = ' ';
		private boolean alignRight = false;

		private Cell() {
		}

		public Cell value(Object v) {
			value = String.valueOf(v);
			unformattedValue = Ansi.PATTERN.matcher(value).replaceAll("");
			return this;
		}

		public Cell padding(char p) {
			padding = p;
			return this;
		}

		public Cell alignRight() {
			alignRight = true;
			return this;
		}
	}

	public static String escapeCSVSpecialCharacters(@Nullable String data) {
		if (data == null) {
			return "null";
		}

		String escapedData = data.replaceAll("\\R", " ");
		if (data.contains(",") || data.contains("\"") || data.contains("'")) {
			data = data.replace("\"", "\"\"");
			escapedData = "\"" + data + "\"";
		}
		return escapedData;

		//return "\"" + data.replace("\"", "\"\"") + "\"";
	}

	public final Cell[] head;
	public final List<Cell[]> rows;

	public Table(String... h) {
		head = new Cell[h.length];
		rows = new ArrayList<>();

		for (int i = 0; i < head.length; i++) {
			head[i] = new Cell().value(Ansi.BOLD + h[i]);
		}
	}

	public Cell[] addRow() {
		Cell[] cells = new Cell[head.length];

		for (int i = 0; i < head.length; i++) {
			cells[i] = new Cell();
		}

		rows.add(cells);
		return cells;
	}

	public void addRow(Object... h) {
		Cell[] c = addRow();

		for (int i = 0; i < h.length; i++) {
			c[i].value(h[i]);
		}
	}

	private void printLine(List<String> lines, boolean colors, StringBuilder sb, int[] max, int type) {
		sb.setLength(0);
		sb.append(type == 0 ? '┌' : type == 1 ? '├' : '└');

		for (int i : max) {
			for (int k = 0; k < i + 2; k++) {
				sb.append('─');
			}

			sb.append(type == 0 ? '┬' : type == 1 ? '┼' : '┴');
		}

		sb.setCharAt(sb.length() - 1, type == 0 ? '┐' : type == 1 ? '┤' : '┘');

		if (colors) {
			lines.add(sb.toString());
		} else {
			lines.add(Ansi.PATTERN.matcher(sb.toString()).replaceAll(""));
		}
	}

	private void printRow(List<String> lines, boolean colors, StringBuilder sb, int[] max, Cell[] cells) {
		sb.setLength(0);
		sb.append('│');

		for (int i = 0; i < max.length; i++) {
			sb.append(' ');

			if (cells[i].alignRight) {
				for (int j = 0; j < (max[i] - cells[i].unformattedValue.length()); j++) {
					sb.append(cells[i].padding);
				}
			}

			sb.append(cells[i].value);
			sb.append(Ansi.RESET);

			if (!cells[i].alignRight) {
				for (int j = 0; j < (max[i] - cells[i].unformattedValue.length()); j++) {
					sb.append(cells[i].padding);
				}
			}

			sb.append(" │");
		}

		if (colors) {
			lines.add(sb.toString());
		} else {
			lines.add(Ansi.PATTERN.matcher(sb.toString()).replaceAll(""));
		}
	}

	public List<String> getCSVLines(boolean includeHead) {
		List<String> lines = new ArrayList<>();

		if (includeHead) {
			addCSVLine(lines, head);
		}

		for (Cell[] c : rows) {
			addCSVLine(lines, c);
		}

		return lines;
	}

	public byte[] getCSVBytes(boolean includeHead) {
		return String.join("\n", getCSVLines(includeHead)).getBytes(StandardCharsets.UTF_8);
	}

	private void addCSVLine(List<String> lines, Cell[] cells) {
		lines.add(Arrays.stream(cells).map(c -> escapeCSVSpecialCharacters(c.unformattedValue)).collect(Collectors.joining(",")));
	}

	public List<String> getLines(boolean colors) {
		List<String> lines = new ArrayList<>();
		if (head.length == 0) {
			lines.add("┌───────┐");
			lines.add("│ Empty │");
			lines.add("└───────┘");
			return lines;
		}

		int[] max = new int[head.length];

		for (int i = 0; i < head.length; i++) {
			max[i] = head[i].unformattedValue.length();
		}

		for (Cell[] cells : rows) {
			for (int i = 0; i < head.length; i++) {
				max[i] = Math.max(max[i], cells[i].unformattedValue.length());
			}
		}

		StringBuilder sb = new StringBuilder();
		printLine(lines, colors, sb, max, 0);
		printRow(lines, colors, sb, max, head);
		printLine(lines, colors, sb, max, 1);

		for (Cell[] cells : rows) {
			printRow(lines, colors, sb, max, cells);
		}

		printLine(lines, colors, sb, max, 2);
		return lines;
	}

	public void print() {
		getLines(true).forEach(App::info);
	}
}