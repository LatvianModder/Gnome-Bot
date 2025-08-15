package dev.gnomebot.app.data.channel;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.LazyOptional;
import dev.gnomebot.app.discord.WebHookDestination;
import dev.gnomebot.app.util.SnowFlake;
import dev.latvian.apps.ansi.log.Log;
import dev.latvian.apps.webutils.data.Lazy;
import discord4j.core.object.entity.channel.CategorizableChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.TopLevelGuildMessageChannel;
import discord4j.core.spec.WebhookCreateSpec;
import discord4j.rest.util.PermissionSet;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TopLevelChannelInfo extends ChannelInfo {
	private final LazyOptional<CategorizableChannel> topLevelChannel;
	private final LazyOptional<WebHookDestination> webHook;
	private Map<Long, Permissions> permissionOverrides;
	private final Lazy<Map<Long, CachedPermissions>> cachedPermissions;

	public TopLevelChannelInfo(GuildCollections g, long id, String name, ChannelSettings s) {
		super(g, id, name, s);

		this.topLevelChannel = LazyOptional.of(() -> {
			// data.type() == Channel.Type.GROUP_DM.getValue()

			try {
				var channel = gc.getGuild().getChannelById(SnowFlake.convert(id)).block();
				Log.info("Fetched channel " + id + ": " + channel);
				return channel instanceof CategorizableChannel c ? c : null;
			} catch (Exception ex) {
				try {
					return new TextChannel(gc.getClient(), getChannelData());
				} catch (Exception ex1) {
					return null;
				}
			}
		});

		this.webHook = LazyOptional.of(() -> {
			if (getTopLevelChannel() instanceof TopLevelGuildMessageChannel tlcMsg) {
				// App.info("Unknown webhook for " + getName() + "/" + id);

				var webhook = tlcMsg.getWebhooks().filter(w -> w.getToken().isPresent() && w.getCreator().map(u -> u.getId().asLong() == gc.db.app.discordHandler.selfId).orElse(false)).blockFirst();

				if (webhook == null) {
					Log.info("Webhook for " + this + " not found, creating");
					webhook = tlcMsg.createWebhook(WebhookCreateSpec.builder().name("Gnome").reason("Gnome Bot webhook").build()).block();
				} else {
					Log.info("Loaded webhook " + webhook.getId().asString() + " '" + webhook.getName().orElse("Unnamed") + "'");
				}

				if (webhook != null) {
					return new WebHookDestination(this, webhook);
				}
			}

			return null;
		});

		this.cachedPermissions = Lazy.of(HashMap::new);
	}

	@Override
	@Nullable
	public CategorizableChannel getTopLevelChannel() {
		return topLevelChannel.get();
	}

	public void refreshPermissions() {
		cachedPermissions.invalidate();
	}

	@Override
	public Map<Long, Permissions> getPermissionOverrides() {
		if (permissionOverrides == null) {
			var map = new HashMap<Long, Permissions>();

			try {
				for (var o : getChannelData().permissionOverwrites().toOptional().orElse(List.of())) {
					var pmap = new EnumMap<>(Permissions.NONE.map());

					for (var p : PermissionSet.of(o.allow())) {
						pmap.put(p, Boolean.TRUE);
					}

					for (var p : PermissionSet.of(o.deny())) {
						pmap.put(p, Boolean.FALSE);
					}

					map.put(o.id().asLong(), new Permissions(pmap, o.id().asLong()));
				}
			} catch (Exception ex) {
			}

			permissionOverrides = map;
			Log.debug("Overrides for " + this + ": " + map);
			return map;
		}

		return permissionOverrides;
	}

	@Override
	public CachedPermissions getPermissions(long member) {
		var c = cachedPermissions.getNonnull();
		var set = c.get(member);

		if (set == null) {
			set = new CachedPermissions(Permissions.compute(gc, this, member).asSet().getRawValue());
			c.put(member, set);
		}

		return set;
	}

	@Override
	public Optional<WebHookDestination> getWebHook() {
		return webHook.getOptional();
	}

	public ThreadChannelInfo thread(long threadId, String name) {
		return new ThreadChannelInfo(this, threadId, name);
	}
}
