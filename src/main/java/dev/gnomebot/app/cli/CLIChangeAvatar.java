package dev.gnomebot.app.cli;

import dev.gnomebot.app.App;
import dev.gnomebot.app.discord.command.RegisterCommand;
import dev.gnomebot.app.util.GuildProfileEditRequest;
import dev.gnomebot.app.util.URLRequest;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.FormattingUtils;
import discord4j.discordjson.possible.Possible;

import java.util.Base64;

public class CLIChangeAvatar {
	@RegisterCommand
	public static final CLICommand COMMAND = CLICommand.make("change_avatar")
			.description("Kicks new accounts")
			.run(CLIChangeAvatar::run);

	private static void run(CLIEvent event) throws Exception {
		String url = event.reader.readRemainingString().orElse("");
		GuildProfileEditRequest request = new GuildProfileEditRequest();

		if (url.isEmpty()) {
			event.respond("Avatar reset!");
			request.avatar = null;
		} else {
			byte[] image = URLRequest.of(url).toBytes().block();
			boolean gif = url.endsWith(".gif");
			String s = (gif ? "data:image/gif;base64," : "data:image/png;base64,") + Base64.getEncoder().encodeToString(image);
			App.info(FormattingUtils.trim(s, 1000));
			request.avatar = Possible.of(s);
		}

		Utils.GUILD_PROFILE_ROUTE.newRequest(event.gc.guildId.asLong())
				.body(request)
				.exchange(event.gc.db.app.discordHandler.client.getCoreResources().getRouter())
				.skipBody()
				.block();

		event.respond("Done!");
	}
}
