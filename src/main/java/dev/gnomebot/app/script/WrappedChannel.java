package dev.gnomebot.app.script;

import dev.gnomebot.app.data.ChannelInfo;
import dev.latvian.mods.rhino.util.HideFromJS;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.ChannelModifyRequest;
import discord4j.rest.service.ChannelService;
import org.jetbrains.annotations.Nullable;

public class WrappedChannel implements WithId, Deletable {
	public final WrappedGuild guild;
	public final WrappedId id;
	private boolean valid;
	private String name;
	private String topic;
	private boolean nsfw;

	public WrappedChannel(WrappedGuild g, WrappedId i) {
		guild = g;
		id = i;
		valid = false;
		name = "unknown";
		topic = "";
		nsfw = false;
	}

	@Override
	public WrappedId id() {
		return id;
	}

	public String getMention() {
		return "<#" + id.asString + ">";
	}

	public boolean isValid() {
		return valid;
	}

	public String getName() {
		return name;
	}

	public String getTopic() {
		return topic;
	}

	public boolean getNsfw() {
		return nsfw;
	}

	@Override
	public void delete(@Nullable String reason) {
		getChannelService().deleteChannel(id.asLong, null).block();
	}

	public void update(@Nullable ChannelInfo c) {
		valid = c != null;
		name = c == null ? "unknown" : c.getName();
		topic = c == null ? "" : c.getTopic().orElse("");
		nsfw = c != null && c.isNsfw();
	}

	@HideFromJS
	public ChannelService getChannelService() {
		return guild.guild.db.app.discordHandler.client.getRestClient().getChannelService();
	}

	public void setName(String s) {
		getChannelService().modifyChannel(id.asLong, ChannelModifyRequest.builder().name(s).build(), null).block();
		name = s;
	}

	public void setTopic(String s) {
		getChannelService().modifyChannel(id.asLong, ChannelModifyRequest.builder().topic(s).build(), null).block();
		topic = s;
	}

	public void setNsfw(boolean b) {
		getChannelService().modifyChannel(id.asLong, ChannelModifyRequest.builder().nsfw(b).build(), null).block();
		nsfw = b;
	}

	public WrappedMessage getMessage(Message w) {
		return new WrappedMessage(this, w);
	}
}
