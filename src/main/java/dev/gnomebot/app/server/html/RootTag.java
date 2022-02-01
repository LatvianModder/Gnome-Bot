package dev.gnomebot.app.server.html;

public class RootTag extends PairedTag {
	public static RootTag create() {
		return new RootTag();
	}

	public static Tag createSimple(String path, String title) {
		RootTag root = create();
		root.head(path, title);
		Tag body = root.paired("body");
		Tag content = body.div().addClass("content");
		content.h2().string(title);
		content.br();
		return content;
	}

	private RootTag() {
		super("html");
		attr("lang", "en");
	}

	@Override
	public RootTag getRoot() {
		return this;
	}

	@Override
	public void build(StringBuilder builder) {
		builder.append("<!DOCTYPE html>\n");
		super.build(builder);
	}
}
