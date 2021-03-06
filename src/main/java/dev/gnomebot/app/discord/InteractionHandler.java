package dev.gnomebot.app.discord;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Updates;
import dev.gnomebot.app.App;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.DiscordFeedback;
import dev.gnomebot.app.data.DiscordPoll;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.Macro;
import dev.gnomebot.app.data.Vote;
import dev.gnomebot.app.data.WebhookExecuteExtra;
import dev.gnomebot.app.data.ping.UserPings;
import dev.gnomebot.app.discord.command.ChatCommandSuggestion;
import dev.gnomebot.app.discord.command.ChatCommandSuggestionEvent;
import dev.gnomebot.app.discord.command.ChatInputInteractionEventWrapper;
import dev.gnomebot.app.discord.command.InteractionType;
import dev.gnomebot.app.discord.command.MessageInteractionEventWrapper;
import dev.gnomebot.app.discord.command.ModpackCommand;
import dev.gnomebot.app.discord.command.PingsCommands;
import dev.gnomebot.app.discord.command.UserInteractionEventWrapper;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.script.event.ComponentEventJS;
import dev.gnomebot.app.script.event.ModalEventJS;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.OngoingAction;
import dev.gnomebot.app.util.ThreadMessageRequest;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.BanQuerySpec;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.rest.util.Permission;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
				Macro macro = gc.getMacro(event.getCommandName());

				if (macro != null) {
					macro.update(Updates.inc("uses", 1));
					event.reply(macro.createMessage(w.context.sender.getId(), false).ephemeral(false).toInteractionApplicationCommandCallbackSpec()).subscribe();
				} else {
					App.error("Weird interaction data from " + event.getInteraction().getUser().getUsername() + ": " + event.getInteraction().getData());
					event.reply("Command not found!").withEphemeral(true).subscribe();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
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

				if (gc.discordJS.onButton.hasListeners() && gc.discordJS.onButton.post(new ComponentEventJS(customId, gc.getWrappedGuild().getUser(member.getId().asString()), eventWrapper), true)) {
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

				if (gc.discordJS.onSelectMenu.hasListeners() && gc.discordJS.onSelectMenu.post(new ComponentEventJS(customId, gc.getWrappedGuild().getUser(member.getId().asString()), eventWrapper), true)) {
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

				if (gc.discordJS.onModal.hasListeners() && gc.discordJS.onModal.post(new ModalEventJS(customId, gc.getWrappedGuild().getUser(member.getId().asString()), eventWrapper), true)) {
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

	private static void button(ComponentEventWrapper event) {
		switch (event.path[0]) {
			case "none" -> event.acknowledge();
			case "delete" -> deleteMessage(event, Snowflake.of(event.path[1]));
			case "callback" -> callback(event, event.path[1]);
			case "unmute" -> unmute(event, Snowflake.of(event.path[1]));
			case "macro" -> macro(event, event.path[1], null);
			case "edit_macro" -> macro(event, event.path[1], Snowflake.of(event.path[2]));
			case "feedback" -> feedback(event, Integer.parseInt(event.path[1]), event.path[2].equals("upvote") ? Vote.UP : event.path[2].equals("downvote") ? Vote.DOWN : Vote.NONE);
			case "warn" -> warn(event, Snowflake.of(event.path[1]), event.path[2], Confirm.of(event.path, 3));
			case "kick" -> kick(event, Snowflake.of(event.path[1]), event.path[2], Confirm.of(event.path, 3));
			case "ban" -> ban(event, Snowflake.of(event.path[1]), event.path[2], Confirm.of(event.path, 3));
			case "refresh_modpack" -> refreshModpack(event);
			case "stop" -> stopOngoingAction(event, event.path[1]);
			case "modal_test" -> modalTest(event);
			case "pings" -> PingsCommands.edit(event);
			case "pings_help" -> PingsCommands.help(event);
			case "regex_help" -> PingsCommands.regexHelp(event);
			default -> {
				App.info(event.context.sender.getTag() + " clicked " + event.context.gc + "/" + Arrays.asList(event.path));
				throw new GnomeException("Unknown button ID: " + Arrays.asList(event.path));
			}
		}
	}

	private static void selectMenu(ComponentEventWrapper event, List<String> values) {
		switch (event.path[0]) {
			case "none" -> event.acknowledge();
			case "delete" -> deleteMessage(event, Snowflake.of(event.path[1]));
			case "poll" -> poll(event, Integer.parseInt(event.path[1]), values.get(0));
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
			case "modal_test" -> event.respond("Modal: " + event);
			case "modmail" -> modmail(event);
			case "report" -> report(event, Snowflake.of(event.path[1]), Snowflake.of(event.path[2]));
			case "feedback" -> feedback(event);
			case "add_macro" -> addMacro(event, event.path[1]);
			case "edit_macro" -> editMacro(event, event.path[1]);
			case "pings" -> pings(event);
			case "webhook" -> webhook(event, Snowflake.of(event.path[1]), Snowflake.of(event.path[2]));
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

	private static void feedback(ComponentEventWrapper event, int number, Vote vote) {
		DiscordFeedback feedback = event.context.gc.feedback.query().eq("number", number).first();

		if (feedback == null) {
			event.acknowledge();
			return;
		}

		Message m = event.context.channelInfo.getMessage(Snowflake.of(feedback.getUID()));

		if (!feedback.getStatus().canEdit()) {
			throw new GnomeException("You can't vote for this suggestion, it's already decided on!");
		}

		if (event.context.gc.feedbackVoteRole.is(event.context.sender)) {
			event.acknowledge();

			if (feedback.setVote(event.context.sender.getId().asString(), vote)) {
				EmbedCreateFields.Footer footer = Utils.getFooter(m);
				m.edit(MessageEditSpec.builder().addEmbed(feedback.edit(event.context.gc, footer)).build()).subscribe();
			}
		} else {
			throw new GnomeException("You can't vote for this suggestion, you have to have " + event.context.gc.regularRole + " role!");
		}
	}

	private static void warn(ComponentEventWrapper event, Snowflake other, String reason, Confirm confirm) {
		event.context.checkSenderAdmin();
		//other.kick(reason).subscribe();
		Utils.editComponents(event.event.getMessage().orElse(null), Collections.singletonList(ActionRow.of(Button.danger("none", Emojis.WARNING, "Warned by " + event.context.sender.getUsername() + "!")).getData()));
		event.respond("Warned <@" + other.asString() + ">");
	}

	private static void kick(ComponentEventWrapper event, Snowflake other, String reason, Confirm confirm) {
		event.context.checkSenderAdmin();
		event.context.gc.getGuild().kick(other, reason).subscribe();
		Utils.editComponents(event.event.getMessage().orElse(null), Collections.singletonList(ActionRow.of(Button.danger("none", Emojis.WARNING, "Kicked by " + event.context.sender.getUsername() + "!")).getData()));
		event.respond("Kicked <@" + other.asString() + ">");
	}

	private static void ban(ComponentEventWrapper event, Snowflake other, String reason, Confirm confirm) {
		event.context.checkSenderAdmin();
		event.context.gc.getGuild().ban(other, BanQuerySpec.builder().deleteMessageDays(1).reason(reason).build()).subscribe();
		Utils.editComponents(event.event.getMessage().orElse(null), Collections.singletonList(ActionRow.of(Button.danger("none", Emojis.WARNING, "Banned by " + event.context.sender.getUsername() + "!")).getData()));
		event.respond("Banned <@" + other.asString() + ">");
	}

	private static void unmute(ComponentEventWrapper event, Snowflake other) {
		event.context.checkSenderAdmin();
		event.context.gc.unmute(other, 0L);
		Utils.editComponents(event.event.getMessage().orElse(null), Collections.singletonList(ActionRow.of(Button.secondary("none", Emojis.CHECKMARK, "Unmuted by " + event.context.sender.getUsername() + "!")).getData()));
		event.respond("Unmuted <@" + other.asString() + ">");
	}

	private static void macro(ComponentEventWrapper event, String name, Snowflake owner) {
		Macro macro = event.context.gc.getMacro(name);

		if (macro == null) {
			throw new GnomeException("Macro '" + name + "' not found!");
		}

		if (owner != null) {
			if (owner.asLong() != event.context.sender.getId().asLong()) {
				event.acknowledge();
			} else {
				macro.update(Updates.inc("uses", 1));
				event.edit().respond(macro.createMessage(owner, false).ephemeral(true));
			}
		} else {
			macro.update(Updates.inc("uses", 1));
			event.respond(macro.createMessage(event.context.sender.getId(), false).ephemeral(true));
		}
	}

	private static void poll(ComponentEventWrapper event, int number, String value) {
		DiscordPoll poll = event.context.gc.polls.query().eq("number", number).first();

		if (poll == null) {
			event.acknowledge();
			return;
		}

		Message m = event.context.channelInfo.getMessage(Snowflake.of(poll.getUID()));

		if (value.equals("vote/none")) {
			event.acknowledge();

			if (poll.setVote(event.context.sender.getId().asString(), -1)) {
				EmbedCreateFields.Footer footer = Utils.getFooter(m);
				m.edit(MessageEditSpec.builder().addEmbed(poll.edit(event.context.gc, footer)).build()).subscribe();
			}
		} else if (value.startsWith("vote/")) {
			event.acknowledge();

			if (poll.setVote(event.context.sender.getId().asString(), Integer.parseInt(value.substring(5)))) {
				EmbedCreateFields.Footer footer = Utils.getFooter(m);
				m.edit(MessageEditSpec.builder().addEmbed(poll.edit(event.context.gc, footer)).build()).subscribe();
			}
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
				event.context.gc.unmute(userId, 0L);
				event.respond("Unmuted <@" + userId.asString() + ">");
			}
			default -> event.respond("This action cannot be undone!");
		}
	}

	private static void refreshModpack(ComponentEventWrapper event) {
		if (event.context.message.getInteraction().isPresent() && event.context.sender.getId().equals(event.context.message.getInteraction().get().getUser().getId())) {
			MessageBuilder builder = MessageBuilder.create();

			ModpackCommand.Pack pack = ModpackCommand.getRandomPack();

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

	private static void stopOngoingAction(ComponentEventWrapper event, String id) {
		event.acknowledge();
		OngoingAction.stop(id);
	}

	private static void modalTest(ComponentEventWrapper event) {
		event.respondModal("modal_test", "Modal Test",
				TextInput.small("modal_test_1", "Test 1", "Placeholder text 1"),
				TextInput.paragraph("modal_test_2", "Test 2", "Placeholder text 2").required(false),
				SelectMenu.of("select_test",
						SelectMenu.Option.of("Test 1", "test_1"),
						SelectMenu.Option.of("Test 2", "test_2"),
						SelectMenu.Option.of("Test 3", "test_3")
				)
		);
	}

	private static void modmail(ModalEventWrapper event) {
		event.respond("Message sent!");

		String message = event.get("message").asString();

		event.context.gc.adminMessagesChannel.messageChannel().flatMap(ChannelInfo::getWebHook).ifPresent(w -> w.execute(MessageBuilder.create()
				.webhookName("Modmail from " + event.context.sender.getTag())
				.webhookAvatarUrl(event.context.sender.getAvatarUrl())
				.allowUserMentions(event.context.sender.getId())
				.content(event.context.sender.getMention() + ":\n" + message)
		));
	}

	private static void report(ModalEventWrapper event, Snowflake channel, Snowflake user) {
		if (true) {
			event.respond("Reporting isn't implemented yet! You'll have to ping admins");
			return;
		}

		CachedRole role = event.context.gc.reportMentionRole.getRole();

		if (role == null) {
			event.respond("Thank you for your report!");
		} else {
			event.respond("Thank you for your report! <@&" + role.id.asString() + "> have been notified.");
		}

		/*
		event.respond(msg -> {
			if (role == null) {
				msg.content("Select reason for reporting this message:");
			} else {
				msg.content("Select reason for reporting this message: (<@&" + role.id.asString() + "> will be pinged)");
			}

			List<SelectMenu.Option> options = new ArrayList<>();
			options.add(SelectMenu.Option.of("Cancel", "-"));

			for (String s : event.context.gc.reportOptions.get().split(" \\| ")) {
				options.add(SelectMenu.Option.of(s, s));
			}

			options.add(SelectMenu.Option.of("Other", "Other"));
			msg.addComponent(ActionRow.of(SelectMenu.of("report/" + m.getChannelId().asString() + "/" + m.getId().asString(), options).withPlaceholder("Select Reason...")).getData());
		});
		 */
	}

	private static void feedback(ModalEventWrapper event) {
		//event.respond("Feedback sent!");

		// event.acknowledgeEphemeral();
		ChannelInfo feedbackChannel = event.context.gc.feedbackChannel.messageChannel().orElse(null);

		if (feedbackChannel == null) {
			throw new GnomeException("Feedback channel is not set up on this server!");
		}

		String suggestion = event.get("feedback").asString();

		int number = event.context.gc.feedbackNumber.get() + 1;
		event.context.gc.feedbackNumber.set(number);
		event.context.gc.feedbackNumber.save();

		event.context.referenceMessage = false;

		event.context.checkBotPerms(feedbackChannel, Permission.ADD_REACTIONS, Permission.SEND_MESSAGES);

		Message m = feedbackChannel.createMessage(EmbedBuilder.create()
				.url(App.url("feedback/" + event.context.gc.guildId.asString() + "/" + number))
				.title("Loading suggestion #" + number + "...")
		).block();

		Document document = new Document();
		document.put("_id", m.getId().asLong());
		document.put("author", event.context.sender.getId().asLong());
		document.put("timestamp", Date.from(m.getTimestamp()));
		document.put("number", number);
		document.put("content", suggestion);
		document.put("status", 0);
		BasicDBObject votes = new BasicDBObject();
		votes.put(event.context.sender.getId().asString(), true);
		document.put("votes", votes);
		event.context.gc.feedback.insert(document);
		m.edit(MessageEditSpec.builder().addEmbed(event.context.gc.feedback.findFirst(m).edit(event.context.gc, event.context.gc.anonymousFeedback.get() ? null : EmbedCreateFields.Footer.of(event.context.sender.getTag(), event.context.sender.getAvatarUrl()))).build()).block();

		try {
			Utils.THREAD_ROUTE.newRequest(m.getChannelId().asLong(), m.getId().asLong())
					.body(new ThreadMessageRequest("Discussion of " + number))
					.exchange(event.context.handler.client.getCoreResources().getRouter())
					.skipBody()
					.block();
		} catch (Exception ex) {
			App.error("Failed to create a thread for suggestion " + event.context.gc + "/#" + number);
		}

		m.edit(MessageEditSpec.builder().addComponent(ActionRow.of(
				Button.secondary("feedback/" + number + "/upvote", Emojis.VOTEUP),
				Button.secondary("feedback/" + number + "/mehvote", Emojis.VOTENONE),
				Button.secondary("feedback/" + number + "/downvote", Emojis.VOTEDOWN),
				Button.link(QuoteHandler.getChannelURL(event.context.gc.guildId, m.getId()), "Discussion")
		)).build()).block();

		event.respond(MessageBuilder.create("Your feedback has been submitted!").addComponentRow(Button.link(QuoteHandler.getMessageURL(event.context.gc.guildId, m.getChannelId(), m.getId()), "Open")));
	}

	private static void addMacro(ModalEventWrapper event, String name) {
		if (name.isEmpty()) {
			throw new GnomeException("Macro name can't be empty!");
		} else if (name.length() > 50) {
			throw new GnomeException("Macro name too long! Max 50 characters.");
		}

		if (event.context.gc.getMacro(name) != null) {
			throw new GnomeException("Macro with that name already exists!");
		}

		String content = event.get("content").asString()
				.replaceAll("<@&(\\d+)>", "role:$1")
				.replaceAll("<@(\\d+)>", "user:$1")
				.replace("@here", "mention:here")
				.replace("@everyone", "mention:everyone");

		if (content.isEmpty()) {
			throw new GnomeException("Can't have empty content!");
		}

		List<String> extra = new ArrayList<>(Arrays.stream(event.get("extra").asString().trim().split("\n")).map(String::trim).filter(s -> !s.isEmpty()).toList());

		Document document = new Document();
		document.put("name", name);
		document.put("content", content);

		extra.remove("clear");

		if (!extra.isEmpty()) {
			document.put("extra", extra);
		}

		document.put("author", event.context.sender.getId().asLong());
		document.put("created", new Date());
		document.put("uses", 0);
		document.put("type", "text");
		event.context.gc.macros.insert(document);
		event.context.gc.updateMacroMap();

		event.respond(MessageBuilder.create("Macro '" + name + "' created!").ephemeral(false));
	}

	private static void editMacro(ModalEventWrapper event, String name) {
		if (name.isEmpty()) {
			throw new GnomeException("Macro name can't be empty!");
		}

		Macro macro = event.context.gc.getMacro(name);

		if (macro == null) {
			throw new GnomeException("Macro not found!");
		} else if (macro.getAuthor() != event.context.sender.getId().asLong() && !event.context.isAdmin()) {
			throw new GnomeException("You can only edit your own macros!");
		}

		String rename = event.get("rename").asString(macro.getName());

		List<Bson> updates = new ArrayList<>();
		long slashId = 0L;

		if (!rename.equals(macro.getName())) {
			if (rename.length() > 50) {
				throw new GnomeException("Macro name too long! Max 50 characters.");
			}

			if (event.context.gc.getMacro(rename) != null) {
				throw new GnomeException("Macro with that name already exists!");
			}

			slashId = macro.setSlashCommand(false);
			updates.add(Updates.set("name", rename));
		}

		String content = event.get("content").asString(macro.getContent());

		if (!content.equals(macro.getContent())) {
			updates.add(Updates.set("content", content));
		}

		List<String> extra = Arrays.stream(event.get("extra").asString().trim().split("\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();

		if (!extra.isEmpty()) {
			if (extra.contains("clear")) {
				updates.add(Updates.unset("extra"));
			} else {
				updates.add(Updates.set("extra", extra));
			}
		}

		macro.update(updates);

		if (slashId != 0L) {
			macro.setSlashCommand(true);
		}

		event.context.gc.updateMacroMap();
		event.respond(MessageBuilder.create("Macro '" + rename + "' updated!").ephemeral(false));
	}

	private static void pings(ModalEventWrapper event) {
		if (!event.context.isAdmin()) {
			// event.respond("WIP! For now this command is only available to admins!");
			// return;
		}

		String config = event.get("config").asString().trim();

		try {
			event.context.gc.db.userPings.query(event.context.sender).upsert(Collections.singletonList(Updates.set("config", config)));

			if (config.isEmpty()) {
				event.context.gc.db.app.pingHandler.update();
				event.respond("Pings cleared!");
				return;
			}

			var pings = UserPings.compile(event.context.gc.db, event.context.sender.getId(), config);

			App.success(event.context.sender.getUsername() + " updated their pings:");

			for (var ping : pings) {
				App.info("- name: " + ping.name);
				App.info("  config: " + ping.buildConfig());
				App.info("  pings:");

				for (var p : ping.pings) {
					App.info("  " + p);
				}
			}

			event.context.gc.db.app.pingHandler.update();
			event.respond("Pings set!");
		} catch (GnomeException ex) {
			event.respond(MessageBuilder.create("Syntax error on line " + ex.position + ":\n" + ex.getMessage()).addComponentRow(
					Button.primary("pings", "Edit"),
					Button.secondary("pings_help", "Help"),
					Button.secondary("regex_help", "RegEx Guide")
			));
		} catch (Exception ex) {
			event.respond(MessageBuilder.create("Syntax error:\n" + ex.getMessage()).addComponentRow(
					Button.primary("pings", "Edit"),
					Button.secondary("pings_help", "Help"),
					Button.secondary("regex_help", "RegEx Guide")
			));
		}
	}

	private static void webhook(ModalEventWrapper event, Snowflake channelId, Snowflake editId) {
		event.context.checkSenderOwner();
		ChannelInfo ci = event.context.gc.getOrMakeChannelInfo(channelId);
		WebHook webHook = ci.getWebHook().orElse(null);

		if (webHook == null) {
			throw new GnomeException("Failed to retrieve webhook!");
		}

		String content = event.get("content").asString();
		List<String> extra = Arrays.asList(event.get("extra").asString().split("\n"));
		MessageBuilder message = Macro.createMessage(content, extra, Utils.NO_SNOWFLAKE, false);
		message.webhookName(event.get("username").asString(event.context.gc.toString()));
		message.webhookAvatarUrl(event.get("avatar_url").asString(event.context.gc.iconUrl.get()));

		Snowflake id = editId.asLong() == 0L ? webHook.execute(message) : editId;

		if (editId.asLong() != 0L) {
			webHook.edit(editId.asString(), message).block();

			WebhookExecuteExtra info = event.context.gc.db.webhookExecuteExtra.findFirst(editId.asLong());

			if (info != null) {
				info.update(Updates.set("extra", String.join("\n", extra)));
			}

			event.respond("Done!");
		} else if (id != Utils.NO_SNOWFLAKE) {
			Document doc = new Document();
			doc.put("_id", id.asLong());
			doc.put("extra", String.join("\n", extra));
			event.context.gc.db.webhookExecuteExtra.insert(doc);
			event.respond("Done!");
		} else {
			throw new GnomeException("Failed to send webhook!");
		}

		// Click this button (Or do `??gamenight` in <#397702104204050434> if it doesn't work):
		// macro "Toggle Game Night Role" gamenight
	}
}