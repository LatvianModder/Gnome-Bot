package dev.gnomebot.app.server.html;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class CombinedTag extends Tag {
	public List<Tag> content;

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
		if (this.content != null && !this.content.isEmpty()) {
			for (Tag tag : this.content) {
				tag.write(writer);
			}
		}
	}
}
