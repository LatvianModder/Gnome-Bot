package dev.gnomebot.app.cli;

import dev.gnomebot.app.discord.command.RootCommand;

public class CLIGuilds {
	@RootCommand
	public static final CLICommand COMMAND = CLICommand.make("guilds")
			.description("Print all guilds bot is in")
			.trustedOnly()
			.run(CLIGuilds::run);

	private static void run(CLIEvent event) {
		event.respond("WIP!");

		/*
		if (find != null) {
			info("Looking for: " + find);
		}

		Snowflake findId = find == null || find.isEmpty() ? null : Snowflake.of(find.trim());

		long guilds = 0L;
		long members = 0L;
		Table table = new Table("ID", "Name", "Members", "Gnome Messages");

		for (Guild guild : discordHandler.getSelfGuilds()) {
			long c = 0L;

			if (findId != null) {
				for (Member member : guild.getMembers().toIterable()) {
					if (member.getId().equals(findId)) {
						info("Found " + find.trim() + " in " + guild.getName());
					}

					c++;
				}
			} else {
				c = guild.getMembers().count().block();
			}

			long gm = db.guild(guild.getId()).messages.query().eq("user", discordHandler.selfId.asLong()).count();
			table.addRow(guild.getId().asString(), guild.getName(), c, gm);
			members += c;
			guilds++;
		}

		table.print();
		App.info("Gnome Bot is in " + guilds + " guilds, total " + members + " members");
		 */
	}
}
