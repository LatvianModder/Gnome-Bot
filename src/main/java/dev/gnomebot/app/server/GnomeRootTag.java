package dev.gnomebot.app.server;

import dev.gnomebot.app.App;
import dev.latvian.apps.webutils.html.RootTag;
import dev.latvian.apps.webutils.html.Tag;

import java.util.function.Consumer;

public class GnomeRootTag {
	public static final Consumer<RootTag> DEFAULT = root -> {
	};

	public static Tag createSimple(String path, String title) {
		var root = RootTag.create(null, null);
		setupHead(root, path, title);
		Tag body = root.body;
		body.iframe("invisibleframe").style("display:none;");
		Tag content = body.div().classes("content");
		content.h2().string(title);
		content.br();
		return root;
	}

	public static void setupHead(RootTag root, String path, String title, String description) {
		root.head.link("rel", "icon", "href", "/favicon.ico");
		// root.head.meta("name", "viewport", "content", "width=device-width, initial-scale=1");
		root.head.meta("name", "theme-color", "content", "#262728");
		root.head.meta("name", "description", "content", "Gnome Bot Panel");
		root.head.link("rel", "apple-touch-icon", "href", "/logo192.png");
		root.head.link("rel", "manifest", "href", "/manifest.json");
		root.head.titleTag().string(title);
		root.head.link("rel", "stylesheet", "href", "/assets/style.css");
		root.head.meta("property", "og:site_name", "content", "Gnome Bot");
		root.head.meta("property", "og:title", "content", title);

		if (!description.isEmpty()) {
			root.head.meta("property", "og:description", "content", description);
			root.head.meta("name", "description", "content", description);
		}

		root.head.meta("property", "og:type", "content", "website");
		root.head.meta("property", "og:url", "content", App.url(path));
		root.head.meta("property", "og:image", "content", "/logo24.png");
		root.head.meta("property", "og:image:width", "content", "24");
		root.head.meta("property", "og:image:height", "content", "24");
	}

	public static void setupHead(RootTag root, String path, String title) {
		setupHead(root, path, title, "");
	}
}
