package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.discord.EmbedColor;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.URLRequest;
import dev.latvian.apps.webutils.math.MathUtils;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.WebhookMessageEditRequest;

import java.util.ArrayList;
import java.util.List;

public class ModpackCommand extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("modpack")
			.description("Gives you a random Minecraft modpack to play")
			.run(ModpackCommand::run);

	public static class Pack {
		public String name;
		public String url;
	}

	private static List<Pack> packs = null;

	public static List<Pack> getPacks() {
		if (packs == null) {
			packs = new ArrayList<>();

			try {
				URLRequest.of("https://raw.githubusercontent.com/LatvianModder/Gnome-Bot-Files/main/modpack_trick_list.csv").toStringList().block().forEach(s -> {
					var pack = new Pack();
					var s1 = s.trim().split(",", 2);

					if (s1.length >= 2 && !s1[0].equals("Modpack Name")) {
						pack.name = s1[0].trim();
						pack.url = s1[1].trim();

						if (!pack.name.isEmpty() && !pack.url.isEmpty()) {
							packs.add(pack);
						}
					}
				});
			} catch (Exception ex) {
			}
		}

		return packs;
	}

	public static Pack getRandomPack() {
		return getPacks().get(MathUtils.RANDOM.nextInt(getPacks().size()));
	}

	private static void run(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledge();
		var pack = getRandomPack();

		event.getResponse().editInitialResponse(WebhookMessageEditRequest.builder()
				.addEmbed(EmbedCreateSpec.builder()
						.color(EmbedColor.GRAY)
						.title("What pack should I play?")
						.description("[" + pack.name + "](" + pack.url + ")")
						.build().asRequest()
				)
				.addComponent(ActionRow.of(Button.secondary("refresh_modpack", Emojis.REFRESH, "Refresh")).getData())
				.build()
		).subscribe();
	}

	public static void refreshCallback(ComponentEventWrapper event) {
		if (event.context.message.getInteraction().isPresent() && event.context.sender.getId().equals(event.context.message.getInteraction().get().getUser().getId())) {
			var builder = MessageBuilder.create();

			var pack = ModpackCommand.getRandomPack();

			builder.addEmbed(EmbedBuilder.create()
					.color(EmbedColor.GRAY)
					.title("What pack should I play?")
					.description("[" + pack.name + "](" + pack.url + ")")
			);

			event.edit().respond(builder);
		} else {
			event.acknowledge();
		}
	}
}
