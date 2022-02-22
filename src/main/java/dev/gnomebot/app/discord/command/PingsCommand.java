package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.ping.UserPings;
import dev.gnomebot.app.discord.DeferrableInteractionEventWrapper;
import discord4j.core.object.component.TextInput;

/**
 * @author LatvianModder
 */
public class PingsCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("pings")
			.description("Manage pings")
			.run(PingsCommand::run);

	private static void run(ApplicationCommandEventWrapper event) {
		modal(event);
	}

	public static final String HELP = """
			This config may look strange, but it's actually very simple:
			```
			- guild 123
			- channel 456
			- user 789
			- bots
						
			# Comment line
			@ mentions
			+ /WantThisToPingMe/i
						
			@ mygroup2
			+ user 789
			+ self
			- /DontWantThisToPingMe/
			+ /WantThisToPingMe/i```
			So what the heck are all of these symbols?
						
			`-/+ guild ID` - Deny/Allow pings from this guild
			`-/+ channel ID` - Deny/Allow pings from this channel
			`-/+ user ID` - Deny/Allow pings from this user
			`-/+ bots` - Deny/Allow pings from bot users
			`-/+ self` - Deny/Allow pings from yourself
			`@ group_name` - Create a new group. Required before RegEx. Group name is destination of where your ping message will go
			`-/+ /RegEx/flags` - Deny/Allow RegEx. If message matches, rest will be skipped. You may add multiple under each group
			`# Comment line` - You can write any comment you want here
						
			Remember, order of operations is important!
			Any `+`/`-` operations done before `@` will apply to all groups, but each group can override them.
			Group name has to match webhook destination. You can either set them up with `/webhook` command or use `@ dm` to send a DM if possible, but it will lose some formatting and you may have to message Gnome first to activate DMs.
			By default, no guild, channel or user is denied, bot pings are allowed and pings from yourself aren't allowed.
			Flags in RegEx are optional. You can add `i` to make it case insensitive, for example, `+ /abc/i` would match "AbC".
						
			A very basic example that will ping you when someone says your name, case insensitive:
			```
			@ mentions
			+ /{USER}/i```
			Here's similar example, it will match "HeLLo" but ignore "HELLO" and will also ignore pings from bots:
			```
			- bots
			@ mentions
			- /HELLO/
			+ /hello/i```
			""";

	public static void modal(DeferrableInteractionEventWrapper<?> event) {
		UserPings pings = event.context.gc.db.userPings.query(event.context.sender).first();
		event.respondModal("pings", "Manage Pings", TextInput.paragraph("config", "Config")
				.placeholder("Run `/about pings` for info on how to set up pings")
				.required(false)
				.prefilled(pings == null ? "" : pings.getConfig())
		);
	}
}