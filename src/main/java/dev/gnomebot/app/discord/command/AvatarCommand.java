package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.URLRequest;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;

public class AvatarCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("avatar")
			.description("Sends avatar image in full resolution")
			.add(user("user").required())
			.add(bool("guild"))
			.run(AvatarCommand::runChatInput);

	@RegisterCommand
	public static final UserInteractionBuilder USER_COMMAND = userInteraction("Get Avatar")
			.run(AvatarCommand::runUser);

	private static void runChatInput(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledge();
		boolean guild = event.get("guild").asBoolean(true);

		User user = event.get("user").asUser().get();
		Member member = event.get("user").asOptionalMember().orElse(null);

		String avatarUrl = Utils.getAvatarUrl(user, guild ? member : null);
		boolean animated = avatarUrl.endsWith(".gif");
		byte[] data = URLRequest.of(avatarUrl + "?size=4096").toBytes().block();
		event.respond(MessageBuilder.create(user.getMention()).addFile(user.getId().asString() + (animated ? ".gif" : ".png"), data));
	}

	private static void runUser(UserInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		event.respond(Utils.getAvatarUrl(event.user, event.getMember()) + "?size=4096");
	}
}
