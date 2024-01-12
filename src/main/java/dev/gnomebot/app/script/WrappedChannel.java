package dev.gnomebot.app.script;

import dev.gnomebot.app.util.MessageBuilder;
import dev.latvian.mods.rhino.util.HideFromJS;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.ChannelModifyRequest;
import discord4j.rest.service.ChannelService;
import org.jetbrains.annotations.Nullable;

public class WrappedChannel extends DiscordObject {
	public final WrappedGuild guild;
	protected String name;
	protected String topic;
	protected Boolean nsfw;

	public WrappedChannel(WrappedId id, WrappedGuild g) {
		super(id);
		guild = g;
		name = null;
		topic = null;
		nsfw = null;
	}

	@Override
	public String toString() {
		return "#" + getName();
	}

	public String getMention() {
		return "<#" + id + ">";
	}

	public String getName() {
		if (name == null) {
			try {
				name = guild.gc.getChannelMap().get(id.asLong()).getName();
			} catch (Exception ex) {
				ex.printStackTrace();
				name = "deleted-channel";
			}
		}

		return name;
	}

	public String getTopic() {
		if (topic == null) {
			try {
				topic = guild.gc.getChannelMap().get(id.asLong()).getTopic().orElse("");
			} catch (Exception ex) {
				ex.printStackTrace();
				topic = "";
			}
		}

		return topic;
	}

	public boolean getNsfw() {
		if (nsfw == null) {
			try {
				nsfw = guild.gc.getChannelMap().get(id.asLong()).isNsfw();
			} catch (Exception ex) {
				ex.printStackTrace();
				nsfw = false;
			}
		}

		return nsfw;
	}

	@HideFromJS
	public ChannelService getChannelService() {
		return guild.gc.db.app.discordHandler.client.getRestClient().getChannelService();
	}

	@Override
	public void delete(@Nullable String reason) {
		guild.discordJS.checkReadOnly();
		getChannelService().deleteChannel(id.asLong(), null).block();
	}

	public void setName(String s) {
		guild.discordJS.checkReadOnly();
		getChannelService().modifyChannel(id.asLong(), ChannelModifyRequest.builder().name(s).build(), null).block();
		name = s;
	}

	public void setTopic(String s) {
		guild.discordJS.checkReadOnly();
		getChannelService().modifyChannel(id.asLong(), ChannelModifyRequest.builder().topic(s).build(), null).block();
		topic = s;
	}

	public void setNsfw(boolean b) {
		guild.discordJS.checkReadOnly();
		getChannelService().modifyChannel(id.asLong(), ChannelModifyRequest.builder().nsfw(b).build(), null).block();
		nsfw = b;
	}

	public String getUrl() {
		return "https://discord.com/channels/" + guild.id + "/" + id;
	}

	public WrappedMessage getMessage(Message w) {
		return new WrappedMessage(this, w);
	}

	@Nullable
	public WrappedMessage findMessage(WrappedId messageId) {
		try {
			return getMessage(new Message(guild.gc.db.app.discordHandler.client, getChannelService().getMessage(id.asLong(), messageId.asLong()).block()));
		} catch (Exception ex) {
			return null;
		}
	}

	public WrappedId send(MessageBuilder content) {
		guild.discordJS.checkReadOnly();
		return new WrappedId(getChannelService().createMessage(id.asLong(), content.toMultipartMessageCreateRequest()).block().id());
	}
}
