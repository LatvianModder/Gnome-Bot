package dev.gnomebot.app.script;

import dev.gnomebot.app.data.DiscordMessage;
import dev.latvian.mods.rhino.util.HideFromJS;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ChannelModifyRequest;
import discord4j.rest.service.ChannelService;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class WrappedChannel implements WithId, Deletable {
	public final WrappedGuild guild;
	public final WrappedId id;
	private String name;
	private String topic;
	private Boolean nsfw;

	public WrappedChannel(WrappedGuild g, WrappedId i) {
		guild = g;
		id = i;
		name = null;
		topic = null;
		nsfw = null;
	}

	@Override
	public WrappedId id() {
		return id;
	}

	public String getMention() {
		return "<#" + id.asString() + ">";
	}

	public String getName() {
		if (name == null) {
			try {
				name = guild.guild.getChannelMap().get(id.asSnowflake()).getName();
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
				topic = guild.guild.getChannelMap().get(id.asSnowflake()).getTopic().orElse("");
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
				nsfw = guild.guild.getChannelMap().get(id.asSnowflake()).isNsfw();
			} catch (Exception ex) {
				ex.printStackTrace();
				nsfw = false;
			}
		}

		return nsfw;
	}

	@Override
	public void delete(@Nullable String reason) {
		getChannelService().deleteChannel(id.asLong(), null).block();
	}

	@HideFromJS
	public ChannelService getChannelService() {
		return guild.guild.db.app.discordHandler.client.getRestClient().getChannelService();
	}

	public void setName(String s) {
		getChannelService().modifyChannel(id.asLong(), ChannelModifyRequest.builder().name(s).build(), null).block();
		name = s;
	}

	public void setTopic(String s) {
		getChannelService().modifyChannel(id.asLong(), ChannelModifyRequest.builder().topic(s).build(), null).block();
		topic = s;
	}

	public void setNsfw(boolean b) {
		getChannelService().modifyChannel(id.asLong(), ChannelModifyRequest.builder().nsfw(b).build(), null).block();
		nsfw = b;
	}

	public String getUrl() {
		return "https://discord.com/channels/" + guild.id.asString() + "/" + id.asString();
	}

	public WrappedMessage getMessage(Message w) {
		return new WrappedMessage(this, w);
	}

	@Nullable
	public WrappedMessage findMessage(Snowflake messageId) {
		try {
			return getMessage(new Message(guild.guild.db.app.discordHandler.client, getChannelService().getMessage(id.asLong(), messageId.asLong()).block()));
		} catch (Exception ex) {
			return null;
		}
	}

	public WrappedId sendMessage(String content) {
		return new WrappedId(getChannelService().createMessage(id.asLong(), MessageCreateSpec.builder()
				.content(content)
				.allowedMentions(DiscordMessage.noMentions())
				.build()
				.asRequest()
		).block().id());
	}

	public WrappedId sendEmbed(Consumer<EmbedCreateSpec.Builder> embed) {
		EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
		embed.accept(builder);

		return new WrappedId(getChannelService().createMessage(id.asLong(), MessageCreateSpec.builder()
				.addEmbed(builder.build())
				.allowedMentions(DiscordMessage.noMentions())
				.build()
				.asRequest()
		).block().id());
	}
}
