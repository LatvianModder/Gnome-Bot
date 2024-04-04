package dev.gnomebot.app.server;

import dev.latvian.apps.webutils.html.RootTag;
import dev.latvian.apps.webutils.html.Tag;

public class GnomeRootTag extends RootTag {
	public static GnomeRootTag createSimple(String path, String title) {
		return new GnomeRootTag(path, title, "");
	}

	public Tag content;

	public GnomeRootTag(String path, String title, String description) {
		super(path, title + " - GnomeBot", description);
		head.meta("name", "theme-color", "content", "#262728");
		head.stylesheet("/assets/style.css");
		body.iframe("invisibleframe").style("display:none;");
		content = body.div().classes("content");
		content.h1().string(title);
		content.br();
	}

	@Override
	public String getSiteName() {
		return "GnomeBot";
	}

	@Override
	public String getRootUrl() {
		return "https://gnomebot.dev";
	}

	@Override
	public String getIconPath() {
		return "logo_64.png";
	}

	@Override
	public int getIconSize() {
		return 24;
	}

	@Override
	public String getAuthor() {
		return "latvian.dev";
	}
}
