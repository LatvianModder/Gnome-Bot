package dev.gnomebot.app.discord.command;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.App;
import dev.gnomebot.app.data.ping.PingData;
import dev.gnomebot.app.data.ping.UserPings;
import dev.gnomebot.app.discord.DeferrableInteractionEventWrapper;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.ModalEventWrapper;
import dev.gnomebot.app.discord.legacycommand.CommandContext;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.FormattingUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.User;

import java.util.ArrayList;
import java.util.List;

public class PingsCommands extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("pings")
			.description("Manage pings")
			.add(sub("edit")
					.description("Edit pings")
					.run(PingsCommands::edit)
			)
			.add(sub("share")
					.description("Shares your pings config in channel")
					.run(PingsCommands::share)
			)
			.add(sub("help")
					.description("Prints info on how to use pings")
					.run(PingsCommands::help)
			)
			.add(sub("regex_help")
					.description("Prints info on how to use RegEx")
					.run(PingsCommands::regexHelp)
			)
			.add(sub("test")
					.description("Test your pings in bulk")
					.add(string("test-text"))
					.add(user("from"))
					.run(PingsCommands::test)
			);

	public static final String HELP = """
			With `/pings` you can configure specific words that will mention you in either DM or a private channel with webhook.
						
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
			`@ group_name` - Create a new group where your pings will go, required before RegEx. Use `@ dm` to receive a DM, otherwise set up a webhook with `/webhook`
			`-/+ /RegEx/flags` - Deny/Allow RegEx. If message matches, rest will be skipped. You may add multiple under each group
			`# Comment line` - You can write any comment you want here
						
			Remember, order of operations is important!
			Any `+`/`-` operations done before `@` will apply to all groups, but each group can override them.
			Group name has to match webhook destination. You can either set them up with `/webhook` command or use `@ dm` to send a DM if possible, but it will lose some formatting and you may have to message Gnome first to activate DMs.
			By default, no guild, channel or user is denied, bot pings are allowed and pings from yourself aren't allowed.
			Flags in RegEx are optional. You can add `i` to make it case insensitive, for example, `+ /abc/i` would match "AbC".
						
			A very basic example that will ping you when someone says your name, case insensitive:
			```
			@ dm
			+ /{USER}/i```
			Here's similar example, it will match "HeLLo" but ignore "HELLO" and will also ignore pings from bots:
			```
			- bots
			@ dm
			- /HELLO/
			+ /hello/i```
			""";

	public static final String HELP_REGEX = """
			***Noone can help you with regex.***
						
			Just kidding, here's [tool to test it](<https://regex101.com/>) and some basic examples:
						
			**Plain string**
			`Word` - Contains "Word" - "Hello this has Word" or "Has InBetweenWordHere" but not "Wo rd"
						
			**Anchors**
			`^Word` - Starts with this - "WordAbc" but not "AbcWord"
			`Word$` - Ends with this - "AbcWord" but not "WordAbc"
						
			**Groups**
			`[abc]` - Matches any of these characters - "a" or "b" or "c"
			`[a-z]` - Character range, matches any of a to z - "a", "b", ..., "y", "z"
			`[a-z0-9_@]` - Multiple character matches in one, ranges and plain characters can be mixed. Escape with \
			`(RegEx)` - Group of RegEx/strings
			`(String1|String2)` - Matches either of these strings - "String1" or "String2"
						
			**Quantifiers**
			`abc{2}` - Matches exact number of times - "abcc" but not "ab", "abc" or "abccc"
			`abc{2,}` - Matches x or more times - "abcc", "abccc", "abcccc"... but not "ab" or "abc"
			`abc{2,4}` - Matches between x and y times - "abc", "abcc", "abccc", "abcccc", but not "ab" or "abccccc"
			`abc+` - Matches one or more times (equal to `{1,}`) - "abc", "abcc", "abcccc"... but not "ab"
			`abc*` - Matches zero or more times (equal to `{0,}`) - "ab", "abc", "abcc", "abcccc"...
			`abc?` - Matches zero or one times (equal to `{0,1}`) - "ab" or "abc"
			Unless you specify a group, quantifiers are only applied to last character. You can do this:
			`[a-d]+` - Matches "abacbadcba"
			`Gnome(tte)? Cool` - Matches "Gnome Cool" or "Gnomette Cool"
						
			**Flags**
			`+ /RegEx/i` - Case insensitive, matches "Abc", "ABC" and "abc"
			`+ /^RegEx/m` - Multiline, will match Anchor expressions in each new line rather than whole string
			`+ /RegEx/mi` - You can combine flags, order doesn't matter
						
			**This guide is still incomplete!**
			""";

	public static void edit(DeferrableInteractionEventWrapper<?> event) {
		UserPings pings = event.context.gc.db.userPings.query(event.context.sender).first();
		event.respondModal("pings", "Manage Pings", TextInput.paragraph("config", "Config")
				.placeholder(FormattingUtils.trim("@ mentions\n+ /" + event.context.sender.getUsername() + "/i\n\nRun `/about pings` for info on how to set up pings", 100))
				.required(false)
				.prefilled(pings == null ? "" : pings.getConfig())
		);
	}

	public static void editCallback(ModalEventWrapper event) {
		if (!event.context.isAdmin()) {
			// event.respond("WIP! For now this command is only available to admins!");
			// return;
		}

		String config = event.get("config").asString().trim();

		try {
			event.context.gc.db.userPings.query(event.context.sender).upsert(List.of(Updates.set("config", config)));

			if (config.isEmpty()) {
				event.context.gc.db.app.pingHandler.update();
				event.respond("Pings cleared!");
				return;
			}

			var pings = UserPings.compile(event.context.gc.db, event.context.sender.getId(), config);

			App.success(event.context.sender.getUsername() + " updated their pings:");

			for (var ping : pings) {
				App.info("- name: " + ping.name);
				App.info("  config: " + ping.buildConfig());
				App.info("  pings:");

				for (var p : ping.pings) {
					App.info("  " + p);
				}
			}

			event.context.gc.db.app.pingHandler.update();
			event.respond("Pings set!");
		} catch (GnomeException ex) {
			event.respond(MessageBuilder.create("Syntax error on line " + ex.position + ":\n" + ex.getMessage()).addComponentRow(
					Button.primary("pings", "Edit"),
					Button.secondary("pings_help", "Help"),
					Button.secondary("regex_help", "RegEx Guide")
			));
		} catch (Exception ex) {
			event.respond(MessageBuilder.create("Syntax error:\n" + ex.getMessage()).addComponentRow(
					Button.primary("pings", "Edit"),
					Button.secondary("pings_help", "Help"),
					Button.secondary("regex_help", "RegEx Guide")
			));
		}
	}

	private static void share(ChatInputInteractionEventWrapper event) {
		UserPings pings = event.context.gc.db.userPings.query(event.context.sender).first();

		if (pings == null) {
			event.respond("You don't have any pings set up.");
		} else {
			event.acknowledge();
			event.respond(MessageBuilder.create("`/pings` config:\n```\n" + pings.getConfig().replace("```", "\\```") + "```").ephemeral(false));
		}
	}

	public static void help(DeferrableInteractionEventWrapper<?> event) {
		event.respond(HELP.replace("{USER}", event.context.sender.getUsername().toLowerCase()));
	}

	public static void regexHelp(DeferrableInteractionEventWrapper<?> event) {
		event.respond(HELP_REGEX);
	}

	public static void test(ChatInputInteractionEventWrapper event) {
		var s = event.get("test-text").asString().trim();
		var from = event.get("from").asUser().map(User::getId).orElse(Utils.NO_SNOWFLAKE);

		if (s.isEmpty()) {
			event.respondModal("ping-test/" + from.asString(), "Test Pings", TextInput.paragraph("text", "Text to Test", 1, 2000).placeholder("Write your test text here. Each line will be treated as its own check."));
		} else {
			event.acknowledgeEphemeral();
			event.respond(testResponse(event.context, from, s.split("\n")));
		}
	}

	private static String testResponse(CommandContext ctx, Snowflake from, String[] input) {
		var lines = new ArrayList<String>();

		loop:
		for (var content : input) {
			var match = Emojis.stripEmojis(content);

			if (match.isEmpty()) {
				continue;
			}

			PingData pingData;

			if (from.asLong() == 0L) {
				var user = ctx.gc.db.app.discordHandler.getUser(ctx.gc.db.app.discordHandler.selfId);

				Snowflake userId = user.getId();
				String username = user.getGlobalName().orElse(user.getUsername());
				String avatar = user.getAvatarUrl();
				boolean bot = false;
				pingData = new PingData(ctx.gc, ctx.channelInfo, user, userId, username, avatar, bot, match, content, "");
			} else {
				var user = ctx.gc.db.app.discordHandler.getUser(from);

				if (user == null) {
					throw new GnomeException("User not found!");
				}

				Snowflake userId = user.getId();
				String username = user.getGlobalName().orElse(user.getUsername());
				String avatar = user.getAvatarUrl();
				boolean bot = user.isBot();
				pingData = new PingData(ctx.gc, ctx.channelInfo, user, userId, username, avatar, bot, match, content, "");
			}

			for (var data : ctx.gc.db.app.pingHandler.getPings()) {
				if (data.user().equals(ctx.sender.getId())) {
					var ping = data.test(pingData);

					if (ping != null) {
						lines.add("1. " + Emojis.YES.asFormat() + " `" + match + "`: `" + ping.pattern() + "`");
						continue loop;
					}
				}
			}

			lines.add("1. " + Emojis.NO.asFormat() + " `" + match + "`");
		}

		return String.join("\n", lines);
	}

	public static void testCallback(ModalEventWrapper event, Snowflake from) {
		event.respond(testResponse(event.context, from, event.get("text").asString().split("\n")));
	}
}