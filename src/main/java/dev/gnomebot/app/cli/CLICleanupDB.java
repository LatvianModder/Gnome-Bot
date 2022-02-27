package dev.gnomebot.app.cli;

import dev.gnomebot.app.App;
import dev.gnomebot.app.discord.command.RootCommand;
import dev.gnomebot.app.util.Utils;
import discord4j.discordjson.json.ChannelData;
import reactor.core.publisher.Flux;

public class CLICleanupDB {
	@RootCommand
	public static final CLICommand COMMAND = CLICommand.make("cleanup_db")
			.ownerOnly()
			.description("Cleans up old data")
			.run(CLICleanupDB::run);

	private static void run(CLIEvent event) {
		Utils.GET_DM_CHANNELS.newRequest()
				.exchange(event.gc.db.app.discordHandler.client.getCoreResources().getRouter())
				.bodyToMono(ChannelData[].class)
				.flatMapMany(Flux::fromArray)
				.toIterable()
				.forEach(channelData -> App.info(channelData.id()));

		/*
		for (var id : event.gc.db.app.discordHandler.getSelfGuildIds()) {
			GuildCollections gc = event.gc.db.guild(id);
		 */

		event.respond("Done");
	}
}
