package dev.gnomebot.app.server.html;

import dev.gnomebot.app.App;

import java.io.Writer;

public class RootTag extends PairedTag {
	public static RootTag create() {
		return new RootTag();
	}

	public static Tag createSimple(String path, String title) {
		RootTag root = create();
		root.setupHead(path, title);
		Tag body = root.paired("body");
		body.paired("iframe").attr("name", "invisibleframe").attr("style", "display:none;");
		Tag content = body.div().addClass("content");
		content.h2().string(title);
		content.br();
		return content;
	}

	public final Tag head;
	public final Tag body;

	private RootTag() {
		super("html");
		attr("lang", "en");
		head = paired("head");
		body = paired("body");
	}

	@Override
	public RootTag getRoot() {
		return this;
	}

	public void setupHead(String path, String title, String description) {
		head.meta("charset", "utf-8");
		head.link("rel", "icon", "href", "/favicon.ico");
		// head.meta("name", "viewport", "content", "width=device-width, initial-scale=1");
		head.meta("name", "theme-color", "content", "#262728");
		head.meta("name", "description", "content", "Gnome Bot Panel");
		head.link("rel", "apple-touch-icon", "href", "/logo192.png");
		head.link("rel", "manifest", "href", "/manifest.json");
		head.paired("title").string(title);
		head.link("rel", "stylesheet", "href", "/assets/style.css");
		head.meta("property", "og:site_name", "content", "Gnome Bot");
		head.meta("property", "og:title", "content", title);

		if (!description.isEmpty()) {
			head.meta("property", "og:description", "content", description);
			head.meta("name", "description", "content", description);
		}

		head.meta("property", "og:type", "content", "website");
		head.meta("property", "og:url", "content", App.url(path));
		head.meta("property", "og:image", "content", "/logo24.png");
		head.meta("property", "og:image:width", "content", "24");
		head.meta("property", "og:image:height", "content", "24");
	}

	public void setupHead(String path, String title) {
		setupHead(path, title, "");
	}

	@Override
	public void write(Writer writer) throws Throwable {
		writer.write("<!DOCTYPE html>");
		super.write(writer);
	}
}
