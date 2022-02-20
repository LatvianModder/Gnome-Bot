package dev.gnomebot.app.data;

import dev.gnomebot.app.discord.WebHook;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
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
import discord4j.rest.util.Image;
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
	public boolean thread;

	public long xp;
	public long totalMessages;
	public long totalXp;
	public boolean autoThread;
	public boolean autoUpvote;

	private final LazyOptional<RestChannel> rest;
	private final LazyOptional<ChannelData> channelData;
	private final LazyOptional<TopLevelGuildMessageChannel> channel;
	private Map<Snowflake, PermissionSet> cachedPermissions;

	public ChannelInfo(GuildCollections g, WrappedCollection<ChannelInfo> c, MapWrapper d, @Nullable Snowflake _id) {
		super(c, d);
		gc = g;
		id = _id == null ? Snowflake.of(document.getLong("_id")) : _id;
		name = "";
		thread = false;
		xp = document.getLong("xp");
		totalMessages = document.getLong("total_messages");
		totalXp = document.getLong("total_xp");
		autoThread = document.getBoolean("auto_thread");
		autoUpvote = document.getBoolean("auto_upvote");

		rest = LazyOptional.of(() -> RestChannel.create(gc.getClient().getRestClient(), id));
		channelData = LazyOptional.of(() -> gc.getClient().getRestClient().getChannelService().getChannel(id.asLong()).block());
		channel = LazyOptional.of(() -> {
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
	public TopLevelGuildMessageChannel getChannel() {
		return channel.get();
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
		channel.invalidate();
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
			set = Utils.getEffectivePermissions(getChannel(), member);
			cachedPermissions.put(member, set);
		}

		return set;
	}

	public boolean checkPermissions(Member member, Permission... permissions) {
		if (permissions.length == 0) {
			return true;
		}

		PermissionSet perms = getPermissions(member.getId());

		for (Permission p : permissions) {
			if (!perms.contains(p)) {
				return true;
			}
		}

		return true;
	}

	public boolean checkPermissions(Snowflake memberId, Permission... permissions) {
		if (permissions.length == 0) {
			return true;
		}

		Member member = gc.getMember(memberId);
		return member != null && checkPermissions(member, permissions);
	}

	public Optional<WebHook> getOrCreateWebhook() {
		TopLevelGuildMessageChannel c = getChannel();

		if (c != null) {
			try {
				Webhook webhook = c.getWebhooks().filter(w -> w.getToken().isPresent() && w.getCreator().map(u -> u.getId().equals(gc.db.app.discordHandler.selfId)).orElse(false)).blockFirst();

				if (webhook == null) {
					webhook = c.createWebhook(WebhookCreateSpec.builder()
							.avatarOrNull(gc.getGuild().getIcon(Image.Format.PNG).block())
							.name(gc.toString())
							.reason("Gnome webhook")
							.build()
					).block();
				}

				if (webhook != null) {
					return Optional.of(new WebHook(webhook));
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return Optional.empty();
	}

	public Optional<String> getTopic() {
		return Possible.flatOpt(getChannelData().topic());
	}

	public boolean isNsfw() {
		return getChannelData().nsfw().toOptional().orElse(false);
	}
}