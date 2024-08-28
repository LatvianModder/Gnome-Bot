package dev.gnomebot.app.data;

import dev.gnomebot.app.data.complex.ComplexMessage;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.util.MessageBuilder;
import dev.latvian.apps.webutils.data.Pair;
import org.jetbrains.annotations.Nullable;

public abstract class ContentType {
	public static final ContentType TEXT = new ContentType("text") {
		@Override
		public MessageBuilder render(GuildCollections gc, @Nullable CommandReader reader, Object cached, long sender) {
			return MessageBuilder.create(String.valueOf(cached)).noComponents().noEmbeds();
		}
	};

	public static final ContentType COMPLEX = new ContentType("complex") {
		@Override
		public MessageBuilder render(GuildCollections gc, @Nullable CommandReader reader, Object cached, long sender) {
			var msg = MessageBuilder.create();
			((ComplexMessage) cached).apply(gc, msg, sender);
			return msg;
		}
	};

	public static final ContentType JS = new ContentType("js") {
		@Override
		public MessageBuilder render(GuildCollections gc, @Nullable CommandReader reader, Object cached, long sender) {
			return MessageBuilder.create(String.valueOf(cached)).noComponents().noEmbeds();
		}
	};

	public static final ContentType MACRO_BUNDLE = new ContentType("macro-bundle") {
		@Override
		public MessageBuilder render(GuildCollections gc, @Nullable CommandReader reader, Object cached, long sender) {
			return ((MacroBundle) cached).render(gc, reader, sender);
		}
	};

	public static final ContentType MACRO_ALIAS = new ContentType("macro-alias") {
		@Override
		public MessageBuilder render(GuildCollections gc, @Nullable CommandReader reader, Object cached, long sender) {
			var name = String.valueOf(cached).split("\n")[1].trim();
			var macro = gc.getMacro(name);
			if (macro == null) return MessageBuilder.create("Macro `" + name + "` doesn't exist!").noComponents().noEmbeds();
			var macroType = ContentType.parse(gc, macro.getContent());
			if (macroType.a().name.equals("macro-alias")) return MessageBuilder.create("You can't create an alias of an alias!").noComponents().noEmbeds();
			return macroType.a().render(gc, reader, macroType.b(), sender);
		}
	};

	public final String name;

	public ContentType(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	public abstract MessageBuilder render(GuildCollections gc, @Nullable CommandReader reader, Object cached, long sender);

	public static String encodeMentions(String content) {
		return content
				.replaceAll("<@&(\\d+)>", "role:$1")
				.replaceAll("<@(\\d+)>", "user:$1")
				.replace("@here", "mention:here")
				.replace("@everyone", "mention:everyone");
	}

	public static String decodeMentions(String content) {
		return content
				.replaceAll("role:(\\d+)", "<@&$1>")
				.replaceAll("user:(\\d+)", "<@$1>")
				.replace("mention:here", "@here")
				.replace("mention:everyone", "@everyone");
	}

	public static Pair<ContentType, Object> parse(GuildCollections gc, String content) {
		content = content.trim();

		var inl = content.indexOf('\n');
		if (inl == -1) {
			return Pair.of(TEXT, content);
		} else {
			var first = content.substring(0, content.indexOf('\n')).replace(" ", "");
			return switch (first) {
				case "//cpx", "//complex" -> Pair.of(COMPLEX, ComplexMessage.parse(gc, content));
				case "//js", "//javascript" -> Pair.of(JS, content);
				case "//macro-bundle", "//bundle" -> Pair.of(MACRO_BUNDLE, MacroBundle.parse(gc, content));
				case "//macro-alias", "//alias" -> Pair.of(MACRO_ALIAS, content);
				default -> Pair.of(TEXT, content);
			};
		}
	}
}
