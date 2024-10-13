package dev.gnomebot.app.util;

import dev.latvian.apps.webutils.ansi.Ansi;
import dev.latvian.apps.webutils.ansi.AnsiCode;
import dev.latvian.apps.webutils.ansi.AnsiComponent;
import dev.latvian.apps.webutils.ansi.Log;
import dev.latvian.apps.webutils.html.PairedTag;
import dev.latvian.apps.webutils.html.Tag;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class Table {
	public final Table.Cell[] head;
	public final List<Table.Cell[]> rows;

	public static String escapeCSVSpecialCharacters(@Nullable String data) {
		if (data == null) {
			return "null";
		} else {
			String escapedData = data.replaceAll("\\R", " ");
			if (data.contains(",") || data.contains("\"") || data.contains("'")) {
				data = data.replace("\"", "\"\"");
				escapedData = "\"" + data + "\"";
			}

			return escapedData;
		}
	}

	public Table(String... h) {
		this.head = new Table.Cell[h.length];
		this.rows = new ArrayList();

		for (int i = 0; i < this.head.length; ++i) {
			this.head[i] = (new Table.Cell()).value(Ansi.bold(h[i]));
		}

	}

	public Table.Cell[] addRow() {
		Table.Cell[] cells = new Table.Cell[this.head.length];

		for (int i = 0; i < this.head.length; ++i) {
			cells[i] = new Table.Cell();
		}

		this.rows.add(cells);
		return cells;
	}

	public void addRow(Object... h) {
		Table.Cell[] c = this.addRow();

		for (int i = 0; i < h.length; ++i) {
			c[i].value(h[i]);
		}

	}

	private void printLine(List<String> lines, boolean colors, StringBuilder sb, int[] max, int type) {
		sb.setLength(0);
		sb.append(type == 0 ? '┌' : (type == 1 ? '├' : '└'));
		int[] var6 = max;
		int var7 = max.length;

		for (int var8 = 0; var8 < var7; ++var8) {
			int i = var6[var8];

			for (int k = 0; k < i + 2; ++k) {
				sb.append('─');
			}

			sb.append(type == 0 ? '┬' : (type == 1 ? '┼' : '┴'));
		}

		sb.setCharAt(sb.length() - 1, type == 0 ? '┐' : (type == 1 ? '┤' : '┘'));
		if (colors) {
			lines.add(sb.toString());
		} else {
			lines.add(Ansi.PATTERN.matcher(sb.toString()).replaceAll(""));
		}

	}

	private void printRow(List<String> lines, boolean colors, StringBuilder sb, int[] max, Table.Cell[] cells) {
		sb.setLength(0);
		sb.append('│');

		for (int i = 0; i < max.length; ++i) {
			sb.append(' ');
			int l = max[i] - cells[i].unformattedLength();
			int j;
			if (cells[i].alignRight) {
				for (j = 0; j < l; ++j) {
					sb.append(cells[i].padding);
				}
			}

			sb.append(cells[i].value);
			sb.append(AnsiCode.RESET);
			if (!cells[i].alignRight) {
				for (j = 0; j < l; ++j) {
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
		List<String> lines = new ArrayList();
		if (includeHead) {
			this.addCSVLine(lines, this.head);
		}

		Iterator var3 = this.rows.iterator();

		while (var3.hasNext()) {
			Table.Cell[] c = (Table.Cell[]) var3.next();
			this.addCSVLine(lines, c);
		}

		return lines;
	}

	public byte[] getCSVBytes(boolean includeHead) {
		return String.join("\n", this.getCSVLines(includeHead)).getBytes(StandardCharsets.UTF_8);
	}

	private void addCSVLine(List<String> lines, Table.Cell[] cells) {
		lines.add(Arrays.stream(cells).map((c) -> {
			return escapeCSVSpecialCharacters(c.unformattedValue());
		}).collect(Collectors.joining(",")));
	}

	public List<String> getLines(boolean colors) {
		List<String> lines = new ArrayList();
		if (this.head.length == 0) {
			lines.add("┌───────┐");
			lines.add("│ Empty │");
			lines.add("└───────┘");
			return lines;
		} else {
			int[] max = new int[this.head.length];

			for (int i = 0; i < this.head.length; ++i) {
				max[i] = this.head[i].unformattedLength();
			}

			Iterator var7 = this.rows.iterator();

			while (var7.hasNext()) {
				Table.Cell[] cells = (Table.Cell[]) var7.next();

				for (int i = 0; i < this.head.length; ++i) {
					max[i] = Math.max(max[i], cells[i].unformattedLength());
				}
			}

			StringBuilder sb = new StringBuilder();
			this.printLine(lines, colors, sb, max, 0);
			this.printRow(lines, colors, sb, max, this.head);
			this.printLine(lines, colors, sb, max, 1);
			Iterator var9 = this.rows.iterator();

			while (var9.hasNext()) {
				Table.Cell[] cells = (Table.Cell[]) var9.next();
				this.printRow(lines, colors, sb, max, cells);
			}

			this.printLine(lines, colors, sb, max, 2);
			return lines;
		}
	}

	public void print() {
		Log.info("\n" + String.join("\n", this.getLines(true)));
	}

	public Tag toTag() {
		Tag table = new PairedTag("table");
		Tag theadTag = table.thead();
		Tag headTag = theadTag.tr();
		Table.Cell[] var4 = this.head;
		int var5 = var4.length;

		for (int var6 = 0; var6 < var5; ++var6) {
			Table.Cell cell = var4[var6];
			cell.fill(headTag.th());
		}

		Tag tbodyTag = table.tbody();
		Iterator var13 = this.rows.iterator();

		while (var13.hasNext()) {
			Table.Cell[] cells = (Table.Cell[]) var13.next();
			Tag rowTag = tbodyTag.tr();
			Table.Cell[] var8 = cells;
			int var9 = cells.length;

			for (int var10 = 0; var10 < var9; ++var10) {
				Table.Cell cell = var8[var10];
				cell.fill(rowTag.td());
			}
		}

		return table;
	}

	public static class Cell {
		private AnsiComponent value = Ansi.of("");
		private Tag tag = null;
		private char padding = ' ';
		private boolean alignRight = false;

		private Cell() {
		}

		public Cell value(Object v) {
			this.value = Ansi.of(v);
			return this;
		}

		public Tag tag() {
			this.tag = new PairedTag("");
			return this.tag;
		}

		public Cell padding(char p) {
			this.padding = p;
			return this;
		}

		public Cell alignRight() {
			this.alignRight = true;
			return this;
		}

		public void fill(Tag cellTag) {
			if (this.tag != null) {
				cellTag.add(this.tag);
			} else {
				cellTag.string(this.value.toUnformattedString());
			}

		}

		public String unformattedValue() {
			return this.tag == null ? this.value.toUnformattedString() : this.tag.toRawString();
		}

		public int unformattedLength() {
			return this.unformattedValue().length();
		}
	}
}

