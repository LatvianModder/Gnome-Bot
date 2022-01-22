package dev.gnomebot.app.server.html;

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
	public void build(StringBuilder builder) {
		builder.append("<").append(this.name);

		if (this.attributes != null) {
			for (Map.Entry<String, String> entry : this.attributes.entrySet()) {
				builder.append(" ").append(entry.getKey()).append("=\"").append(StringTag.fixHtml(entry.getValue())).append("\"");
			}
		}

		builder.append(" />");
	}
}
