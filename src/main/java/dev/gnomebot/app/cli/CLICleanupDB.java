package dev.gnomebot.app.cli;

import dev.gnomebot.app.discord.command.RootCommand;

public class CLICleanupDB {
	@RootCommand
	public static final CLICommand COMMAND = CLICommand.make("cleanup_db")
			.trustedOnly()
			.description("Cleans up old data")
			.run(CLICleanupDB::run);

	private static void run(CLIEvent event) {
		/*
		for (var id : event.gc.db.app.discordHandler.getSelfGuildIds()) {
			GuildCollections gc = event.gc.db.guild(id);

			for (Macro macro : gc.getMacroMap().values()) {
				List<String> newContent = new ArrayList<>();
				List<String> newExtra = new ArrayList<>();
				boolean modified = false;
				boolean firstRow = true;

				if (macro.getName().startsWith("__")) {
					modified = true;
					newExtra.add("hidden");
				}

				for (String s : macro.getContent().split("\n")) {
					if (s.startsWith("$ [") && s.endsWith("]")) {
						JsonArray array = Utils.asArray(Utils.GSON.fromJson(s.substring(2), JsonElement.class));

						if (firstRow) {
							firstRow = false;
						} else {
							newExtra.add("newrow");
						}

						for (JsonElement e : array) {
							if (e.isJsonObject()) {
								JsonObject o = e.getAsJsonObject();

								if (o.has("macro_button")) {
									int style = o.has("style") ? o.get("style").getAsInt() : 2;

									String styleString = switch (o.has("emoji") ? 2 : (style == 2 ? 0 : style)) {
										case 1 -> " blurple";
										case 2 -> " gray";
										case 3 -> " green";
										case 4 -> " red";
										default -> "";
									};

									String emojiString = o.has("emoji") ? " " + o.get("emoji").getAsString() : "";

									newExtra.add("macro " + o.get("label") + " " + o.get("macro_button").getAsString() + styleString + emojiString);
								} else if (o.has("url_button")) {
									newExtra.add("url " + o.get("label") + " " + o.get("url_button"));
								} else {
									App.warn(e);
								}
							}
						}

						modified = true;
					} else {
						newContent.add(s);
					}
				}

				if (modified) {
					if (newContent.isEmpty()) {
						newContent.add("Missingno");
					}

					App.warn("Modified macro " + macro.getName() + ":");
					App.info("- Content: \n" + String.join("\n", newContent));
					App.info("- Extra: \n" + String.join("\n", newExtra));

					macro.update(Updates.set("content", String.join("\n", newContent)));
					macro.update(Updates.set("extra", newExtra));
				}
			}
		}
		 */

		event.respond("Done");
	}
}
