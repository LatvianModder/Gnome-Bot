package dev.gnomebot.app.script;

import dev.gnomebot.app.App;
import dev.gnomebot.app.util.SnowFlake;
import dev.latvian.mods.rhino.util.HideFromJS;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.BanQuerySpec;
import discord4j.core.spec.GuildMemberEditSpec;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.Objects;

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
		return Objects.requireNonNull(guild.gc.getMember(id.asLong()));
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
		var i = getDiscordMember().getJoinTime().orElse(null);
		return i == null ? null : Date.from(i);
	}

	public boolean hasRoles() {
		return !getDiscordMember().getMemberData().roles().isEmpty();
	}

	public boolean hasRole(WrappedId id) {
		return getDiscordMember().getRoleIds().contains(SnowFlake.convert(id.asLong()));
	}

	public WrappedRole[] getRoles() {
		var list = getDiscordMember().getRoleIds();
		var array = new WrappedRole[list.size()];

		var i = 0;
		for (var s : list) {
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

	public boolean addRole(WrappedId roleId, @Nullable String reason) {
		guild.discordJS.checkReadOnly();
		var role = guild.gc.getRoleMap().get(roleId.asLong());

		if (role != null) {
			return role.add(id.asLong(), reason);
		} else {
			App.warn("Unknown role " + roleId.asString());
			return false;
		}
	}

	public boolean addRole(WrappedId id) {
		return addRole(id, null);
	}

	public boolean removeRole(WrappedId roleId, @Nullable String reason) {
		guild.discordJS.checkReadOnly();
		var role = guild.gc.getRoleMap().get(roleId.asLong());

		if (role != null) {
			return role.remove(id.asLong(), reason);
		} else {
			App.warn("Unknown role " + roleId.asString());
			return false;
		}
	}

	public boolean removeRole(WrappedId id) {
		return removeRole(id, null);
	}

	public boolean toggleRole(WrappedId id, @Nullable String reason) {
		if (getDiscordMember().getRoleIds().contains(SnowFlake.convert(id.asLong()))) {
			return removeRole(id, reason);
		} else {
			return addRole(id, reason);
		}
	}

	public boolean toggleRole(WrappedId id) {
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

	public void move(WrappedId channel) {
		guild.discordJS.checkReadOnly();
		getDiscordMember().edit(GuildMemberEditSpec.builder().newVoiceChannelOrNull(SnowFlake.convert(channel.asLong())).build()).block();
	}

	public void disconnect() {
		guild.discordJS.checkReadOnly();
		getDiscordMember().edit(GuildMemberEditSpec.builder().newVoiceChannelOrNull(null).build()).block();
	}

	public boolean hasVoiceConnection() {
		return getDiscordMember().getVoiceState().block().getChannelId().isPresent();
	}
}
