package dev.gnomebot.app.discord;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.Vote;
import dev.gnomebot.app.data.complex.ComplexMessageRenderContext;
import dev.gnomebot.app.discord.command.ChatCommandSuggestion;
import dev.gnomebot.app.discord.command.ChatCommandSuggestionEvent;
import dev.gnomebot.app.discord.command.ChatInputInteractionEventWrapper;
import dev.gnomebot.app.discord.command.FeedbackCommand;
import dev.gnomebot.app.discord.command.GnomeMemberInteraction;
import dev.gnomebot.app.discord.command.GnomeMessageInteraction;
import dev.gnomebot.app.discord.command.InteractionType;
import dev.gnomebot.app.discord.command.MacroCommands;
import dev.gnomebot.app.discord.command.MessageInteractionEventWrapper;
import dev.gnomebot.app.discord.command.ModmailCommand;
import dev.gnomebot.app.discord.command.ModpackCommand;
import dev.gnomebot.app.discord.command.PasteCommands;
import dev.gnomebot.app.discord.command.PingsCommands;
import dev.gnomebot.app.discord.command.PollCommand;
import dev.gnomebot.app.discord.command.ReportCommand;
import dev.gnomebot.app.discord.command.UserInteractionEventWrapper;
import dev.gnomebot.app.discord.command.WebhookCommands;
import dev.gnomebot.app.discord.command.admin.BanCommand;
import dev.gnomebot.app.discord.command.admin.KickCommand;
import dev.gnomebot.app.discord.command.admin.UnmuteCommand;
import dev.gnomebot.app.discord.command.admin.WarnCommand;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.server.handler.MinecraftHandlers;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.OngoingAction;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.ansi.log.Log;
import dev.latvian.apps.webutils.data.Confirm;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.core.spec.BanQuerySpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InteractionHandler {
	public static void chatInputInteraction(DiscordHandler handler, ChatInputInteractionEvent event) {
		var gc = event.getInteraction().getGuildId().map(handler.app.db::guild).orElse(null);
		var command = InteractionType.CHAT_INPUT.builders.get(event.getCommandName());
		var options = event.getOptions();

		while (command != null && options.size() == 1 && options.getFirst().getValue().isEmpty()) {
			command = command.getSub(options.getFirst().getName());
			options = options.getFirst().getOptions();
		}

		if (command != null && !command.supportsDM && gc == null) {
			event.reply("DM interactions aren't supported!").withEphemeral(true).subscribe();
			return;
		}

		try {
			var w = new ChatInputInteractionEventWrapper(handler.app, gc, event, options);

			if (command != null) {
				if (App.debug) {
					Log.info("Chat command '" + event.getCommandName() + "': " + w.options);
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
					var ctx = new ComplexMessageRenderContext(gc, w.context.sender.getId().asLong());
					ctx.reader = reader;
					macro.createMessageOrTimeout(ctx).thenAccept(m -> event.reply(m.ephemeral(false).toInteractionApplicationCommandCallbackSpec()).subscribe());
				} else {
					Log.error("Weird interaction data from " + event.getInteraction().getUser().getUsername() + ": " + event.getInteraction().getData());
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
		var gc = event.getInteraction().getGuildId().map(handler.app.db::guild).orElse(null);
		var command = InteractionType.USER.builders.get(event.getCommandName());

		if (command != null && !command.supportsDM && gc == null) {
			event.reply("DM interactions aren't supported!").withEphemeral(true).subscribe();
			return;
		}

		try {
			var w = new UserInteractionEventWrapper(handler.app, gc, event);

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
				Log.error("Weird interaction data from " + event.getInteraction().getUser().getUsername() + ": " + event.getInteraction().getData());
				event.reply("Command not found!").withEphemeral(true).subscribe();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void messageInteraction(DiscordHandler handler, MessageInteractionEvent event) {
		var gc = event.getInteraction().getGuildId().map(handler.app.db::guild).orElse(null);
		var command = InteractionType.MESSAGE.builders.get(event.getCommandName());

		if (command != null && !command.supportsDM && gc == null) {
			event.reply("DM interactions aren't supported!").withEphemeral(true).subscribe();
			return;
		}

		try {
			var w = new MessageInteractionEventWrapper(handler.app, gc, event);

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
				Log.error("Weird interaction data from " + event.getInteraction().getUser().getUsername() + ": " + event.getInteraction().getData());
				event.reply("Command not found!").withEphemeral(true).subscribe();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void button(DiscordHandler handler, ButtonInteractionEvent event) {
		var gc = event.getInteraction().getGuildId().map(handler.app.db::guild).orElse(null);

		if (gc != null) {
			var member = event.getInteraction().getMember().orElse(null);

			if (member != null) {
				var customId = event.getCustomId();
				var eventWrapper = new ComponentEventWrapper(handler.app, gc, event, customId);

				try {
					try {
						button(eventWrapper);
					} catch (GnomeException ex) {
						Log.error("Error in " + eventWrapper + ": " + ex.getMessage());
						eventWrapper.respond(ex.getMessage());
					} catch (Exception ex) {
						Log.error("Error in " + eventWrapper + ": " + ex);
						eventWrapper.respond("Error: " + ex);
					}
				} catch (Exception ex) {
				}
			}
		}
	}

	public static void selectMenu(DiscordHandler handler, SelectMenuInteractionEvent event) {
		var gc = event.getInteraction().getGuildId().map(handler.app.db::guild).orElse(null);

		if (gc != null) {
			var member = event.getInteraction().getMember().orElse(null);

			if (member != null) {
				var customId = event.getCustomId();

				var eventWrapper = new ComponentEventWrapper(handler.app, gc, event, customId);

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
		var gc = event.getInteraction().getGuildId().map(handler.app.db::guild).orElse(null);

		if (gc != null) {
			var member = event.getInteraction().getMember().orElse(null);

			if (member != null) {
				var customId = event.getCustomId();
				var eventWrapper = new ModalEventWrapper(handler.app, gc, event, customId);

				try {
					try {
						modalSubmit(eventWrapper);
					} catch (GnomeException ex) {
						Log.error("Error in " + eventWrapper + ": " + ex.getMessage());
						eventWrapper.respond(ex.getMessage());
					} catch (Exception ex) {
						Log.error("Error in " + eventWrapper + ": " + ex);
						eventWrapper.respond("Error: " + ex);
					}
				} catch (Exception ex) {
				}
			}
		}
	}

	public static void chatInputAutoComplete(DiscordHandler handler, ChatInputAutoCompleteEvent event) {
		var gc = event.getInteraction().getGuildId().map(handler.app.db::guild).orElse(null);

		if (gc == null) {
			return;
		}

		var command = InteractionType.CHAT_INPUT.builders.get(event.getCommandName());
		var options = event.getOptions();

		while (command != null && options.size() == 1 && options.getFirst().getValue().isEmpty()) {
			command = command.getSub(options.getFirst().getName());
			options = options.getFirst().getOptions();
		}

		try {
			if (command != null) {
				var eventWrapper = new ChatCommandSuggestionEvent(handler.app, gc, event, options);

				if (eventWrapper.focused != null) {
					// App.info(eventWrapper.focused.name + " " + command + " " + optionsToString(new StringBuilder(), options));
					var sub = command.getSub(eventWrapper.focused.name);

					if (sub != null && sub.suggestions != null) {
						sub.suggestions.getSuggestions(eventWrapper);

						if (eventWrapper.suggestions.isEmpty()) {
							event.respondWithSuggestions(Collections.emptyList()).subscribe();
						} else {
							eventWrapper.suggestions.sort(ChatCommandSuggestion::compareTo);
							var search = eventWrapper.transformSearch.apply(eventWrapper.focused.asString());

							List<ApplicationCommandOptionChoiceData> list = new ArrayList<>();

							for (var data : eventWrapper.suggestions) {
								if (list.size() == 25) {
									break;
								} else if (search.isEmpty() || data.match().startsWith(search)) {
									list.add(data.build());
								}
							}

							for (var data : eventWrapper.suggestions) {
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
			case "restart-bot" -> restartBot(event, event.path[1]);
			case "delete" -> deleteMessage(event, SnowFlake.num(event.path[1]));
			case "callback" -> callback(event, event.path[1]);
			case "stop" -> stopOngoingAction(event, event.path[1]);
			case "message-action" -> GnomeMessageInteraction.callback(event, SnowFlake.num(event.path[1]), event.path[2]);
			case "member-action" -> GnomeMemberInteraction.callback(event, SnowFlake.num(event.path[1]), event.path[2]);
			case "unmute" -> UnmuteCommand.unmuteButtonCallback(event, SnowFlake.num(event.path[1]));
			case "macro" -> MacroCommands.macroButtonCallback(event, SnowFlake.num(event.path[1]), event.path[2], 0L);
			case "edit-macro", "edit_macro" -> MacroCommands.macroButtonCallback(event, SnowFlake.num(event.path[1]), event.path[2], SnowFlake.num(event.path[3]));
			case "feedback" -> FeedbackCommand.feedbackButtonCallback(event, Integer.parseInt(event.path[1]), event.path[2].equals("upvote") ? Vote.UP : event.path[2].equals("downvote") ? Vote.DOWN : Vote.NONE);
			case "poll-vote" -> PollCommand.buttonCallback(event, Integer.parseInt(event.path[1]), Integer.parseInt(event.path[2]));
			case "warn" -> WarnCommand.warnButtonCallback(event, SnowFlake.num(event.path[1]), event.path[2], Confirm.of(event.path, 3));
			case "kick" -> KickCommand.kickButtonCallback(event, SnowFlake.num(event.path[1]), event.path[2], Confirm.of(event.path, 3));
			case "ban" -> BanCommand.banButtonCallback(event, SnowFlake.num(event.path[1]), event.path[2], Confirm.of(event.path, 3));
			case "refresh_modpack" -> ModpackCommand.refreshCallback(event);
			case "pings" -> PingsCommands.edit(event);
			case "pings-help" -> PingsCommands.help(event);
			case "regex-help" -> PingsCommands.regexHelp(event);
			case "verify-minecraft" -> MinecraftHandlers.verifyCallback(event, SnowFlake.num(event.path[1]));
			default -> {
				Log.info(event.context.sender.getTag() + " clicked " + event.context.gc + "/" + Arrays.asList(event.path));
				throw new GnomeException("Unknown button ID: " + Arrays.asList(event.path));
			}
		}
	}

	private static void selectMenu(ComponentEventWrapper event, List<String> values) throws Exception {
		switch (event.path[0]) {
			case "none" -> event.acknowledge();
			case "delete" -> deleteMessage(event, SnowFlake.num(event.path[1]));
			case "punish" -> punishMenu(event, SnowFlake.num(event.path[1]), ComponentEventWrapper.decode(event.path[2]), values.isEmpty() ? "" : values.getFirst());
			case "report" -> ReportHandler.report(event, SnowFlake.num(event.path[1]), SnowFlake.num(event.path[2]), values.getFirst());
			case "macro-menu" -> MacroCommands.macroButtonCallback(event, SnowFlake.num(event.path[1]), values.getFirst(), SnowFlake.num(event.path[2]));
			default -> {
				Log.info(event.context.sender.getTag() + " selected " + event.context.gc + "/" + Arrays.asList(event.path) + "/" + values);
				throw new GnomeException("Unknown select menu ID: " + Arrays.asList(event.path) + "/" + values);
			}
		}
	}

	private static void modalSubmit(ModalEventWrapper event) {
		switch (event.path[0]) {
			case "none" -> event.acknowledge();
			case "delete" -> deleteMessage(event, SnowFlake.num(event.path[1]));
			case "modal-test" -> event.respond("Modal: " + event);
			case "modmail" -> ModmailCommand.modmailCallback(event);
			case "report" -> ReportCommand.reportCallback(event, SnowFlake.num(event.path[1]), SnowFlake.num(event.path[2]));
			case "feedback" -> FeedbackCommand.submitCallback(event);
			case "poll" -> PollCommand.submitCallback(event);
			case "ping-test" -> PingsCommands.testCallback(event, SnowFlake.num(event.path[1]));
			case "add-macro" -> MacroCommands.addMacroCallback(event, event.path[1]);
			case "edit-macro" -> MacroCommands.editMacroCallback(event, event.path[1]);
			case "pings" -> PingsCommands.editCallback(event);
			case "webhook" -> WebhookCommands.executeCallback(event, SnowFlake.num(event.path[1]), SnowFlake.num(event.path[2]));
			case "create-paste" -> PasteCommands.createCallback(event);
			default -> {
				Log.warn(event.context.sender.getTag() + " submitted unknown modal " + event.context.gc + "/" + event);
				throw new GnomeException("Unknown modal ID: " + event);
			}
		}
	}

	// Actions //

	private static void restartBot(DeferrableInteractionEventWrapper<?> event, String token) {
		if (event.context.gc.db.app.config.discord.restart_button_token.equals(token)) {
			event.edit().respond(MessageBuilder.create("# Restart Gnome Bot\nLast clicked by " + event.context.sender.getMention() + " " + Utils.formatRelativeDate(Instant.now())));
			event.context.handler.app.restart();
		} else if (event.requiresTextResponse()) {
			event.respond("Incorrect token!");
		} else {
			event.acknowledge();
		}
	}

	private static void deleteMessage(DeferrableInteractionEventWrapper<?> event, long owner) {
		if (event.context.isAdmin() || event.context.sender.getId().asLong() == owner) {
			event.getResponse().deleteInitialResponse().block();
		} else if (event.requiresTextResponse()) {
			event.respond("You can't delete this message!");
		} else {
			event.acknowledge();
		}
	}

	private static void callback(ComponentEventWrapper event, String id) {
		var callback = ComponentCallback.MAP.get(id);

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

	private static void punishMenu(ComponentEventWrapper event, long userId, String reason, String type) {
		event.context.checkSenderAdmin();

		switch (type) {
			case "kick" -> {
				event.context.gc.getGuild().kick(SnowFlake.convert(userId), reason).subscribe();
				event.respond("Kicked <@" + userId + ">");
			}
			case "ban" -> {
				event.context.gc.getGuild().ban(SnowFlake.convert(userId), BanQuerySpec.builder().deleteMessageDays(1).reason(reason).build()).subscribe();
				event.respond("Banned <@" + userId + ">");
			}
			case "unmute" -> {
				event.context.gc.unmute(userId, 0L, "");
				event.respond("Unmuted <@" + userId + ">");
			}
			default -> event.respond("This action cannot be undone!");
		}
	}

	private static void stopOngoingAction(ComponentEventWrapper event, String id) {
		event.acknowledge();
		OngoingAction.stop(id);
	}
}