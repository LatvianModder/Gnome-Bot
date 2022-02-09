package dev.gnomebot.app.server.html;

import java.io.Writer;
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

		writer.write('>');

		if (this.content != null && !this.content.isEmpty()) {
			for (Tag tag : this.content) {
				tag.write(writer);
			}
		}

		writer.write("</");
		writer.write(this.name);
		writer.write('>');
	}
}
