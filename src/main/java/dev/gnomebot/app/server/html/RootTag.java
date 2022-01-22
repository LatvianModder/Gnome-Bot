package dev.gnomebot.app.server.html;

public class RootTag extends PairedTag {
	public static RootTag create() {
		return new RootTag();
	}

	private RootTag() {
		super("html");
		attr("lang", "en");
	}

	@Override
	public void build(StringBuilder builder) {
		builder.append("<!DOCTYPE html>\n");
		super.build(builder);
	}
}
