package dev.gnomebot.app.data;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.discord.command.ChatCommandSuggestion;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.MessageBuilder;
import discord4j.common.util.Snowflake;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class Macro extends WrappedDocument<Macro> {
	public static final Pattern REMOVE_MD_LINKS = Pattern.compile("\\[.+?]\\((.+?)\\)");

	private ChatCommandSuggestion chatCommandSuggestion;

	public Macro(WrappedCollection<Macro> c, MapWrapper d) {
		super(c, d);
	}

	public long getAuthor() {
		return document.getLong("author");
	}

	@Override
	public Date getDate() {
		return document.getDate("created");
	}

	public String getContent() {
		return document.getString("content");
	}

	public List<String> getExtra() {
		return document.getList("extra");
	}

	public int getUses() {
		return document.getInt("uses");
	}

	public long getSlashCommand() {
		return document.getLong("slash_command");
	}

	public MessageBuilder createMessage(Snowflake sender, boolean removeLinks) {
		return createMessage(getContent(), getExtra(), sender, removeLinks);
	}

	public static MessageBuilder createMessage(String contentString, List<String> extra, Snowflake sender, boolean removeLinks) {
		MessageBuilder builder = MessageBuilder.create();
		List<String> lines = new ArrayList<>();

		Collections.addAll(lines, contentString
				.replaceAll("role:(\\d+)", "<@&$1>")
				.replaceAll("user:(\\d+)", "<@$1>")
				.replace("mention:here", "@here")
				.replace("mention:everyone", "@everyone")
				.split("\n"));

		if (lines.isEmpty()) {
			lines.add("Missingno");
		}

		builder.content(lines);

		DiscordMessage.applyExtra(builder, extra, sender);
		return builder;
	}

	public void rename(String rename) {
		long l = setSlashCommand(false);

		document.map.put("name", rename);
		update("name", rename);

		if (l != 0L) {
			setSlashCommand(true);
		}
	}

	public void updateContent(String content, List<String> extra) {
		if (!content.equals(getContent())) {
			document.map.put("content", content);
			update("content", content);
		}

		if (!extra.isEmpty() && !extra.equals(getExtra())) {
			if (extra.contains("clear")) {
				document.map.remove("extra");
				update(Updates.unset("extra"));
			} else {
				document.map.put("extra", extra);
				update("extra", extra);
			}
		}
	}

	public long setSlashCommand(boolean b) {
		var gc = collection.gc;

		if (b) {
			String author = gc.db.app.discordHandler.getUserName(Snowflake.of(getAuthor())).orElse("Deleted User");

			var data = gc.getClient().getRestClient().getApplicationService().createGuildApplicationCommand(gc.db.app.discordHandler.applicationId, gc.guildId.asLong(), ApplicationCommandRequest.builder()
					.name(getName().toLowerCase())
					.description("Macro created by " + author)
					.build()
			).block();

			long id = data == null ? 0L : Snowflake.of(data.id()).asLong();

			if (data != null) {
				document.map.put("slash_command", id);
				update("slash_command", id);
			}

			return id;
		} else {
			long id = getSlashCommand();

			if (id != 0L) {
				try {
					gc.getClient().getRestClient().getApplicationService().deleteGuildApplicationCommand(gc.db.app.discordHandler.applicationId, gc.guildId.asLong(), id).block();
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				document.map.remove("slash_command");
				update(Updates.unset("slash_command"));
			}

			return id;
		}
	}

	public ChatCommandSuggestion getChatCommandSuggestion() {
		if (chatCommandSuggestion == null) {
			chatCommandSuggestion = new ChatCommandSuggestion(getName(), getName(), getName().toLowerCase(), 0);
		}

		return chatCommandSuggestion;
	}

	public void addUse() {
		update("uses", getUses() + 1);
	}
}