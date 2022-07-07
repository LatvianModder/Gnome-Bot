package dev.gnomebot.app.script;

import dev.gnomebot.app.App;
import dev.gnomebot.app.discord.CachedRole;
import dev.latvian.mods.rhino.util.HideFromJS;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.BanQuerySpec;
import discord4j.core.spec.GuildMemberEditSpec;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

public class WrappedMember extends WrappedUser {
	private String nickname;

	public WrappedMember(WrappedId id, WrappedGuild g) {
		super(id, g);
	}

	@Override
	public void clearCache() {
		super.clearCache();
		nickname = null;
	}

	@Override
	public boolean isMember() {
		return true;
	}

	@HideFromJS
	public Member getDiscordMember() {
		return Objects.requireNonNull(guild.gc.getMember(id.asSnowflake()));
	}

	public String getNickname() {
		if (nickname == null) {
			nickname = getDiscordMember().getNickname().orElse("");
		}

		return nickname;
	}

	public String getDisplayName() {
		if (getNickname().isEmpty()) {
			return getName();
		}

		return getNickname();
	}

	@Nullable
	public Date getDateJoined() {
		Instant i = getDiscordMember().getJoinTime().orElse(null);
		return i == null ? null : Date.from(i);
	}

	public boolean hasRoles() {
		return !getDiscordMember().getMemberData().roles().isEmpty();
	}

	public boolean hasRole(Snowflake id) {
		return getDiscordMember().getRoleIds().contains(id);
	}

	public WrappedRole[] getRoles() {
		Set<Snowflake> list = getDiscordMember().getRoleIds();
		WrappedRole[] array = new WrappedRole[list.size()];

		int i = 0;
		for (Snowflake s : list) {
			array[i] = guild.roles.get(s.asString());
			i++;
		}

		return array;
	}

	public boolean isPending() {
		return getDiscordMember().isPending();
	}

	public boolean isDeafened() {
		return getDiscordMember().getMemberData().deaf();
	}

	public boolean isMuted() {
		return getDiscordMember().getMemberData().mute();
	}

	public void kick(@Nullable String reason) {
		guild.discordJS.checkReadOnly();
		getDiscordMember().kick(reason == null || reason.isBlank() ? null : reason).block();
	}

	public void ban(@Nullable String reason, boolean deleteMessages) {
		guild.discordJS.checkReadOnly();
		getDiscordMember().ban(BanQuerySpec.builder().reason(reason == null || reason.isBlank() ? null : reason).deleteMessageDays(deleteMessages ? 1 : null).build()).block();
	}

	public boolean addRole(Snowflake roleId, @Nullable String reason) {
		guild.discordJS.checkReadOnly();
		CachedRole role = guild.gc.getRoleMap().get(roleId);

		if (role != null) {
			return role.add(id.asSnowflake(), reason);
		} else {
			App.warn("Unknown role " + roleId.asString());
			return false;
		}
	}

	public boolean addRole(Snowflake id) {
		return addRole(id, null);
	}

	public boolean removeRole(Snowflake roleId, @Nullable String reason) {
		guild.discordJS.checkReadOnly();
		CachedRole role = guild.gc.getRoleMap().get(roleId);

		if (role != null) {
			return role.remove(id.asSnowflake(), reason);
		} else {
			App.warn("Unknown role " + roleId.asString());
			return false;
		}
	}

	public boolean removeRole(Snowflake id) {
		return removeRole(id, null);
	}

	public boolean toggleRole(Snowflake id, @Nullable String reason) {
		if (getDiscordMember().getRoleIds().contains(id)) {
			return removeRole(id, reason);
		} else {
			return addRole(id, reason);
		}
	}

	public boolean toggleRole(Snowflake id) {
		return toggleRole(id, null);
	}

	public void setNickname(String n) {
		guild.discordJS.checkReadOnly();
		nickname = n == null ? "" : n;
		getDiscordMember().edit(GuildMemberEditSpec.builder().nicknameOrNull(nickname.isBlank() ? null : nickname).build()).block();
	}

	public void setDeafened(boolean b) {
		guild.discordJS.checkReadOnly();
		getDiscordMember().edit(GuildMemberEditSpec.builder().deafen(b).build()).block();
	}

	public void setMuted(boolean b) {
		guild.discordJS.checkReadOnly();
		getDiscordMember().edit(GuildMemberEditSpec.builder().mute(b).build()).block();
	}

	public void move(Snowflake channel) {
		guild.discordJS.checkReadOnly();
		getDiscordMember().edit(GuildMemberEditSpec.builder().newVoiceChannelOrNull(channel).build()).block();
	}

	public void disconnect() {
		guild.discordJS.checkReadOnly();
		getDiscordMember().edit(GuildMemberEditSpec.builder().newVoiceChannelOrNull(null).build()).block();
	}

	public boolean hasVoiceConnection() {
		return getDiscordMember().getVoiceState().block().getChannelId().isPresent();
	}
}
