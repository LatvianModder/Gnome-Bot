package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.ping.UserPings;
import dev.gnomebot.app.discord.DeferrableInteractionEventWrapper;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.component.TextInput;

/**
 * @author LatvianModder
 */
public class PingsCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("pings")
			.description("Manage pings")
			.add(bool("share").description("If set to true, this will print your config in channel"))
			.run(PingsCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) {
		modal(event);
	}

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

	public static void modal(DeferrableInteractionEventWrapper<?> event) {
		UserPings pings = event.context.gc.db.userPings.query(event.context.sender).first();
		event.respondModal("pings", "Manage Pings", TextInput.paragraph("config", "Config")
				.placeholder(Utils.trim("@ mentions\n+ /" + event.context.sender.getUsername() + "/i\n\nRun `/about pings` for info on how to set up pings", 100))
				.required(false)
				.prefilled(pings == null ? "" : pings.getConfig())
		);
	}
}