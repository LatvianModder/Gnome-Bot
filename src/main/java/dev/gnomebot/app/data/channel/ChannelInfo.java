package dev.gnomebot.app.data.channel;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.WebHookDestination;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.SnowFlake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.CategorizableChannel;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.discordjson.Id;
import discord4j.discordjson.json.ChannelData;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.PaginationUtil;
import discord4j.rest.util.Permission;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public abstract class ChannelInfo {
	public final GuildCollections gc;
	public final long id;
	public final String name;
	public final ChannelSettings settings;

	private RestChannel rest;
	private ChannelData channelData;

	public ChannelInfo(GuildCollections g, long id, String name, ChannelSettings settings) {
		this.gc = g;
		this.id = id;
		this.name = name;
		this.settings = settings;
	}

	public boolean isThread() {
		return false;
	}

	public long getTopId() {
		return id;
	}

	public int getXp() {
		return settings.xp >= 0 ? settings.xp : gc.globalXp.get();
	}

	public boolean isXpSet() {
		return getXp() > 0;
	}

	public RestChannel getRest() {
		if (rest == null) {
			rest = RestChannel.create(gc.getClient().getRestClient(), SnowFlake.convert(id));
		}

		return rest;
	}

	public ChannelData getChannelData() {
		if (channelData == null) {
			channelData = gc.getClient().getRestClient().getChannelService().getChannel(id).block();
		}

		return channelData;
	}

	public abstract Map<Long, Permissions> getPermissionOverrides();

	@Nullable
	public abstract CategorizableChannel getTopLevelChannel();

	@Nullable
	public Message getMessage(long messageId) {
		try {
			return gc.getClient().getMessageById(SnowFlake.convert(id), SnowFlake.convert(messageId)).block();
		} catch (Exception ex) {
			return null;
		}
	}

	@Nullable
	public Message getUncachedMessage(long messageId) {
		try {
			return gc.getClient().withRetrievalStrategy(EntityRetrievalStrategy.REST).getMessageById(SnowFlake.convert(id), SnowFlake.convert(messageId)).block();
		} catch (Exception ex) {
			return null;
		}
	}

	public Flux<Message> getMessagesBefore(long messageId) {
		final Function<Map<String, Object>, Flux<MessageData>> doRequest = params -> gc.getClient().getRestClient().getChannelService().getMessages(id, params);
		return PaginationUtil.paginateBefore(doRequest, data -> data.id().asLong(), messageId, 100).map(data -> new Message(gc.getClient(), data));
	}

	public Flux<Message> getMessagesAfter(long messageId) {
		final Function<Map<String, Object>, Flux<MessageData>> doRequest = params -> gc.getClient().getRestClient().getChannelService().getMessages(id, params);
		return PaginationUtil.paginateAfter(doRequest, data -> data.id().asLong(), messageId, 100).map(data -> new Message(gc.getClient(), data));
	}

	public long getLastMessageId() {
		return Possible.flatOpt(getChannelData().lastMessageId()).map(Id::asLong).orElse(0L);
	}

	public Mono<Message> createMessage(MessageBuilder builder) {
		return Mono.defer(() -> getRest().createMessage(builder.toMultipartMessageCreateRequest())).map(data -> new Message(gc.getClient(), data));
	}

	public Mono<Message> createMessage(String content) {
		return createMessage(MessageBuilder.create(content));
	}

	public Mono<Message> createMessage(EmbedBuilder embed) {
		return createMessage(MessageBuilder.create(embed));
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return id + "/" + name;
	}

	@Override
	public boolean equals(Object o) {
		return o == this || o instanceof ChannelInfo i && id == i.id;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(id);
	}

	public String getMention() {
		return "<#" + SnowFlake.str(id) + ">";
	}

	public abstract CachedPermissions getPermissions(long member);

	public CachedPermissions getSelfPermissions() {
		return getPermissions(gc.db.app.discordHandler.selfId);
	}

	public boolean checkPermissions(long memberId, Permission... permissions) {
		return permissions.length > 0 && getPermissions(memberId).has(permissions);
	}

	public boolean checkPermissions(long memberId, Permission permission) {
		return getPermissions(memberId).has(permission);
	}

	public boolean canViewChannel(long memberId) {
		return checkPermissions(memberId, Permission.VIEW_CHANNEL);
	}

	public abstract Optional<WebHookDestination> getWebHook();

	public Optional<String> getTopic() {
		return Possible.flatOpt(getChannelData().topic());
	}

	public boolean isNsfw() {
		return getChannelData().nsfw().toOptional().orElse(false);
	}
}