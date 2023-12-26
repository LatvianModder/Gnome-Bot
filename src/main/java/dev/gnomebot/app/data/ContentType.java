package dev.gnomebot.app.data;

import dev.gnomebot.app.data.complex.ComplexMessage;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.util.MessageBuilder;
import dev.latvian.apps.webutils.data.Pair;
import discord4j.common.util.Snowflake;
import org.jetbrains.annotations.Nullable;

public abstract class ContentType {
	public static final ContentType TEXT = new ContentType() {
		@Override
		public MessageBuilder render(@Nullable CommandReader reader, Object cached, Snowflake sender) {
			return MessageBuilder.create(String.valueOf(cached));
		}
	};

	public static final ContentType COMPLEX = new ContentType() {
		@Override
		public MessageBuilder render(@Nullable CommandReader reader, Object cached, Snowflake sender) {
			var msg = MessageBuilder.create();
			((ComplexMessage) cached).apply(msg, sender);
			return msg;
		}
	};

	public static final ContentType JS = new ContentType() {
		@Override
		public MessageBuilder render(@Nullable CommandReader reader, Object cached, Snowflake sender) {
			return MessageBuilder.create(String.valueOf(cached));
		}
	};

	public static final ContentType MACRO_BUNDLE = new ContentType() {
		@Override
		public MessageBuilder render(@Nullable CommandReader reader, Object cached, Snowflake sender) {
			return ((MacroBundle) cached).render(reader, sender);
		}
	};

	public abstract MessageBuilder render(@Nullable CommandReader reader, Object cached, Snowflake sender);

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

		int inl = content.indexOf('\n');

		if (inl == -1) {
			return Pair.of(TEXT, content);
		} else {
			var first = content.substring(0, content.indexOf('\n')).replace(" ", "");

			if (first.equals("//cpx") || first.equals("//complex")) {
				return Pair.of(COMPLEX, ComplexMessage.parse(content));
			} else if (first.equals("//js") || first.equals("//javascript")) {
				return Pair.of(JS, content);
			} else if (first.equals("//macro-bundle")) {
				return Pair.of(MACRO_BUNDLE, MacroBundle.parse(gc, content));
			} else {
				return Pair.of(TEXT, content);
			}
		}
	}
}
