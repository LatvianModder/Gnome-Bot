package dev.gnomebot.app.data;

import dev.gnomebot.app.data.complex.ComplexMessage;
import dev.gnomebot.app.data.complex.ComplexMessageRenderContext;
import dev.gnomebot.app.util.MessageBuilder;
import dev.latvian.apps.webutils.data.Pair;

import java.util.regex.Pattern;

public abstract class ContentType {
	private static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile("^//\\s*([\\w-]+)$");

	public static final ContentType TEXT = new ContentType("text") {
		@Override
		public MessageBuilder render(ComplexMessageRenderContext ctx) {
			return MessageBuilder.create(String.valueOf(ctx.cached)).noComponents().noEmbeds();
		}
	};

	public static final ContentType COMPLEX = new ContentType("complex") {
		@Override
		public MessageBuilder render(ComplexMessageRenderContext ctx) {
			var msg = MessageBuilder.create();
			((ComplexMessage) ctx.cached).apply(msg, ctx);
			return msg;
		}
	};

	public static final ContentType JS = new ContentType("js") {
		@Override
		public MessageBuilder render(ComplexMessageRenderContext ctx) {
			// Not implemented
			return TEXT.render(ctx);
		}
	};

	public static final ContentType MACRO_BUNDLE = new ContentType("macro-bundle") {
		@Override
		public MessageBuilder render(ComplexMessageRenderContext ctx) {
			return ((MacroBundle) ctx.cached).render(ctx);
		}
	};

	public static final ContentType MACRO_ALIAS = new ContentType("macro-alias") {
		@Override
		public MessageBuilder render(ComplexMessageRenderContext ctx) {
			var name = String.valueOf(ctx.cached).split("\n")[1].trim();
			var macro = ctx.targetGuild.getMacro(name);

			if (macro == null) {
				return MessageBuilder.create("Macro `" + name + "` doesn't exist!").noComponents().noEmbeds();
			}

			var macroType = ContentType.parse(ctx.targetGuild, macro.getContent());

			if (macroType.a().name.equals("macro-alias")) {
				return MessageBuilder.create("You can't create an alias of an alias!").noComponents().noEmbeds();
			}

			var ctx2 = ctx.copy();
			ctx2.cached = macroType.b();
			return macroType.a().render(ctx2);
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

	public abstract MessageBuilder render(ComplexMessageRenderContext ctx);

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
		}

		var matcher = CONTENT_TYPE_PATTERN.matcher(content.substring(0, inl));

		if (matcher.find()) {
			return switch (matcher.group(1)) {
				case "cpx", "complex" -> Pair.of(COMPLEX, ComplexMessage.parse(gc, content));
				case "js", "javascript" -> Pair.of(JS, content);
				case "macro-bundle", "bundle" -> Pair.of(MACRO_BUNDLE, MacroBundle.parse(gc, content));
				case "macro-alias", "alias" -> Pair.of(MACRO_ALIAS, content);
				default -> Pair.of(TEXT, content);
			};
		}

		return Pair.of(TEXT, content);
	}
}
