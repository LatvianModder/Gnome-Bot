package dev.gnomebot.app.server.html;

import dev.gnomebot.app.App;
import dev.gnomebot.app.server.handler.FileResponse;

import java.nio.charset.StandardCharsets;

public abstract class Tag {
	public Tag parent = null;

	public Tag end() {
		return parent;
	}

	public void add(Tag tag) {
		throw new IllegalStateException("This tag type does not support children tags");
	}

	public Tag attr(String key, Object value) {
		throw new IllegalStateException("This tag type does not support attributes");
	}

	public Tag addClass(String className) {
		throw new IllegalStateException("This tag type does not support attributes");
	}

	public abstract void build(StringBuilder builder);

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		build(builder);
		return builder.toString();
	}

	public FileResponse asResponse() {
		return FileResponse.of("text/html; charset=utf-8", toString().getBytes(StandardCharsets.UTF_8));
	}

	public Tag string(Object string) {
		add(new StringTag(String.valueOf(string)));
		return this;
	}

	public UnpairedTag unpaired(String name) {
		UnpairedTag tag = new UnpairedTag(name);
		add(tag);
		return tag;
	}

	public PairedTag paired(String name) {
		PairedTag tag = new PairedTag(name);
		add(tag);
		return tag;
	}

	public PairedTag head(String title, String url) {
		PairedTag head = paired("head");
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
		head.meta("property", "og:type", "content", "website");
		head.meta("property", "og:url", "content", App.url(url));
		head.meta("property", "og:image", "content", "/logo24.png");
		head.meta("property", "og:image:width", "content", "24");
		head.meta("property", "og:image:height", "content", "24");
		return head;
	}

	public Tag meta(String key1, Object value1, String key2, Object value2) {
		return unpaired("meta").attr(key1, value1).attr(key2, value2).end();
	}

	public Tag meta(String key, Object value) {
		return meta(key, value, "", "");
	}

	public Tag link(String key1, Object value1, String key2, Object value2) {
		return unpaired("link").attr(key1, value1).attr(key2, value2).end();
	}

	public Tag link(String key, Object value) {
		return link(key, value, "", "");
	}

	public Tag br() {
		return unpaired("br");
	}

	public Tag hr() {
		return unpaired("hr");
	}

	public Tag div() {
		return paired("div");
	}

	public Tag span() {
		return paired("span");
	}

	public Tag p() {
		return paired("p");
	}

	public Tag h1() {
		return paired("h1");
	}

	public Tag h2() {
		return paired("h2");
	}

	public Tag h3() {
		return paired("h3");
	}

	public Tag a(String url) {
		if (url.isEmpty()) {
			return paired("a");
		}

		return paired("a").attr("href", url);
	}
}
