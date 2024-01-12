package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.App;
import dev.gnomebot.app.Config;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.discord.EmbedColor;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.json.JSON;
import dev.latvian.apps.webutils.json.JSONArray;
import dev.latvian.apps.webutils.json.JSONObject;
import dev.latvian.apps.webutils.json.JSONResponse;
import dev.latvian.apps.webutils.net.FileResponse;
import dev.latvian.apps.webutils.net.Response;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import io.javalin.http.BadRequestResponse;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MinecraftHandlers {
	public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5L))
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

	private static final class VerifyData implements Runnable {
		private final GuildCollections gc;
		private final String token;
		private final Member user;
		private final Instant expires;
		private Message message;
		private ComponentEventWrapper event;
		private int stage;

		public VerifyData(GuildCollections gc, String token, Member user) {
			this.gc = gc;
			this.token = token;
			this.user = user;
			this.expires = Instant.now().plusSeconds(900);
			this.stage = 0;
		}

		public EmbedBuilder waiting() {
			var thread = new Thread(this);
			thread.setDaemon(true);
			thread.start();

			return EmbedBuilder.create("Verify Minecraft", "Waiting for " + user.getMention() + " to start verification...")
					.color(EmbedColor.RED)
					.field("User", user.getMention())
					.field("Link expires", Utils.formatRelativeDate(expires));
		}

		public void started() {
			stage = 1;

			if (event != null) {
				event.event.deleteReply().subscribe();
				event = null;
			}

			App.warn(user.getUsername() + " started mc verification");

			message.edit().withEmbedsOrNull(List.of(
					EmbedBuilder.create("Verify Minecraft", "Waiting for " + user.getMention() + " to complete verification...")
							.color(EmbedColor.YELLOW)
							.field("User", user.getMention())
							.field("Link expires", Utils.formatRelativeDate(expires))
							.toEmbedCreateSpec()
			)).subscribe();
		}

		public void success(UUID uuid, String name) {
			stage = 2;

			App.success(user.getUsername() + " completed mc verification as " + name);

			message.edit().withEmbedsOrNull(List.of(
					EmbedBuilder.create("Verify Minecraft", "Success! Verified as [" + name + "](https://mcuuid.net/?q=" + uuid + ")")
							.color(EmbedColor.GREEN)
							.field("User", user.getMention())
							.thumbnail("https://crafatar.com/renders/head/" + uuid + "?overlay=true")
							.toEmbedCreateSpec()
			)).subscribe();

			gc.unmute(user.getId().asLong(), 0L, user.getMention() + " Verified Minecraft");

			if (gc.isMM()) {
				var role = gc.getRoleMap().get(1119549049717149808L);

				if (role != null) {
					role.add(user, "Verified Minecraft");
				}
			}
		}

		public void fail(String error) {
			stage = 3;

			App.error(user.getUsername() + " failed mc verification: " + error);

			message.edit().withEmbedsOrNull(List.of(
					EmbedBuilder.create("Verify Minecraft", error.startsWith("com.fasterxml.jackson") ? "Internal Error" : ("Error!\n\n" + error))
							.color(EmbedColor.RED)
							.field("User", user.getMention())
							.toEmbedCreateSpec()
			)).subscribe();
		}

		@Override
		public void run() {
			try {
				Thread.sleep(expires.toEpochMilli() - System.currentTimeMillis());
			} catch (Exception ex) {
			}

			if (stage < 2) {
				fail("Link Expired");
			}

			MAP.remove(token);
		}
	}

	private record BaseMinecraftProfile(UUID uuid, String name) {
	}

	public static final Map<String, VerifyData> MAP = new HashMap<>();

	@Nullable
	private static VerifyData checkButton(long user) {
		for (var t : MAP.values()) {
			if (t.user.getId().asLong() == user && t.stage < 3) {
				return t;
			}
		}

		return null;
	}

	public static final URI TOKEN_URI = URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/token");
	public static final URI XBOX_AUTH_URI = URI.create("https://user.auth.xboxlive.com/user/authenticate");
	public static final URI XSTS_AUTH_URI = URI.create("https://xsts.auth.xboxlive.com/xsts/authorize");
	public static final URI MC_XBOX_AUTH_URI = URI.create("https://api.minecraftservices.com/authentication/login_with_xbox");
	public static final URI MC_PROFILE_URI = URI.create("https://api.minecraftservices.com/minecraft/profile");

	public static void verifyCallback(ComponentEventWrapper event, long userId) {
		if (!event.context.sender.getId().equals(userId)) {
			event.respond("You can't verify account of someone else!");
			return;
		} else {
			var prev = checkButton(userId);

			if (prev != null) {
				event.respond("Finish verifying the [previously opened page](https://gnomebot.dev/minecraft/verify?state=" + prev.token + ")!");
				return;
			}
		}

		var data = new VerifyData(event.context.gc, Utils.createShortToken(), event.context.sender);
		data.message = event.context.channelInfo.createMessage(data.waiting()).block();
		data.event = event;

		event.respond("[Click this link](<https://gnomebot.dev/minecraft/verify?state=" + data.token + ">) to verify your Minecraft profile!" +
				"\nYou will have to log in with your Microsoft Account");

		MAP.put(data.token, data);
	}

	public static Response verify(ServerRequest request) {
		if (Config.get().microsoft_client_id.isEmpty() || Config.get().microsoft_client_secret.isEmpty()) {
			throw new BadRequestResponse("This bot does not have microsoft web app set up");
		}

		var stateStr = request.query("state").asString();

		if (stateStr.equals("success")) {
			return FileResponse.plainText("Success!\n\nYou can now close this tab.");
		} else if (stateStr.equals("error")) {
			return FileResponse.plainText("Failed to verify your Minecraft account! You can find more info on discord.\n\nYou can now close this tab.");
		}

		var code = request.query("code").asString();
		var data = MAP.get(stateStr);

		if (data == null || data.expires.isBefore(Instant.now())) {
			return FileResponse.plainText("Link expired! Please click Verify button again to generate a new link!");
		}

		if (code != null && !code.isEmpty()) {
			MAP.remove(data.token);

			try {
				var profile = verify1(code);
				data.success(profile.uuid, profile.name);
				return Response.redirect("/minecraft/verify?state=success");
			} catch (Exception ex) {
				data.fail(ex.getMessage());
				return Response.redirect("/minecraft/verify?state=error");
			}
		} else {
			data.started();
			return Response.redirect("https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?client_id=" + Config.get().microsoft_client_id + "&scope=XboxLive.signin&response_type=code&redirect_uri=https%3A%2F%2Fgnomebot.dev%2Fminecraft%2Fverify&prompt=select_account&state=" + data.token);
		}
	}

	private static JSONObject parseJson(String stage, HttpResponse<String> resp) {
		try {
			return JSON.DEFAULT.read(resp.body()).readObject();
		} catch (Exception ex) {
			throw new BadRequestResponse("Minecraft link error " + stage + ": " + resp.statusCode() + " " + resp.body());
		}
	}

	private static BaseMinecraftProfile verify1(String code) throws Exception {
		var request = HttpRequest.newBuilder(TOKEN_URI)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("Accept", "application/x-www-form-urlencoded")
				.POST(FileResponse.formData(Map.of(
						"client_id", Config.get().microsoft_client_id,
						"client_secret", Config.get().microsoft_client_secret,
						"code", code,
						"grant_type", "authorization_code",
						"redirect_uri", "https://gnomebot.dev/minecraft/verify",
						"scope", "xboxlive.signin"
				)).bodyPublisher()).build();

		var resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		var json = parseJson("A0", resp);

		if (resp.statusCode() >= 200 && resp.statusCode() < 400) {
			if (!json.containsKey("access_token")) {
				throw new BadRequestResponse("Minecraft link error A1: " + JSON.DEFAULT.writePretty(json));
			}

			return getMcProfile(json.asString("access_token"));
		}

		if (json.asString("error").equals("invalid_grant")) {
			throw new BadRequestResponse("Login took too long!");
		}

		throw new BadRequestResponse("Minecraft link error A2 " + resp.statusCode() + ": " + JSON.DEFAULT.writePretty(json));
	}

	private static BaseMinecraftProfile getMcProfile(String accessToken) throws Exception {
		var data = JSONObject.of();
		var props = JSONObject.of();
		props.put("AuthMethod", "RPS");
		props.put("SiteName", "user.auth.xboxlive.com");
		props.put("RpsTicket", "d=" + accessToken);
		data.put("Properties", props);
		data.put("RelyingParty", "http://auth.xboxlive.com");
		data.put("TokenType", "JWT");

		var request = HttpRequest.newBuilder(XBOX_AUTH_URI)
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.header("x-xbl-contract-version", "1")
				.POST(JSONResponse.of(data).bodyPublisher()).build();

		var resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		var json = parseJson("B0", resp);

		if (resp.statusCode() >= 200 && resp.statusCode() < 400) {
			if (!json.containsKey("Token")) {
				throw new BadRequestResponse("Minecraft link error B1: " + JSON.DEFAULT.writePretty(json));
			}

			var xblToken = json.asString("Token");
			return acquireXsts(xblToken);
		}

		throw new BadRequestResponse("Minecraft link error B2 " + resp.statusCode() + ": " + JSON.DEFAULT.writePretty(json));
	}

	private static BaseMinecraftProfile acquireXsts(String xblToken) throws Exception {
		var data = JSONObject.of();
		var props = JSONObject.of();
		var userTokens = JSONArray.of();
		userTokens.add(xblToken);
		props.put("SandboxId", "RETAIL");
		props.put("UserTokens", userTokens);
		data.put("Properties", props);
		data.put("RelyingParty", "rp://api.minecraftservices.com/");
		data.put("TokenType", "JWT");

		var request = HttpRequest.newBuilder(XSTS_AUTH_URI)
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.POST(JSONResponse.of(data).bodyPublisher()).build();

		var resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		var json = parseJson("C0", resp);

		if (resp.statusCode() >= 200 && resp.statusCode() < 400) {
			if (!json.containsKey("Token") || !json.containsKey("DisplayClaims")) {
				throw new BadRequestResponse("Minecraft link error C1: " + JSON.DEFAULT.writePretty(json));
			}

			var xblXsts = json.asString("Token");
			var claims = json.asObject("DisplayClaims");

			if (!claims.containsKey("xui")) {
				throw new BadRequestResponse("Minecraft link error C1: " + JSON.DEFAULT.writePretty(json));
			}

			var xui = claims.asArray("xui");

			if (xui.isEmpty() || !xui.asObject(0).containsKey("uhs")) {
				throw new BadRequestResponse("Minecraft link error C1: " + JSON.DEFAULT.writePretty(json));
			}

			var uhs = xui.asObject(0).asString("uhs");

			try {
				return acquireMinecraftToken(uhs, xblXsts);
			} catch (HttpTimeoutException ex) {
				throw new BadRequestResponse("Timeout while acquiring Minecraft token! It's possible that Minecraft auth servers are down, please try again in few hours!");
			}
		}

		var xerr = json.asString("XErr");

		if (!xerr.isEmpty()) {
			switch (xerr) {
				case "2148916233" -> throw new BadRequestResponse("Microsoft account does not have an Xbox account");
				case "2148916235" -> throw new BadRequestResponse("Accounts from countries where XBox Live is not available or banned");
				case "2148916236" -> throw new BadRequestResponse("You must complete adult verification on the XBox homepage");
				case "2148916237" -> throw new BadRequestResponse("Age verification must be completed on the XBox homepage");
				case "2148916238" -> throw new BadRequestResponse("The account is under the age of 18, an adult must add the account to the family. You may need to check your e-mail.");
				default -> throw new BadRequestResponse("Xbox XSTS Authentication Error! Code: " + xerr);
			}
		}

		throw new BadRequestResponse("Minecraft link error C2 " + resp.statusCode() + ": " + JSON.DEFAULT.writePretty(json));
	}

	private static BaseMinecraftProfile acquireMinecraftToken(String xblUhs, String xblXsts) throws Exception {
		var data = JSONObject.of();
		data.put("identityToken", "XBL3.0 x=" + xblUhs + ";" + xblXsts);

		var request = HttpRequest.newBuilder(MC_XBOX_AUTH_URI)
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.POST(JSONResponse.of(data).bodyPublisher()).build();

		var resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		var json = parseJson("D0", resp);

		if (resp.statusCode() >= 200 && resp.statusCode() < 400) {
			if (!json.containsKey("access_token")) {
				throw new BadRequestResponse("Minecraft link error D1: " + JSON.DEFAULT.writePretty(json));
			}

			var mcAccessToken = json.asString("access_token");
			return checkMcProfile(mcAccessToken);
		}

		if (json.asString("path").equals("/authentication/login_with_xbox")) {
			throw new BadRequestResponse("Error while acquiring Minecraft token! It's possible that Minecraft auth servers are down, please try again in a while!");
		}

		throw new BadRequestResponse("Minecraft link error D2 " + resp.statusCode() + ": " + JSON.DEFAULT.writePretty(json));
	}

	private static BaseMinecraftProfile checkMcProfile(String mcAccessToken) throws Exception {
		var request = HttpRequest.newBuilder(MC_PROFILE_URI)
				.header("Authorization", "Bearer " + mcAccessToken)
				.GET().build();

		var resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		var json = parseJson("E0", resp);

		if (resp.statusCode() >= 200 && resp.statusCode() < 400) {
			if (!json.containsKey("name") || !json.containsKey("id")) {
				throw new BadRequestResponse("Minecraft link error E1: " + JSON.DEFAULT.writePretty(json));
			}

			var name = json.asString("name");
			var uuid = json.asString("id");
			return new BaseMinecraftProfile(UUID.fromString(uuid.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5")), name);
		}

		if (json.asString("error").equals("NOT_FOUND")) {
			throw new BadRequestResponse("This Microsoft account isn't linked with a Minecraft profile!");
		}

		if (resp.statusCode() == 503) {
			throw new BadRequestResponse("Error while acquiring Minecraft profile! It's possible that Minecraft auth servers are down, please try again in a while!");
		}

		throw new BadRequestResponse("Minecraft link error E2 " + resp.statusCode() + ": " + JSON.DEFAULT.writePretty(json));
	}
}