package dev.gnomebot.app.data;

import dev.gnomebot.app.discord.WebHook;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.ansi.Log;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.TopLevelGuildMessageChannel;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.spec.WebhookCreateSpec;
import discord4j.discordjson.Id;
import discord4j.discordjson.json.ChannelData;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.PaginationUtil;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class ChannelInfo {
	public final GuildCollections gc;
	public final long id;
	public final ChannelSettings settings;
	public String threadName;
	public ChannelInfo threadParent;

	private final LazyOptional<RestChannel> rest;
	private final LazyOptional<ChannelData> channelData;
	private final LazyOptional<TopLevelGuildMessageChannel> topLevelChannel;
	private final LazyOptional<WebHook> webHook;
	private Map<Long, PermissionSet> cachedPermissions;

	public ChannelInfo(GuildCollections g, long _id, ChannelSettings s) {
		gc = g;
		id = _id;
		settings = s;
		threadName = null;
		threadParent = null;

		rest = LazyOptional.of(() -> RestChannel.create(gc.getClient().getRestClient(), SnowFlake.convert(id)));
		channelData = LazyOptional.of(() -> gc.getClient().getRestClient().getChannelService().getChannel(id).block());
		topLevelChannel = LazyOptional.of(() -> {
			if (threadParent != null) {
				return threadParent.getTopLevelChannel();
			}

			try {
				var channel = gc.getGuild().getChannelById(SnowFlake.convert(id)).block();
				return channel instanceof TopLevelGuildMessageChannel ? (TopLevelGuildMessageChannel) channel : null;
			} catch (Exception ex) {
				try {
					return new TextChannel(gc.getClient(), getChannelData());
				} catch (Exception ex1) {
					return null;
				}
			}
		});
		webHook = LazyOptional.of(() -> {
			var tlc = getTopLevelChannel();

			if (tlc != null) {
				// App.info("Unknown webhook for " + getName() + "/" + id);

				var webhook = tlc.getWebhooks().filter(w -> w.getToken().isPresent() && w.getCreator().map(u -> u.getId().asLong() == gc.db.app.discordHandler.selfId).orElse(false)).blockFirst();

				if (webhook == null) {
					Log.info("Webhook for " + this + " not found, creating");
					webhook = tlc.createWebhook(WebhookCreateSpec.builder().name("Gnome").reason("Gnome Bot webhook").build()).block();
				} else {
					Log.info("Loaded webhook " + webhook.getId().asString() + " '" + webhook.getName().orElse("Unnamed") + "'");
				}

				if (webhook != null) {
					return new WebHook(this, webhook);
				}
			}

			return null;
		});
	}

	public ChannelInfo thread(long threadId, String name) {
		var ci = new ChannelInfo(gc, threadId, settings);
		ci.threadParent = this;
		ci.threadName = name;
		return ci;
	}

	public long getTopId() {
		return threadParent == null ? id : threadParent.getTopId();
	}

	public int getXp() {
		if (threadParent != null) {
			return settings.threadXp >= 0 ? settings.threadXp : threadParent.getXp();
		} else {
			return settings.xp >= 0 ? settings.xp : gc.globalXp.get();
		}
	}

	public boolean isXpSet() {
		return getXp() > 0;
	}

	public RestChannel getRest() {
		return rest.get();
	}

	public ChannelData getChannelData() {
		return channelData.get();
	}

	@Nullable
	public TopLevelGuildMessageChannel getTopLevelChannel() {
		return topLevelChannel.get();
	}

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
		return threadName == null ? settings.getName() : threadName;
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public boolean equals(Object o) {
		return o == this || o instanceof ChannelInfo i && id == i.id;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(id);
	}

	public void refreshCache() {
		rest.invalidate();
		channelData.invalidate();
		topLevelChannel.invalidate();
		cachedPermissions = null;
	}

	public String getMention() {
		return "<#" + SnowFlake.str(id) + ">";
	}

	public PermissionSet getSelfPermissions() {
		return getPermissions(gc.db.app.discordHandler.selfId);
	}

	public PermissionSet getPermissions(long member) {
		if (threadParent != null) {
			return threadParent.getPermissions(member);
		}

		if (cachedPermissions == null) {
			cachedPermissions = new HashMap<>();
		}

		var set = cachedPermissions.get(member);

		if (set == null) {
			set = Utils.getEffectivePermissions(getTopLevelChannel(), member);
			cachedPermissions.put(member, set);
		}

		return set;
	}

	public boolean checkPermissions(long memberId, Permission... permissions) {
		if (permissions.length == 0) {
			return true;
		}

		var perms = getPermissions(memberId);

		for (var p : permissions) {
			if (!perms.contains(p)) {
				return false;
			}
		}

		return true;
	}

	public boolean checkPermissions(long memberId, Permission permission) {
		return getPermissions(memberId).contains(permission);
	}

	public boolean canViewChannel(long memberId) {
		return checkPermissions(memberId, Permission.VIEW_CHANNEL);
	}

	public Optional<WebHook> getWebHook() {
		if (threadParent != null) {
			return threadParent.getWebHook().map(w -> w.withThread(this, id));
		}

		return webHook.getOptional();
	}

	public Optional<String> getTopic() {
		return Possible.flatOpt(getChannelData().topic());
	}

	public boolean isNsfw() {
		return getChannelData().nsfw().toOptional().orElse(false);
	}
}