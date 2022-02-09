package dev.gnomebot.app.server.html;

import dev.gnomebot.app.App;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.handler.FileResponse;

import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public abstract class Tag {
	public Tag parent = null;

	public Tag end() {
		return parent;
	}

	public RootTag getRoot() {
		return parent.getRoot();
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

	public abstract void write(Writer writer) throws Throwable;

	@Override
	public String toString() {
		StringWriter writer = new StringWriter();

		try {
			write(writer);
		} catch (OutOfMemoryError error) {
			App.error("Out of memory while generating HTML:");
			error.printStackTrace();
		} catch (Throwable error) {
			error.printStackTrace();
		}

		return writer.toString();
	}

	public FileResponse asResponse(HTTPResponseCode code) {
		return FileResponse.of(code, "text/html; charset=utf-8", getRoot().toString().getBytes(StandardCharsets.UTF_8));
	}

	public FileResponse asResponse() {
		return asResponse(HTTPResponseCode.OK);
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

	public Tag span(String className) {
		return span().addClass(className);
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

	public Tag script(String script) {
		return paired("script").string(script);
	}
}
