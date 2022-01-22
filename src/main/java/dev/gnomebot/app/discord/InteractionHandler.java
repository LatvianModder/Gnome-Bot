package dev.gnomebot.app.discord;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.App;
import dev.gnomebot.app.data.DiscordCustomCommand;
import dev.gnomebot.app.data.DiscordFeedback;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.data.DiscordPoll;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.Macro;
import dev.gnomebot.app.data.Vote;
import dev.gnomebot.app.discord.command.ApplicationCommandEventWrapper;
import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.discord.command.ChatCommandSuggestion;
import dev.gnomebot.app.discord.command.ChatCommandSuggestionEvent;
import dev.gnomebot.app.discord.command.CommandBuilder;
import dev.gnomebot.app.discord.command.ModpackCommand;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.util.Ansi;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.BanQuerySpec;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InteractionHandler {
	public static void applicationCommand(DiscordHandler handler, ApplicationCommandInteractionEvent event) {
		GuildCollections gc = event.getInteraction().getGuildId().map(handler.app.db::guild).orElse(null);

		if (gc == null) {
			event.reply("DM interactions aren't supported!").withEphemeral(true).subscribe();
			return;
		}

		CommandBuilder command = ApplicationCommands.COMMANDS.get(event.getCommandName());
		List<ApplicationCommandInteractionOption> options = event instanceof ChatInputInteractionEvent ? ((ChatInputInteractionEvent) event).getOptions() : new ArrayList<>();

		while (command != null && options.size() == 1 && options.get(0).getValue().isEmpty()) {
			command = command.getSub(options.get(0).getName());
			options = options.get(0).getOptions();
		}

		try {
			if (command != null) {
				ApplicationCommandEventWrapper w = new ApplicationCommandEventWrapper(gc, event, options);

				try {
					command.callback.run(w);
				} catch (DiscordCommandException ex) {
					w.acknowledgeEphemeral();
					w.respond(ex.getMessage());
				} catch (Exception ex) {
					w.acknowledgeEphemeral();
					w.respond(ex.toString());
					ex.printStackTrace();
				}
			} else {
				Macro macro = gc.macros.query().eq("slash_command", event.getCommandId().asLong()).first();

				if (macro != null) {
					InteractionApplicationCommandCallbackSpec.Builder spec = InteractionApplicationCommandCallbackSpec.builder();
					spec.allowedMentions(DiscordMessage.noMentions());
					macro.createMessage(spec);
					event.reply(spec.build()).subscribe();
					macro.update(Updates.inc("uses", 1));
				} else {
					App.error("Weird interaction data from " + event.getInteraction().getUser().getUsername() + ": " + event.getInteraction().getData());
					event.reply("Command not found!").withEphemeral(true).subscribe();
				}
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

				try {
					try {
						button(eventWrapper);
					} catch (DiscordCommandException ex) {
						App.error("Error in " + eventWrapper + ": " + ex.getMessage());
						eventWrapper.respond(ex.getMessage());
					} catch (Exception ex) {
						App.error("Error in " + eventWrapper + ": " + ex);
						eventWrapper.respond("Error: " + ex);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
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

				try {
					selectMenu(eventWrapper, event.getValues());
				} catch (DiscordCommandException ex) {
					eventWrapper.respond(ex.getMessage());
				} catch (Exception ex) {
					eventWrapper.respond("Error: " + ex);
				}
			}
		}
	}

	private static void button(ComponentEventWrapper event) throws DiscordCommandException {
		switch (event.path[0]) {
			case "none" -> event.acknowledge();
			case "unmute" -> unmute(event, event.getMember(1));
			case "macro" -> macro(event, event.path[1]);
			case "feedback" -> feedback(event, Integer.parseInt(event.path[1]), event.path[2].equals("upvote") ? Vote.UP : event.path[2].equals("downvote") ? Vote.DOWN : Vote.NONE);
			case "warn" -> warn(event, event.getMember(1), event.path[2], Confirm.of(event.path, 3));
			case "kick" -> kick(event, event.getMember(1), event.path[2], Confirm.of(event.path, 3));
			case "ban" -> ban(event, event.getMember(1), event.path[2], Confirm.of(event.path, 3));
			case "refresh_modpack" -> refreshModpack(event);
			default -> {
				App.info(event.context.sender.getTag() + " clicked " + event.context.gc + "/" + Arrays.asList(event.path));
				throw new DiscordCommandException("Unknown button ID: " + Arrays.asList(event.path));
			}
		}
	}

	private static void selectMenu(ComponentEventWrapper event, List<String> values) throws DiscordCommandException {
		switch (event.path[0]) {
			case "none" -> event.acknowledge();
			case "button" -> {
				event.acknowledge();

				for (String val : values) {
					button(event.copy(val));
				}
			}
			case "poll" -> poll(event, Integer.parseInt(event.path[1]), values.get(0));
			case "report" -> ReportHandler.report(event, Snowflake.of(event.path[1]), Snowflake.of(event.path[2]), values.get(0));
			default -> {
				App.info(event.context.sender.getTag() + " selected " + event.context.gc + "/" + Arrays.asList(event.path) + "/" + values);
				throw new DiscordCommandException("Unknown select menu ID: " + Arrays.asList(event.path) + "/" + values);
			}
		}
	}


	// Actions //
	private static void feedback(ComponentEventWrapper event, int number, Vote vote) throws DiscordCommandException {
		DiscordFeedback feedback = event.context.gc.feedback.query().eq("number", number).first();

		if (feedback == null) {
			event.acknowledge();
			return;
		}

		Message m = event.context.channelInfo.getMessage(Snowflake.of(feedback.getUID()));

		if (!feedback.getStatus().canEdit()) {
			throw new DiscordCommandException("You can't vote for this suggestion, it's already decided on!");
		}

		if (event.context.gc.feedbackVoteRole.is(event.context.sender)) {
			event.acknowledge();

			if (feedback.setVote(event.context.sender.getId().asString(), vote)) {
				EmbedCreateFields.Footer footer = Utils.getFooter(m);
				m.edit(MessageEditSpec.builder().addEmbed(feedback.edit(event.context.gc, footer)).build()).subscribe();
			}
		} else {
			throw new DiscordCommandException("You can't vote for this suggestion, you have to have " + event.context.gc.regularRole + " role!");
		}
	}

	private static void warn(ComponentEventWrapper event, Member other, String reason, Confirm confirm) throws DiscordCommandException {
		event.context.checkSenderAdmin();
		//other.kick(reason).subscribe();
		Utils.editComponents(event.event.getMessage().orElse(null), Collections.singletonList(ActionRow.of(Button.danger("none", Emojis.WARNING, "Warned by " + event.context.sender.getUsername() + "!")).getData()));
		event.respond("Warned " + other.getMention());
	}

	private static void kick(ComponentEventWrapper event, Member other, String reason, Confirm confirm) throws DiscordCommandException {
		event.context.checkSenderAdmin();
		other.kick(reason).subscribe();
		Utils.editComponents(event.event.getMessage().orElse(null), Collections.singletonList(ActionRow.of(Button.danger("none", Emojis.WARNING, "Kicked by " + event.context.sender.getUsername() + "!")).getData()));
		event.respond("Kicked " + other.getMention());
	}

	private static void ban(ComponentEventWrapper event, Member other, String reason, Confirm confirm) throws DiscordCommandException {
		event.context.checkSenderAdmin();
		other.ban(BanQuerySpec.builder().deleteMessageDays(1).reason(reason).build()).subscribe();
		Utils.editComponents(event.event.getMessage().orElse(null), Collections.singletonList(ActionRow.of(Button.danger("none", Emojis.WARNING, "Banned by " + event.context.sender.getUsername() + "!")).getData()));
		event.respond("Banned " + other.getMention());
	}

	private static void unmute(ComponentEventWrapper event, Member other) throws DiscordCommandException {
		event.context.checkSenderAdmin();
		event.context.gc.unmute(other.getId(), 0L);
		Utils.editComponents(event.event.getMessage().orElse(null), Collections.singletonList(ActionRow.of(Button.secondary("none", Emojis.CHECKMARK, "Unmuted by " + event.context.sender.getUsername() + "!")).getData()));
		event.respond("Unmuted " + other.getMention());
	}

	private static void macro(ComponentEventWrapper event, String id) throws DiscordCommandException {
		DiscordCustomCommand customCommand = event.context.gc.customCommands.query().eq("command_name", id.toLowerCase()).first();

		if (customCommand != null) {
			event.acknowledge();

			try {
				customCommand.runCommand(event.context, new CommandReader(event.context.gc, ""));
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		Macro macro = event.context.gc.macros.query().eq("command_name", id.toLowerCase()).first();

		if (macro == null) {
			throw new DiscordCommandException("Macro '" + id + "' not found!");
		}

		event.respond(macro::createMessage);
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

	private static void refreshModpack(ComponentEventWrapper event) {
		if (event.context.message.getInteraction().isPresent() && event.context.sender.getId().equals(event.context.message.getInteraction().get().getUser().getId())) {
			InteractionApplicationCommandCallbackSpec.Builder builder = InteractionApplicationCommandCallbackSpec.builder();
			builder.allowedMentions(DiscordMessage.noMentions());

			ModpackCommand.Pack pack = ModpackCommand.getRandomPack();

			builder.addEmbed(EmbedCreateSpec.builder()
					.color(EmbedColors.GRAY)
					.title("What pack should I play?")
					.description("[" + pack.name + "](" + pack.url + ")")
					.build()
			);

			event.event.edit(builder.build()).subscribe();
		} else {
			event.acknowledge();
		}
	}

	public static void chatInputAutoComplete(DiscordHandler handler, ChatInputAutoCompleteEvent event) {
		GuildCollections gc = event.getInteraction().getGuildId().map(handler.app.db::guild).orElse(null);

		if (gc == null) {
			return;
		}

		CommandBuilder command = ApplicationCommands.COMMANDS.get(event.getCommandName());
		List<ApplicationCommandInteractionOption> options = event.getOptions();

		while (command != null && options.size() == 1 && options.get(0).getValue().isEmpty()) {
			command = command.getSub(options.get(0).getName());
			options = options.get(0).getOptions();
		}

		try {
			if (command != null) {
				ChatCommandSuggestionEvent eventWrapper = new ChatCommandSuggestionEvent(gc, event, options);

				if (eventWrapper.focused != null) {
					CommandBuilder sub = command.getSub(eventWrapper.focused.name);

					if (sub != null && sub.suggestions != null) {
						sub.suggestions.getSuggestions(eventWrapper);

						if (eventWrapper.suggestions.isEmpty()) {
							event.respondWithSuggestions(Collections.emptyList()).subscribe();
						} else {
							eventWrapper.suggestions.sort(ChatCommandSuggestion::compareTo);
							String search = eventWrapper.focused.asString().toLowerCase();

							List<ApplicationCommandOptionChoiceData> list = new ArrayList<>();

							for (ChatCommandSuggestion data : eventWrapper.suggestions) {
								if (list.size() == 25) {
									break;
								} else if (data.name().toLowerCase().startsWith(search)) {
									list.add(data.build());
								}
							}

							for (ChatCommandSuggestion data : eventWrapper.suggestions) {
								if (list.size() == 25) {
									break;
								} else if (!data.name().toLowerCase().startsWith(search) && data.name().toLowerCase().contains(search)) {
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

	public static void optionsToString(StringBuilder sb, List<ApplicationCommandInteractionOption> options) {
		sb.append('{');

		for (int i = 0; i < options.size(); i++) {
			if (i > 0) {
				sb.append(", ");
			}

			ApplicationCommandInteractionOption o = options.get(i);

			if (o.isFocused()) {
				sb.append(Ansi.GREEN);
			}

			sb.append(o.getName());

			if (o.isFocused()) {
				sb.append(Ansi.RESET);
			}

			sb.append("=");

			if (o.getOptions().isEmpty()) {
				if (o.getValue().isEmpty()) {
					sb.append("<Empty>");
				} else {
					sb.append('"');
					sb.append(o.getValue().get().getRaw());
					sb.append('"');
				}
			} else {
				optionsToString(sb, o.getOptions());
			}

			sb.append(']');
		}

		sb.append('}');
	}
}