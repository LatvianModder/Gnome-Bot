package dev.gnomebot.app.server.html;

public class StringTag extends Tag {
	public static String fixHtml(String string) {
		return string.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	public final String string;

	public StringTag(String string) {
		this.string = string;
	}

	@Override
	public void build(StringBuilder builder) {
		builder.append(fixHtml(string));
	}
}
