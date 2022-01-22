package dev.gnomebot.app.discord.legacycommand;

import com.google.gson.JsonObject;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.ReactionHandler;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author LatvianModder
 */
public class EmojifulCommand {
	@LegacyDiscordCommand(name = "emojiful", help = "Creates Emojiful .json recipes from reactions", arguments = "[*] <category name>")
	public static final CommandCallback COMMAND = (context, reader) -> {
		String cat = reader.readString().orElse(context.gc.toString());

		if (cat.equals("*")) {
			String cat1 = reader.readString().orElse(context.gc.toString());
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			try (ZipOutputStream zipOutputStream = new ZipOutputStream(out)) {
				zipOutputStream.putNextEntry(new ZipEntry("pack.mcmeta"));

				JsonObject mcmeta = new JsonObject();
				JsonObject mcmetaPack = new JsonObject();
				mcmetaPack.addProperty("pack_format", 6);
				mcmetaPack.addProperty("description", "Emojiful emojis!");
				mcmeta.add("pack", mcmetaPack);
				zipOutputStream.write(mcmeta.toString().getBytes(StandardCharsets.UTF_8));
				zipOutputStream.closeEntry();

				for (GuildEmoji custom : context.gc.getGuild().getEmojis().toIterable()) {
					zipOutputStream.putNextEntry(new ZipEntry("data/emojiful/recipes/" + custom.getName().toLowerCase() + ".json"));
					JsonObject json = new JsonObject();
					json.addProperty("type", "emojiful:emoji_recipe");
					json.addProperty("category", cat1);
					json.addProperty("url", "https://cdn.discordapp.com/emojis/" + custom.getId().asString() + ".png");
					json.addProperty("name", custom.getName());
					zipOutputStream.write(json.toString().getBytes(StandardCharsets.UTF_8));
					zipOutputStream.closeEntry();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			context.reply("Done!\nCategory: " + cat1 + "\nEmojis: Everything lol");

			try {
				context.replyFile("emojiful-" + cat1, "zip", out.toByteArray(), false);
			} catch (DiscordCommandException e) {
				e.printStackTrace();
			}
			return;
		}

		Message message = context.reply("React to this message with all emojis you want in the datapack, then press " + Emojis.VOTEUP.asFormat());
		message.addReaction(Emojis.VOTEUP).block();

		ReactionHandler.addListener(new ReactionHandler.Callback(context.gc, message) {
			@Override
			public boolean onReaction(Member member, ReactionEmoji emoji) throws Exception {
				if (member.getId().equals(context.sender.getId())) {
					ReactionEmoji.Custom custom = emoji.asCustomEmoji().orElse(null);
					if (custom != null) {
						if (custom.equals(Emojis.VOTEUP)) {
							ReactionHandler.removeListener(message.getId());
						} else {
							return false;
						}
					}
				}

				return true;
			}

			@Override
			public void onRemoved(boolean shutdown) throws Exception {
				Set<ReactionEmoji.Custom> reactions = context.channelInfo.getMessage(message.getId()).getReactions().stream().filter(r -> r.getEmoji().asCustomEmoji().isPresent()).map(r -> r.getEmoji().asCustomEmoji().get()).collect(Collectors.toSet());
				reactions.remove(Emojis.VOTEUP);

				ByteArrayOutputStream out = new ByteArrayOutputStream();

				try (ZipOutputStream zipOutputStream = new ZipOutputStream(out)) {
					zipOutputStream.putNextEntry(new ZipEntry("pack.mcmeta"));

					JsonObject mcmeta = new JsonObject();
					JsonObject mcmetaPack = new JsonObject();
					mcmetaPack.addProperty("pack_format", 6);
					mcmetaPack.addProperty("description", "Emojiful emojis!");
					mcmeta.add("pack", mcmetaPack);
					zipOutputStream.write(mcmeta.toString().getBytes(StandardCharsets.UTF_8));
					zipOutputStream.closeEntry();

					for (ReactionEmoji.Custom custom : reactions) {
						zipOutputStream.putNextEntry(new ZipEntry("data/emojiful/recipes/" + custom.getName().toLowerCase() + ".json"));
						JsonObject json = new JsonObject();
						json.addProperty("type", "emojiful:emoji_recipe");
						json.addProperty("category", cat);
						json.addProperty("url", "https://cdn.discordapp.com/emojis/" + custom.getId().asString() + ".png");
						json.addProperty("name", custom.getName());
						zipOutputStream.write(json.toString().getBytes(StandardCharsets.UTF_8));
						zipOutputStream.closeEntry();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				context.reply("Done!\nCategory: " + cat + "\nEmojis: " + reactions.stream().map(Utils::reactionToString).collect(Collectors.joining(" ")));

				try {
					context.replyFile("emojiful-" + cat, "zip", out.toByteArray(), false);
				} catch (DiscordCommandException e) {
					e.printStackTrace();
				}
			}
		}, 500L);
	};
}
