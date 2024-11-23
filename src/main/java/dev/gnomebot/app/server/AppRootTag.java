package dev.gnomebot.app.server;

import dev.gnomebot.app.App;
import dev.latvian.apps.webutils.html.RootTag;
import dev.latvian.apps.webutils.html.Tag;

public class AppRootTag extends RootTag {
	public final App app;
	public Tag content;

	public AppRootTag(AppRequest req, String title, String description) {
		super("/" + req.path(), title + " - GnomeBot", description);
		this.app = req.app;
		this.head.meta("name", "theme-color", "content", "#262728");
		this.head.stylesheet("/assets/style.css");
		this.head.deferScript("/assets/script.js");
		this.body.iframe("invisibleframe").style("display:none;");
		this.content = body.div().classes("content");
		this.content.h1().string(title);
		this.content.br();
	}

	@Override
	public String getSiteName() {
		return "GnomeBot";
	}

	@Override
	public String getRootUrl() {
		// RAAH
		return App.instance.config.web.panel_url;
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
