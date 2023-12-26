package dev.gnomebot.app.discord;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.Vote;
import dev.gnomebot.app.discord.command.ChatCommandSuggestion;
import dev.gnomebot.app.discord.command.ChatCommandSuggestionEvent;
import dev.gnomebot.app.discord.command.ChatInputInteractionEventWrapper;
import dev.gnomebot.app.discord.command.FeedbackCommands;
import dev.gnomebot.app.discord.command.GnomeMemberInteraction;
import dev.gnomebot.app.discord.command.GnomeMessageInteraction;
import dev.gnomebot.app.discord.command.InteractionType;
import dev.gnomebot.app.discord.command.MacroCommands;
import dev.gnomebot.app.discord.command.MessageInteractionEventWrapper;
import dev.gnomebot.app.discord.command.ModmailCommand;
import dev.gnomebot.app.discord.command.ModpackCommand;
import dev.gnomebot.app.discord.command.PasteCommands;
import dev.gnomebot.app.discord.command.PingsCommands;
import dev.gnomebot.app.discord.command.ReportCommand;
import dev.gnomebot.app.discord.command.UserInteractionEventWrapper;
import dev.gnomebot.app.discord.command.WebhookCommands;
import dev.gnomebot.app.discord.command.admin.BanCommand;
import dev.gnomebot.app.discord.command.admin.KickCommand;
import dev.gnomebot.app.discord.command.admin.UnmuteCommand;
import dev.gnomebot.app.discord.command.admin.WarnCommand;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.discord.legacycommand.PollCommand;
import dev.gnomebot.app.script.event.ComponentEventJS;
import dev.gnomebot.app.script.event.ModalEventJS;
import dev.gnomebot.app.server.handler.MinecraftHandlers;
import dev.gnomebot.app.util.OngoingAction;
import dev.latvian.apps.webutils.data.Confirm;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.BanQuerySpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InteractionHandler {
	public static void chatInputInteraction(DiscordHandler handler, ChatInputInteractionEvent event) {
		GuildCollections gc = event.getInteraction().getGuildId().map(handler.app.db::guild).orElse(null);
		var command = InteractionType.CHAT_INPUT.builders.get(event.getCommandName());
		var options = event.getOptions();

		while (command != null && options.size() == 1 && options.get(0).getValue().isEmpty()) {
			command = command.getSub(options.get(0).getName());
			options = options.get(0).getOptions();
		}

		if (command != null && !command.supportsDM && gc == null) {
			event.reply("DM interactions aren't supported!").withEphemeral(true).subscribe();
			return;
		}

		try {
			var w = new ChatInputInteractionEventWrapper(gc, event, options);

			if (command != null) {
				if (App.debug) {
					App.info("Chat command '" + event.getCommandName() + "': " + w.options);
				}

				try {
					command.callback.run(w);
				} catch (GnomeException ex) {
					w.respond(ex.getMessage());
				} catch (Exception ex) {
					w.respond(ex.toString());
					ex.printStackTrace();
				}
			} else {
				var reader = new CommandReader(gc, getFullCommand(event));
				var macro = gc.getMacro(reader.readString().orElse(""));

				if (macro != null) {
					macro.addUse();
					event.reply(macro.createMessage(reader, w.context.sender.getId()).ephemeral(false).toInteractionApplicationCommandCallbackSpec()).subscribe();
				} else {
					App.error("Weird interaction data from " + event.getInteraction().getUser().getUsername() + ": " + event.getInteraction().getData());
					event.reply("Command not found!").withEphemeral(true).subscribe();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static String getFullCommand(ChatInputInteractionEvent event) {
		var sb = new StringBuilder(event.getCommandName());

		for (var o : event.getOptions()) {
			sb.append(' ').append(o.getName());

			for (var o2 : o.getOptions()) {
				sb.append(' ').append(o2.getName());
			}
		}

		return sb.toString();
	}

	public static void userInteraction(DiscordHandler handler, UserInteractionEvent event) {
		GuildCollections gc = event.getInteraction().getGuildId().map(handler.app.db::guild).orElse(null);
		var command = InteractionType.USER.builders.get(event.getCommandName());

		if (command != null && !command.supportsDM && gc == null) {
			event.reply("DM interactions aren't supported!").withEphemeral(true).subscribe();
			return;
		}

		try {
			var w = new UserInteractionEventWrapper(gc, event);

			if (command != null) {
				try {
					command.callback.run(w);
				} catch (GnomeException ex) {
					w.respond(ex.getMessage());
				} catch (Exception ex) {
					w.respond(ex.toString());
					ex.printStackTrace();
				}
			} else {
				App.error("Weird interaction data from " + event.getInteraction().getUser().getUsername() + ": " + event.getInteraction().getData());
				event.reply("Command not found!").withEphemeral(true).subscribe();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void messageInteraction(DiscordHandler handler, MessageInteractionEvent event) {
		GuildCollections gc = event.getInteraction().getGuildId().map(handler.app.db::guild).orElse(null);
		var command = InteractionType.MESSAGE.builders.get(event.getCommandName());

		if (command != null && !command.supportsDM && gc == null) {
			event.reply("DM interactions aren't supported!").withEphemeral(true).subscribe();
			return;
		}

		try {
			var w = new MessageInteractionEventWrapper(gc, event);

			if (command != null) {
				try {
					command.callback.run(w);
				} catch (GnomeException ex) {
					w.respond(ex.getMessage());
				} catch (Exception ex) {
					w.respond(ex.toString());
					ex.printStackTrace();
				}
			} else {
				App.error("Weird interaction data from " + event.getInteraction().getUser().getUsername() + ": " + event.getInteraction().getData());
				event.reply("Command not found!").withEphemeral(true).subscribe();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void button(DiscordHandler handler, ButtonInteractionEvent event) {
		GuildCollections gc = event.getInteraction().getGuildId().map(handler.app.db::guild).orElse(null);

		if (gc != null) {
			Member member = event.getInteraction().getMember().orElse(null);

			if (member != null) {
				String customId = event.getCustomId();
				ComponentEventWrapper eventWrapper = new ComponentEventWrapper(gc, event, customId);

				if (gc.discordJS.onButton.hasListeners() && gc.discordJS.onButton.post(customId, new ComponentEventJS(customId, gc.getWrappedGuild().getUser(member.getId().asString()), eventWrapper))) {
					return;
				}

				try {
					try {
						button(eventWrapper);
					} catch (GnomeException ex) {
						App.error("Error in " + eventWrapper + ": " + ex.getMessage());
						eventWrapper.respond(ex.getMessage());
					} catch (Exception ex) {
						App.error("Error in " + eventWrapper + ": " + ex);
						eventWrapper.respond("Error: " + ex);
					}
				} catch (Exception ex) {
				}
			}
		}
	}

	public static void selectMenu(DiscordHandler handler, SelectMenuInteractionEvent event) {
		GuildCollections gc = event.getInteraction().getGuildId().map(handler.app.db::guild).orElse(null);

		if (gc != null) {
			Member member = event.getInteraction().getMember().orElse(null);

			if (member != null) {
				String customId = event.getCustomId();

				ComponentEventWrapper eventWrapper = new ComponentEventWrapper(gc, event, customId);

				if (gc.discordJS.onSelectMenu.hasListeners() && gc.discordJS.onSelectMenu.post(customId, new ComponentEventJS(customId, gc.getWrappedGuild().getUser(member.getId().asString()), eventWrapper))) {
					return;
				}

				try {
					try {
						selectMenu(eventWrapper, event.getValues());
					} catch (GnomeException ex) {
						eventWrapper.respond(ex.getMessage());
					} catch (Exception ex) {
						eventWrapper.respond("Error: " + ex);
					}
				} catch (Exception ex) {
				}
			}
		}
	}

	public static void modalSubmitInteraction(DiscordHandler handler, ModalSubmitInteractionEvent event) {
		GuildCollections gc = event.getInteraction().getGuildId().map(handler.app.db::guild).orElse(null);

		if (gc != null) {
			Member member = event.getInteraction().getMember().orElse(null);

			if (member != null) {
				String customId = event.getCustomId();
				ModalEventWrapper eventWrapper = new ModalEventWrapper(gc, event, customId);

				if (gc.discordJS.onModal.hasListeners() && gc.discordJS.onModal.post(customId, new ModalEventJS(customId, gc.getWrappedGuild().getUser(member.getId().asString()), eventWrapper))) {
					return;
				}

				try {
					try {
						modalSubmit(eventWrapper);
					} catch (GnomeException ex) {
						App.error("Error in " + eventWrapper + ": " + ex.getMessage());
						eventWrapper.respond(ex.getMessage());
					} catch (Exception ex) {
						App.error("Error in " + eventWrapper + ": " + ex);
						eventWrapper.respond("Error: " + ex);
					}
				} catch (Exception ex) {
				}
			}
		}
	}

	public static void chatInputAutoComplete(DiscordHandler handler, ChatInputAutoCompleteEvent event) {
		GuildCollections gc = event.getInteraction().getGuildId().map(handler.app.db::guild).orElse(null);

		if (gc == null) {
			return;
		}

		var command = InteractionType.CHAT_INPUT.builders.get(event.getCommandName());
		var options = event.getOptions();

		while (command != null && options.size() == 1 && options.get(0).getValue().isEmpty()) {
			command = command.getSub(options.get(0).getName());
			options = options.get(0).getOptions();
		}

		try {
			if (command != null) {
				ChatCommandSuggestionEvent eventWrapper = new ChatCommandSuggestionEvent(gc, event, options);

				if (eventWrapper.focused != null) {
					// App.info(eventWrapper.focused.name + " " + command + " " + optionsToString(new StringBuilder(), options));
					var sub = command.getSub(eventWrapper.focused.name);

					if (sub != null && sub.suggestions != null) {
						sub.suggestions.getSuggestions(eventWrapper);

						if (eventWrapper.suggestions.isEmpty()) {
							event.respondWithSuggestions(Collections.emptyList()).subscribe();
						} else {
							eventWrapper.suggestions.sort(ChatCommandSuggestion::compareTo);
							String search = eventWrapper.transformSearch.apply(eventWrapper.focused.asString());

							List<ApplicationCommandOptionChoiceData> list = new ArrayList<>();

							for (ChatCommandSuggestion data : eventWrapper.suggestions) {
								if (list.size() == 25) {
									break;
								} else if (search.isEmpty() || data.match().startsWith(search)) {
									list.add(data.build());
								}
							}

							for (ChatCommandSuggestion data : eventWrapper.suggestions) {
								if (list.size() == 25) {
									break;
								} else if (!search.isEmpty() && !data.match().startsWith(search) && data.match().contains(search)) {
									list.add(data.build());
								}
							}

							event.respondWithSuggestions(list).subscribe();
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void button(ComponentEventWrapper event) throws Exception {
		switch (event.path[0]) {
			case "none" -> event.acknowledge();
			case "delete" -> deleteMessage(event, Snowflake.of(event.path[1]));
			case "callback" -> callback(event, event.path[1]);
			case "stop" -> stopOngoingAction(event, event.path[1]);
			case "message-action" -> GnomeMessageInteraction.callback(event, Snowflake.of(event.path[1]), event.path[2]);
			case "member-action" -> GnomeMemberInteraction.callback(event, Snowflake.of(event.path[1]), event.path[2]);
			case "unmute" -> UnmuteCommand.unmuteButtonCallback(event, Snowflake.of(event.path[1]));
			case "macro" -> MacroCommands.macroButtonCallback(event, event.path[1], null);
			case "edit-macro", "edit_macro" -> MacroCommands.macroButtonCallback(event, event.path[1], Snowflake.of(event.path[2]));
			case "feedback" -> FeedbackCommands.feedbackButtonCallback(event, Integer.parseInt(event.path[1]), event.path[2].equals("upvote") ? Vote.UP : event.path[2].equals("downvote") ? Vote.DOWN : Vote.NONE);
			case "warn" -> WarnCommand.warnButtonCallback(event, Snowflake.of(event.path[1]), event.path[2], Confirm.of(event.path, 3));
			case "kick" -> KickCommand.kickButtonCallback(event, Snowflake.of(event.path[1]), event.path[2], Confirm.of(event.path, 3));
			case "ban" -> BanCommand.banButtonCallback(event, Snowflake.of(event.path[1]), event.path[2], Confirm.of(event.path, 3));
			case "refresh_modpack" -> ModpackCommand.refreshCallback(event);
			case "pings" -> PingsCommands.edit(event);
			case "pings-help" -> PingsCommands.help(event);
			case "regex-help" -> PingsCommands.regexHelp(event);
			case "verify-minecraft" -> MinecraftHandlers.verifyCallback(event, Snowflake.of(event.path[1]));
			default -> {
				App.info(event.context.sender.getTag() + " clicked " + event.context.gc + "/" + Arrays.asList(event.path));
				throw new GnomeException("Unknown button ID: " + Arrays.asList(event.path));
			}
		}
	}

	private static void selectMenu(ComponentEventWrapper event, List<String> values) throws Exception {
		switch (event.path[0]) {
			case "none" -> event.acknowledge();
			case "delete" -> deleteMessage(event, Snowflake.of(event.path[1]));
			case "poll" -> PollCommand.pollMenuCallback(event, Integer.parseInt(event.path[1]), values.get(0));
			case "punish" -> punishMenu(event, Snowflake.of(event.path[1]), ComponentEventWrapper.decode(event.path[2]), values.isEmpty() ? "" : values.get(0));
			case "report" -> ReportHandler.report(event, Snowflake.of(event.path[1]), Snowflake.of(event.path[2]), values.get(0));
			default -> {
				App.info(event.context.sender.getTag() + " selected " + event.context.gc + "/" + Arrays.asList(event.path) + "/" + values);
				throw new GnomeException("Unknown select menu ID: " + Arrays.asList(event.path) + "/" + values);
			}
		}
	}

	private static void modalSubmit(ModalEventWrapper event) {
		switch (event.path[0]) {
			case "none" -> event.acknowledge();
			case "delete" -> deleteMessage(event, Snowflake.of(event.path[1]));
			case "modal-test" -> event.respond("Modal: " + event);
			case "modmail" -> ModmailCommand.modmailCallback(event);
			case "report" -> ReportCommand.reportCallback(event, Snowflake.of(event.path[1]), Snowflake.of(event.path[2]));
			case "feedback" -> FeedbackCommands.submitCallback(event);
			case "ping-test" -> PingsCommands.testCallback(event, Snowflake.of(event.path[1]));
			case "add-macro" -> MacroCommands.addMacroCallback(event, event.path[1]);
			case "edit-macro" -> MacroCommands.editMacroCallback(event, event.path[1]);
			case "pings" -> PingsCommands.editCallback(event);
			case "webhook" -> WebhookCommands.executeCallback(event, Snowflake.of(event.path[1]), Snowflake.of(event.path[2]));
			case "create-paste" -> PasteCommands.createCallback(event);
			default -> {
				App.warn(event.context.sender.getTag() + " submitted unknown modal " + event.context.gc + "/" + event);
				throw new GnomeException("Unknown modal ID: " + event);
			}
		}
	}

	// Actions //

	private static void deleteMessage(DeferrableInteractionEventWrapper<?> event, Snowflake owner) {
		if (event.context.isAdmin() || event.context.sender.getId().asLong() == owner.asLong()) {
			event.getResponse().deleteInitialResponse().block();
		} else if (event.requiresTextResponse()) {
			event.respond("You can't delete this message!");
		} else {
			event.acknowledge();
		}
	}

	private static void callback(ComponentEventWrapper event, String id) {
		ComponentCallback callback = ComponentCallback.MAP.get(id);

		if (callback == null) {
			event.respond("Callback expired!");
		} else {
			var delete = callback.run(event);

			if (delete == ComponentCallback.SELF) {
				ComponentCallback.MAP.remove(id);
			} else if (delete != ComponentCallback.NONE) {
				for (var i : delete) {
					if (i.startsWith("callback/")) {
						i = i.substring(9);
					}

					ComponentCallback.MAP.remove(i);
				}
			}

			event.acknowledge();
		}
	}

	private static void punishMenu(ComponentEventWrapper event, Snowflake userId, String reason, String type) {
		event.context.checkSenderAdmin();

		switch (type) {
			case "kick" -> {
				event.context.gc.getGuild().kick(userId, reason).subscribe();
				event.respond("Kicked <@" + userId.asString() + ">");
			}
			case "ban" -> {
				event.context.gc.getGuild().ban(userId, BanQuerySpec.builder().deleteMessageDays(1).reason(reason).build()).subscribe();
				event.respond("Banned <@" + userId.asString() + ">");
			}
			case "unmute" -> {
				event.context.gc.unmute(userId, 0L, "");
				event.respond("Unmuted <@" + userId.asString() + ">");
			}
			default -> event.respond("This action cannot be undone!");
		}
	}

	private static void stopOngoingAction(ComponentEventWrapper event, String id) {
		event.acknowledge();
		OngoingAction.stop(id);
	}
}