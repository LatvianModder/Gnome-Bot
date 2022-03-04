package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.util.CharMap;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.GuildMemberEditSpec;

import java.util.Random;

/**
 * @author LatvianModder
 */
public class ForcePingableNameCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("force_pingable_name")
			.description("Forces username to be mentionable, transforms unicode characters into basic latin")
			.add(user("member").required())
			.run(ForcePingableNameCommand::run);

	public static String randomUserName(Snowflake id, int l) {
		Random r = new Random(id.asLong());
		char[] c = new char[l / 2 * 2];

		for (int i = 0; i < l / 2; i++) {
			RandomGibberishCommand.putChars(c, i * 2, r);
		}

		return new String(c);
	}

	public static String makePingable(String name, Snowflake id) {
		return CharMap.makePingable(name, randomUserName(id, 6));
	}

	private static void run(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();
		Member member = event.get("member").asMember().get();
		String name = member.getUsername();
		String newName = makePingable(name, member.getId());

		try {
			if (!newName.equals(name)) {
				member.edit(GuildMemberEditSpec.builder().nicknameOrNull(newName).build()).block();
				event.respond("Modified " + member.getMention() + " name to: `" + newName + "`");
			} else {
				event.respond("No need to modify, user already can be mentioned");
			}
		} catch (Exception ex) {
			event.respond("No permission to update name!");
		}
	}
}
