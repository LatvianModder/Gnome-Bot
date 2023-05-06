package dev.gnomebot.app.server;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.WebToken;
import dev.gnomebot.app.discord.command.CommandOption;
import dev.gnomebot.app.discord.legacycommand.CommandContext;
import dev.gnomebot.app.server.handler.HTTPCodeException;
import dev.latvian.apps.webutils.CodingUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author LatvianModder
 */
public class ServerRequest {
	public final App app;
	private final Context context;
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

		CommandContext c = new CommandContext();
		c.handler = app.discordHandler;
		c.gc = gc;
		c.sender = member;

		for (Map.Entry<String, List<String>> entry : context.queryParamMap().entrySet()) {
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
		CommandOption o = query.get(id);

		if (o == null) {
			CommandContext c = new CommandContext();
			c.handler = app.discordHandler;
			c.gc = gc;
			c.sender = member;
			return new CommandOption(c, id, "", false);
		}

		return o;
	}

	public String header(String name, String def) {
		String s = context.header(name);
		return s == null || s.isEmpty() ? def : s;
	}

	public Body getMainBody() {
		return getBodyList().get(0);
	}

	@Nullable
	public Body getBody(Predicate<Body> b) {
		for (Body body : getBodyList()) {
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

			byte[] b = context.bodyAsBytes();
			String t = header("Content-Type", "unknown");

			// An eh implementation of multipart content
			if (t.startsWith("multipart/form-data;")) {
				var m = CodingUtils.decodeHeaders(t);
				var multipartStream = new MultipartStream(new ByteArrayInputStream(b), m.get("boundary").getBytes(StandardCharsets.UTF_8));

				try {
					boolean nextPart = multipartStream.skipPreamble();

					while (nextPart) {
						Body body = new Body();

						for (String s : multipartStream.readHeaders().split("\r\n")) {
							if (s.toLowerCase().startsWith("content-type:")) {
								body.properties.put("content-type", s.substring(13).trim());
							} else if (s.toLowerCase().startsWith("content-disposition")) {
								Matcher matcher = Pattern.compile("([\\w-]+)=\"([^\"]+)\"").matcher(s);

								while (matcher.find()) {
									body.properties.put(matcher.group(1).toLowerCase(), matcher.group(2));
								}
							}
						}

						body.name = body.getProperty("name", "payload");
						body.filename = body.getProperty("filename", body.name);
						body.contentType = body.getProperty("content-type", "text/plain");

						ByteArrayOutputStream out = new ByteArrayOutputStream();
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
				Body body = new Body();
				body.contentType = t;
				body.bytes = b;
				bodyList.add(body);
			}
		}

		return bodyList;
	}

	public Snowflake getSnowflake(String var) throws HTTPCodeException {
		try {
			return Snowflake.of(variable(var));
		} catch (Exception ex) {
			throw HTTPResponseCode.BAD_REQUEST.error("Invalid Snowflake: " + var + " / " + variable(var));
		}
	}

	public long getUnsignedLong(String var) throws HTTPCodeException {
		try {
			return Long.parseUnsignedLong(variable(var));
		} catch (Exception ex) {
			throw HTTPResponseCode.BAD_REQUEST.error("Invalid number: " + var + " / " + variable(var));
		}
	}

	public AuthLevel getAuthLevel() {
		return token == null ? AuthLevel.NO_AUTH : token.authLevel;
	}
}