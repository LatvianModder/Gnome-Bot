package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.ReactionHandler;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.json.JSONObject;
import discord4j.core.object.entity.Member;
import discord4j.core.object.reaction.ReactionEmoji;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class EmojifulCommand {
	@LegacyDiscordCommand(name = "emojiful", help = "Creates Emojiful .json recipes from reactions", arguments = "[*] <category name>")
	public static final CommandCallback COMMAND = (context, reader) -> {
		var cat = reader.readString().orElse(context.gc.toString());

		if (cat.equals("*")) {
			var cat1 = reader.readString().orElse(context.gc.toString());
			var out = new ByteArrayOutputStream();

			try (var zipOutputStream = new ZipOutputStream(out)) {
				zipOutputStream.putNextEntry(new ZipEntry("pack.mcmeta"));

				var mcmeta = JSONObject.of();
				var mcmetaPack = mcmeta.addObject("pack");
				mcmetaPack.put("pack_format", 6);
				mcmetaPack.put("description", "Emojiful emojis!");
				zipOutputStream.write(mcmeta.toString().getBytes(StandardCharsets.UTF_8));
				zipOutputStream.closeEntry();

				for (var custom : context.gc.getGuild().getEmojis().toIterable()) {
					zipOutputStream.putNextEntry(new ZipEntry("data/emojiful/recipes/" + custom.getName().toLowerCase() + ".json"));
					var json = JSONObject.of();
					json.put("type", "emojiful:emoji_recipe");
					json.put("category", cat1);
					json.put("url", "https://cdn.discordapp.com/emojis/" + custom.getId().asString() + ".png");
					json.put("name", custom.getName());
					zipOutputStream.write(json.toString().getBytes(StandardCharsets.UTF_8));
					zipOutputStream.closeEntry();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			try {
				context.reply(MessageBuilder.create("Done!\nCategory: " + cat1 + "\nEmojis: Everything lol").addFile("emojiful-" + cat1 + ".zip", out.toByteArray()));
			} catch (GnomeException e) {
				e.printStackTrace();
			}

			return;
		}

		var message = context.reply("React to this message with all emojis you want in the datapack, then press " + Emojis.VOTEUP.asFormat());
		message.addReaction(Emojis.VOTEUP).block();

		ReactionHandler.addListener(new ReactionHandler.Callback(context.gc, message) {
			@Override
			public boolean onReaction(Member member, ReactionEmoji emoji) throws Exception {
				if (member.getId().equals(context.sender.getId())) {
					var custom = emoji.asCustomEmoji().orElse(null);
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
				var reactions = context.channelInfo.getMessage(message.getId()).getReactions().stream().filter(r -> r.getEmoji().asCustomEmoji().isPresent()).map(r -> r.getEmoji().asCustomEmoji().get()).collect(Collectors.toSet());
				reactions.remove(Emojis.VOTEUP);

				var out = new ByteArrayOutputStream();

				try (var zipOutputStream = new ZipOutputStream(out)) {
					zipOutputStream.putNextEntry(new ZipEntry("pack.mcmeta"));

					var mcmeta = JSONObject.of();
					var mcmetaPack = mcmeta.addObject("pack");
					mcmetaPack.put("pack_format", 6);
					mcmetaPack.put("description", "Emojiful emojis!");
					zipOutputStream.write(mcmeta.toString().getBytes(StandardCharsets.UTF_8));
					zipOutputStream.closeEntry();

					for (var custom : reactions) {
						zipOutputStream.putNextEntry(new ZipEntry("data/emojiful/recipes/" + custom.getName().toLowerCase() + ".json"));
						var json = JSONObject.of();
						json.put("type", "emojiful:emoji_recipe");
						json.put("category", cat);
						json.put("url", "https://cdn.discordapp.com/emojis/" + custom.getId().asString() + ".png");
						json.put("name", custom.getName());
						zipOutputStream.write(json.toString().getBytes(StandardCharsets.UTF_8));
						zipOutputStream.closeEntry();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				try {
					context.reply(MessageBuilder.create("Done!\nCategory: " + cat + "\nEmojis: " + reactions.stream().map(Utils::reactionToString).collect(Collectors.joining(" "))).addFile("emojiful-" + cat + ".zip", out.toByteArray()));
				} catch (GnomeException e) {
					e.printStackTrace();
				}
			}
		}, 500L);
	};
}
