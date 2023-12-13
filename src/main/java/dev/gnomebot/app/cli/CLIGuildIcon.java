package dev.gnomebot.app.cli;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.URLRequest;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.rest.util.Image;

public class CLIGuildIcon {
	public static final CLICommand COMMAND = CLICommand.make("guild_icon")
			.description("Guild Icon")
			.noAdmin()
			.run(CLIGuildIcon::run);

	private static void run(CLIEvent event) {
		Snowflake guildId = event.reader.readString().map(Snowflake::of).orElse(Utils.NO_SNOWFLAKE);
		GuildCollections gc = Utils.NO_SNOWFLAKE.equals(guildId) ? event.gc : event.gc.db.guildOrNull(guildId);

		if (gc == null) {
			event.respond("Guild not found!");
			return;
		}

		String s = gc.getGuild().getIconUrl(Image.Format.GIF).orElse("");

		if (!s.isEmpty()) {
			try {
				URLRequest.of(s).method("HEAD").block();
			} catch (Exception ex) {
				s = gc.iconUrl.get();
			}
		}

		event.respond(s.isEmpty() ? "Guild doesn't have an avatar!" : s);
	}
}
