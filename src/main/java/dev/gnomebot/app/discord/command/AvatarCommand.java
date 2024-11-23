package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.URLRequest;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.entity.Member;

public class AvatarCommand extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("avatar")
			.supportsDM()
			.description("Sends avatar image in full resolution")
			.add(user("user").required())
			.add(bool("guild"))
			.run(AvatarCommand::runChatInput);

	private static void runChatInput(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledge();
		var guild = event.get("guild").asBoolean(true);

		var user = event.get("user").asUser().get();
		var member = event.get("user").asOptionalMember().orElse(null);

		var avatarUrl = Utils.getAvatarURL(user, guild ? member : null);
		var animated = avatarUrl.endsWith(".gif");
		var data = URLRequest.of(avatarUrl + "?size=4096").toBytes().block();
		event.respond(MessageBuilder.create(user.getMention()).addFile(user.getId().asString() + (animated ? ".gif" : ".png"), data));
	}

	public static void memberInteraction(Member member, ComponentEventWrapper event) {
		event.respond(Utils.getAvatarURL(member, member) + "?size=4096");
	}
}
