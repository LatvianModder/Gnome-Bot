package dev.gnomebot.app.data;

import dev.gnomebot.app.discord.WebHook;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Webhook;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.TopLevelGuildMessageChannel;
import discord4j.core.spec.WebhookCreateSpec;
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

/**
 * @author LatvianModder
 */
public class ChannelInfo extends WrappedDocument<ChannelInfo> {
	public final GuildCollections gc;
	public final Snowflake id;
	public String name;
	public ChannelInfo threadParent;

	public long xp;
	public long totalMessages;
	public long totalXp;
	public boolean autoThread;
	public boolean autoUpvote;

	private final LazyOptional<RestChannel> rest;
	private final LazyOptional<ChannelData> channelData;
	private final LazyOptional<TopLevelGuildMessageChannel> topLevelChannel;
	private final LazyOptional<WebHook> webHook;
	private Map<Snowflake, PermissionSet> cachedPermissions;

	public ChannelInfo(GuildCollections g, WrappedCollection<ChannelInfo> c, MapWrapper d, @Nullable Snowflake _id) {
		super(c, d);
		gc = g;
		id = _id == null ? Snowflake.of(document.getLong("_id")) : _id;
		name = "";
		threadParent = null;
		xp = document.getLong("xp");
		totalMessages = document.getLong("total_messages");
		totalXp = document.getLong("total_xp");
		autoThread = document.getBoolean("auto_thread");
		autoUpvote = document.getBoolean("auto_upvote");

		rest = LazyOptional.of(() -> RestChannel.create(gc.getClient().getRestClient(), id));
		channelData = LazyOptional.of(() -> gc.getClient().getRestClient().getChannelService().getChannel(id.asLong()).block());
		topLevelChannel = LazyOptional.of(() -> {
			if (threadParent != null) {
				return threadParent.getTopLevelChannel();
			}

			try {
				GuildChannel channel = gc.getGuild().getChannelById(id).block();
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
			TopLevelGuildMessageChannel tlc = getTopLevelChannel();

			if (tlc != null) {
				Webhook webhook = tlc.getWebhooks().filter(w -> w.getToken().isPresent() && w.getCreator().map(u -> u.getId().equals(gc.db.app.discordHandler.selfId)).orElse(false)).blockFirst();

				if (webhook == null) {
					webhook = tlc.createWebhook(WebhookCreateSpec.builder().name("Gnome").reason("Gnome Bot webhook").build()).block();
				}

				if (webhook != null) {
					return new WebHook(this, webhook);
				}
			}

			return null;
		});
	}

	public ChannelInfo thread(Snowflake threadId, String name) {
		ChannelInfo ci = new ChannelInfo(gc, collection, MapWrapper.EMPTY, threadId);
		ci.threadParent = this;
		ci.name = name;
		ci.xp = xp; // TODO: Add config for thread xp but for now just re-use parent channel xp
		ci.totalMessages = 0L;
		ci.totalXp = 0L;
		ci.autoThread = false;
		ci.autoUpvote = false;
		return ci;
	}

	public Snowflake getTopId() {
		return threadParent == null ? id : threadParent.getTopId();
	}

	public void updateFrom(GuildChannel ch) {
		name = ch.getName();

		if (!document.getString("name", "").equals(name)) {
			update("name", name);
		}
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
	public Message getMessage(Snowflake messageId) {
		try {
			return gc.getClient().getMessageById(id, messageId).block();
		} catch (Exception ex) {
			return null;
		}
	}

	public Flux<Message> getMessagesBefore(final Snowflake messageId) {
		final Function<Map<String, Object>, Flux<MessageData>> doRequest = params -> gc.getClient().getRestClient().getChannelService().getMessages(id.asLong(), params);
		return PaginationUtil.paginateBefore(doRequest, data -> Snowflake.asLong(data.id()), messageId.asLong(), 100).map(data -> new Message(gc.getClient(), data));
	}

	public Flux<Message> getMessagesAfter(final Snowflake messageId) {
		final Function<Map<String, Object>, Flux<MessageData>> doRequest = params -> gc.getClient().getRestClient().getChannelService().getMessages(id.asLong(), params);
		return PaginationUtil.paginateAfter(doRequest, data -> Snowflake.asLong(data.id()), messageId.asLong(), 100).map(data -> new Message(gc.getClient(), data));
	}

	@Nullable
	public Snowflake getLastMessageId() {
		return Possible.flatOpt(getChannelData().lastMessageId()).map(Snowflake::of).orElse(null);
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

	@Override
	public String getName() {
		if (name.isEmpty()) {
			name = document.getString("name", "");

			if (name.isEmpty()) {
				ChannelData data = getChannelData();

				if (data != null) {
					name = data.name().toOptional().orElse("deleted-channel");
				} else {
					name = "deleted-channel";
				}
			}
		}

		return name;
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof ChannelInfo && (o == this || id.equals(((ChannelInfo) o).id));
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	public void refreshCache() {
		name = "";
		rest.invalidate();
		channelData.invalidate();
		topLevelChannel.invalidate();
		cachedPermissions = null;
	}

	public String getMention() {
		return "<#" + id.asString() + ">";
	}

	public PermissionSet getSelfPermissions() {
		return getPermissions(gc.db.app.discordHandler.selfId);
	}

	public PermissionSet getPermissions(Snowflake member) {
		if (cachedPermissions == null) {
			cachedPermissions = new HashMap<>();
		}

		PermissionSet set = cachedPermissions.get(member);

		if (set == null) {
			set = Utils.getEffectivePermissions(getTopLevelChannel(), member);
			cachedPermissions.put(member, set);
		}

		return set;
	}

	public boolean checkPermissions(Snowflake memberId, Permission... permissions) {
		if (permissions.length == 0) {
			return true;
		}

		PermissionSet perms = getPermissions(memberId);

		for (Permission p : permissions) {
			if (!perms.contains(p)) {
				return false;
			}
		}

		return true;
	}

	public boolean checkPermissions(Snowflake memberId, Permission permission) {
		return getPermissions(memberId).contains(permission);
	}

	public boolean canViewChannel(Snowflake memberId) {
		return checkPermissions(memberId, Permission.VIEW_CHANNEL);
	}

	public Optional<WebHook> getWebHook() {
		if (threadParent != null) {
			return threadParent.getWebHook().map(webHook1 -> webHook1.withThread(this, id.asString()));
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