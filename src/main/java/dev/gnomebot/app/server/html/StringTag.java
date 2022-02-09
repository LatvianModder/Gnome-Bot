package dev.gnomebot.app.server.html;

import java.io.Writer;

public class StringTag extends Tag {
	public static String fixHtml(String string) {
		return string.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	public final String string;

	public StringTag(String string) {
		this.string = string;
	}

	@Override
	public void write(Writer writer) throws Throwable {
		writer.write(fixHtml(string));
	}
}
