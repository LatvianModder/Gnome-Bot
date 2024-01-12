package dev.gnomebot.app.server;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.WebToken;
import dev.gnomebot.app.discord.command.CommandOption;
import dev.gnomebot.app.discord.legacycommand.CommandContext;
import dev.gnomebot.app.util.SnowFlake;
import dev.latvian.apps.webutils.CodingUtils;
import discord4j.core.object.entity.Member;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import org.apache.commons.fileupload.MultipartStream;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ServerRequest {
	public final App app;
	public final Context context;
	public final String ip;
	public final String country;
	private final Map<String, String> variables;
	private Map<String, CommandOption> query;
	public WebToken token;
	public GuildCollections gc;
	public Member member;
	private List<Body> bodyList = null;

	public ServerRequest(App m, Context ctx, String i, String c, Map<String, String> v) {
		app = m;
		context = ctx;
		ip = i;
		country = c;
		variables = v;
	}

	public String getPath() {
		return WebServer.getPath(context);
	}

	private void initQuery() {
		if (query != null) {
			return;
		}

		query = new LinkedHashMap<>();

		var c = new CommandContext();
		c.handler = app.discordHandler;
		c.gc = gc;
		c.sender = member;

		for (var entry : context.queryParamMap().entrySet()) {
			if (!entry.getValue().isEmpty()) {
				query.put(entry.getKey(), new CommandOption(c, entry.getKey(), entry.getValue().get(0), false));
			}
		}
	}

	@Override
	public String toString() {
		return ip + "/" + country;
	}

	public String variable(String name) {
		return variables.getOrDefault(name, "");
	}

	public boolean hasQuery(String name) {
		initQuery();
		return query.containsKey(name);
	}

	public CommandOption query(String id) {
		initQuery();
		var o = query.get(id);

		if (o == null) {
			var c = new CommandContext();
			c.handler = app.discordHandler;
			c.gc = gc;
			c.sender = member;
			return new CommandOption(c, id, "", false);
		}

		return o;
	}

	public String header(String name, String def) {
		var s = context.header(name);
		return s == null || s.isEmpty() ? def : s;
	}

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

	public long getSnowflake(String var) {
		var s = SnowFlake.num(variable(var));

		if (s == 0L) {
			throw new BadRequestResponse("Invalid Snowflake: " + var + " / " + variable(var));
		} else {
			return s;
		}
	}

	public long getUnsignedLong(String var) {
		try {
			return Long.parseUnsignedLong(variable(var));
		} catch (Exception ex) {
			throw new BadRequestResponse("Invalid number: " + var + " / " + variable(var));
		}
	}

	public AuthLevel getAuthLevel() {
		return token == null ? AuthLevel.NO_AUTH : token.authLevel;
	}
}