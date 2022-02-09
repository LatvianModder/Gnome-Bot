package dev.gnomebot.app.server.html;

import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

public class UnpairedTag extends Tag {
	public final String name;
	protected Map<String, String> attributes;

	public UnpairedTag(String name) {
		this.name = name;
		this.attributes = null;
	}

	@Override
	public Tag attr(String key, Object value) {
		if (key.isEmpty()) {
			return this;
		}

		if (this.attributes == null) {
			this.attributes = new LinkedHashMap<>();
		}

		this.attributes.put(key, String.valueOf(value));
		return this;
	}

	@Override
	public Tag addClass(String className) {
		if (attributes == null || !attributes.containsKey("class")) {
			return attr("class", className);
		} else {
			return attr("class", attributes.get("class") + " " + className);
		}
	}

	@Override
	public void write(Writer writer) throws Throwable {
		writer.write('<');
		writer.write(this.name);

		if (this.attributes != null) {
			for (Map.Entry<String, String> entry : this.attributes.entrySet()) {
				writer.write(' ');
				writer.write(entry.getKey());
				writer.write("=\"");
				writer.write(StringTag.fixHtml(entry.getValue()));
				writer.write('"');
			}
		}

		writer.write(" />");
	}
}
