package dev.gnomebot.app.server.html;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PairedTag extends UnpairedTag {
	public List<Tag> content;

	public PairedTag(String name) {
		super(name);
		this.content = null;
	}

	@Override
	public void add(Tag content) {
		if (this.content == null) {
			this.content = new ArrayList<>();
		}

		if (!this.content.isEmpty() && content instanceof StringTag sc1 && this.content.get(this.content.size() - 1) instanceof StringTag sc0) {
			this.content.set(this.content.size() - 1, new StringTag(sc0.string + sc1.string));
		} else {
			content.parent = this;
			this.content.add(content);
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

		builder.append(">");

		for (Tag tag : this.content) {
			tag.build(builder);
		}

		builder.append("</").append(this.name).append(">");
	}
}
