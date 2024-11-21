package dev.gnomebot.app.server;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.WebToken;
import dev.gnomebot.app.util.SnowFlake;
import dev.latvian.apps.ansi.ANSI;
import dev.latvian.apps.ansi.color.Color16;
import dev.latvian.apps.ansi.log.CallbackPrintStream;
import dev.latvian.apps.ansi.log.Log;
import dev.latvian.apps.tinyserver.http.HTTPHandler;
import dev.latvian.apps.tinyserver.http.HTTPRequest;
import dev.latvian.apps.tinyserver.http.response.HTTPPayload;
import dev.latvian.apps.tinyserver.http.response.HTTPResponse;
import dev.latvian.apps.tinyserver.http.response.HTTPStatus;
import dev.latvian.apps.tinyserver.http.response.error.HTTPError;
import dev.latvian.apps.tinyserver.http.response.error.client.BadRequestError;
import dev.latvian.apps.tinyserver.http.response.error.client.ForbiddenError;
import dev.latvian.apps.tinyserver.http.response.error.client.UnauthorizedError;
import dev.latvian.apps.webutils.html.TagFunction;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class AppRequest extends HTTPRequest {
	public static final String COOKIE_TOKEN = "gnometoken";

	public final App app;
	public WebToken token;
	private AuthLevel authLevel;
	public boolean log;
	public GuildCollections gc;
	private Member member;

	public AppRequest(App app) {
		this.app = app;
		this.token = null;
		this.authLevel = AuthLevel.NO_AUTH;
		this.log = true;
		this.gc = null;
	}

	public AuthLevel authLevel() {
		return authLevel;
	}

	@Nullable
	public User self() {
		return token == null ? null : app.discordHandler.getUser(token.userId);
	}

	public Member member() {
		if (member == null) {
			if (gc == null) {
				throw new BadRequestError();
			}

			member = token == null ? null : gc.getMember(token.userId);

			if (member == null) {
				throw new UnauthorizedError();
			}
		}

		return member;
	}

	private void checkAuth(AuthLevel required) {
		if (!authLevel().is(required)) {
			if (authLevel().isLoggedIn()) {
				throw new ForbiddenError("You're not cool enough to view this page!");
			} else {
				throw new UnauthorizedError("You must be logged in to view this page!").withExtraData((TagFunction) tag -> {
					tag.p().string("Type ").span("green").string("/panel login").end().string(" command in any Discord server with this bot to generate login link.");
					tag.p().string("You can refresh this page once you've logged in.");
				});
			}
		}
	}

	public void checkLoggedIn() {
		checkAuth(AuthLevel.LOGGED_IN);
	}

	public void checkMember() {
		checkAuth(AuthLevel.MEMBER);
	}

	public void checkAdmin() {
		checkAuth(AuthLevel.ADMIN);
	}

	public void checkOwner() {
		checkAuth(AuthLevel.OWNER);
	}

	public AppRootTag createRoot(String title) {
		return new AppRootTag(this, title, "");
	}

	public AppRootTag createRoot(String title, String description) {
		return new AppRootTag(this, title, description);
	}

	public long getSnowflake(String id) {
		var s = SnowFlake.num(variable(id).asString());

		if (s == 0L) {
			throw new BadRequestError("Invalid Snowflake: " + id + " / " + variable(id));
		} else {
			return s;
		}
	}

	@Override
	public void afterInit() {
		super.afterInit();

		var tokenStr = cookie(COOKIE_TOKEN).asString();

		if (tokenStr.isEmpty()) {
			tokenStr = header("Authorization").asString();

			if (tokenStr.startsWith("Bearer ")) {
				tokenStr = tokenStr.substring("Bearer ".length());
			}
		}

		if (!tokenStr.isEmpty()) {
			token = app.db.getToken(tokenStr);

			if (token != null) {
				authLevel = token.authLevel;
			}
		}

		if (variables().containsKey("guild")) {
			gc = app.db.guildOrNull(variable("guild").asULong());

			if (gc != null && token != null) {
				var lvl = gc.getAuthLevel(token.userId);

				if (lvl.ordinal() > authLevel.ordinal()) {
					authLevel = lvl;
				}
			}
		}
	}

	@Nullable
	@Override
	public HTTPResponse createPreResponse(@Nullable HTTPHandler<?> handler) {
		if (handler == null) {
			var root = createRoot("Not Found");
			var main = root.content;
			main.h1().string("Error 404");
			main.hr();
			main.h2().string("Page Not Found");
			return root.asResponse(HTTPStatus.NOT_FOUND, true);
		} else if (app.config.cloudflare.required && (ip().isEmpty() || country().equals("XX"))) {
			var root = createRoot("Bad Request");
			var main = root.content;
			main.h1().string("Bad Request");
			main.hr();
			main.h2().string("Missing required headers");
			return root.asResponse(HTTPStatus.BAD_REQUEST, true);
		} else if (gc == null && variables().containsKey("guild")) {
			var root = createRoot("Guild Not Found");
			var main = root.content;
			main.h1().string("Guild Not Found");
			return root.asResponse(HTTPStatus.NOT_FOUND, true);
		} else {
			return super.createPreResponse(handler);
		}
	}

	@Override
	public HTTPResponse handleResponse(HTTPPayload payload, HTTPResponse response, @Nullable Throwable error) {
		if (error instanceof HTTPError err && err.getStatus().code() != 500) {
			var root = createRoot("Error " + err.getStatus().code());
			root.content.h1().string("Error " + err.getStatus().code());
			root.content.hr();
			root.content.h2().string(err.getMessage());

			if (err.getExtraData() instanceof TagFunction f) {
				f.acceptTag(root.content);
			}

			return root.asResponse(err.getStatus(), true);
		} else if (error != null) {
			var root = createRoot("Internal Server Error");
			root.content.p().span("red").string(error.getMessage());

			if (token != null && token.authLevel.is(AuthLevel.ADMIN)) {
				var err = new ArrayList<String>();
				error.printStackTrace(new CallbackPrintStream(err::add));
				root.content.pre().span("red", String.join("\n", err));
			}

			return root.asResponse(HTTPStatus.INTERNAL_ERROR, true);
		} else {
			return response.cors();
		}
	}

	@Override
	public void afterResponse(HTTPPayload payload, HTTPResponse response, @Nullable HTTPHandler<?> handler, @Nullable Throwable error) {
		var status = error instanceof HTTPError err ? err.getStatus() : error != null ? HTTPStatus.INTERNAL_ERROR : response.status();

		var wstr = ANSI.empty();
		wstr.append("WEB");
		wstr.append(ANSI.of(' ' + country() + ' ').magenta());

		var login = self();

		if (login != null) {
			wstr.append(ANSI.cyan(login.getUsername() + ' '));
		} else {
			wstr.append(ANSI.green("PUBLIC "));
		}

		wstr.append(ANSI.of(method() + " " + status + " ").foreground(status.error() ? Color16.RED : status.redirect() ? Color16.YELLOW : Color16.LIME));
		wstr.append("/" + fullPath().replaceAll("code=\\w+", "code=***"));

		var userAgent = userAgent();

		if (userAgent == null || userAgent.isEmpty()) {
			userAgent = "Unknown";
		}

		wstr.append(ANSI.darkGray(" " + userAgent));
		Log.info(wstr);
	}

	// Multipart body handling //

	/*
	public Body getMainBody() {
		return getBodyList().get(0);
	}

	@Nullable
	public Body getBody(Predicate<Body> b) {
		for (var body : getBodyList()) {
			if (b.test(body)) {
				return body;
			}
		}

		return null;
	}

	@Nullable
	public Body getBody(String contentType) {
		return getBody(b -> b.contentType.startsWith(contentType));
	}

	@SuppressWarnings("deprecation")
	public List<Body> getBodyList() {
		if (bodyList == null) {
			bodyList = new ArrayList<>();

			var b = context.bodyAsBytes();
			var t = header("Content-Type", "unknown");

			// An eh implementation of multipart content
			if (t.startsWith("multipart/form-data;")) {
				var m = CodingUtils.decodeHeaders(t);
				var multipartStream = new MultipartStream(new ByteArrayInputStream(b), m.get("boundary").getBytes(StandardCharsets.UTF_8));

				try {
					var nextPart = multipartStream.skipPreamble();

					while (nextPart) {
						var body = new Body();

						for (var s : multipartStream.readHeaders().split("\r\n")) {
							if (s.toLowerCase().startsWith("content-type:")) {
								body.properties.put("content-type", s.substring(13).trim());
							} else if (s.toLowerCase().startsWith("content-disposition")) {
								var matcher = Pattern.compile("([\\w-]+)=\"([^\"]+)\"").matcher(s);

								while (matcher.find()) {
									body.properties.put(matcher.group(1).toLowerCase(), matcher.group(2));
								}
							}
						}

						body.name = body.getProperty("name", "payload");
						body.filename = body.getProperty("filename", body.name);
						body.contentType = body.getProperty("content-type", "text/plain");

						var out = new ByteArrayOutputStream();
						multipartStream.readBodyData(out);
						body.bytes = out.toByteArray();
						bodyList.add(body);

						nextPart = multipartStream.readBoundary();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			if (bodyList.isEmpty()) {
				var body = new Body();
				body.contentType = t;
				body.bytes = b;
				bodyList.add(body);
			}
		}

		return bodyList;
	}
	 */
}
