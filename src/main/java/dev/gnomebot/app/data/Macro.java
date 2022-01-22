package dev.gnomebot.app.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.client.model.Updates;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * @author LatvianModder
 */
public class Macro extends WrappedDocument<Macro> {
	public static final Pattern REMOVE_MD_LINKS = Pattern.compile("\\[.+?]\\((.+?)\\)");

	public final GuildCollections gc;

	public Macro(GuildCollections g, WrappedCollection<Macro> c, MapWrapper d) {
		super(c, d);
		gc = g;
	}

	public String getCommandName() {
		return document.getString("command_name");
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

	public boolean isScript() {
		return document.getBoolean("script");
	}

	public int getUses() {
		return document.getInt("uses");
	}

	public void createMessage(Consumer<String> content, Consumer<ActionRow> row) {
		List<String> lines = new ArrayList<>();

		for (String s : getContent()
				.replaceAll("role:(\\d+)", "<@&$1>")
				.replaceAll("user:(\\d+)", "<@$1>")
				.replace("mention:here", "@here")
				.replace("mention:everyone", "@everyone")
				.split("\n")) {
			if (s.startsWith("$ ")) {
				try {
					JsonElement element = Utils.GSON.fromJson(s.substring(2), JsonElement.class);
					row.accept(Utils.parseRow(element.getAsJsonArray()));
				} catch (Exception ex) {
					lines.add("Invalid component format: " + ex);
				}
			} else {
				lines.add(s);
			}
		}

		if (lines.isEmpty()) {
			lines.add("No content");
		}

		content.accept(String.join("\n", lines));
	}

	public void createMessage(MessageCreateSpec.Builder builder) {
		createMessage(s -> builder.content(REMOVE_MD_LINKS.matcher(s).replaceAll("$1")), builder::addComponent);
	}

	public void createMessage(InteractionApplicationCommandCallbackSpec.Builder builder) {
		createMessage(builder::content, builder::addComponent);
	}

	public static void addComponent(List<ActionComponent> components, JsonElement element) {
		if (element.isJsonArray()) {
			for (JsonElement e : element.getAsJsonArray()) {
				addComponent(components, e);
			}

			return;
		}

		JsonObject o = element.getAsJsonObject();

		if (o.has("macro_button")) {
			Button.Style style = o.has("style") ? Button.Style.of(o.get("style").getAsInt()) : Button.Style.SECONDARY;

			String id = "macro/" + o.get("macro_button").getAsString();
			String label = o.has("label") ? o.get("label").getAsString() : null;
			ReactionEmoji emoji = o.has("emoji") ? Utils.stringToReaction(o.get("emoji").getAsString().trim()) : null;

			switch (style) {
				case PRIMARY -> components.add(Button.primary(id, emoji, label));
				case SECONDARY -> components.add(Button.secondary(id, emoji, label));
				case SUCCESS -> components.add(Button.success(id, emoji, label));
				case DANGER -> components.add(Button.danger(id, emoji, label));
			}
		} else if (o.has("url_button")) {
			String url = o.get("url_button").getAsString();
			String label = o.has("label") ? o.get("label").getAsString() : null;
			ReactionEmoji emoji = o.has("emoji") ? Utils.stringToReaction(o.get("emoji").getAsString()) : null;
			components.add(Button.link(url, emoji, label));
		}
	}

	public long setSlashCommand(boolean b) {
		if (b) {
			String author = gc.db.app.discordHandler.getUserName(Snowflake.of(getAuthor())).orElse("Deleted User");

			ApplicationCommandData data = gc.getClient().getRestClient().getApplicationService().createGuildApplicationCommand(gc.db.app.discordHandler.applicationId, gc.guildId.asLong(), ApplicationCommandRequest.builder()
					.name(getCommandName())
					.description("Macro created by " + author)
					.build()
			).block();

			long id = data == null ? 0L : Snowflake.of(data.id()).asLong();

			if (data != null) {
				update("slash_command", id);
			}

			return id;
		} else {
			long id = document.getLong("slash_command");

			if (id != 0L) {
				gc.getClient().getRestClient().getApplicationService().deleteGuildApplicationCommand(gc.db.app.discordHandler.applicationId, gc.guildId.asLong(), id).block();
				update(Updates.unset("slash_command"));
			}

			return id;
		}
	}
}