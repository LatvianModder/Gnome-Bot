package dev.gnomebot.app.server;

import dev.gnomebot.app.App;
import dev.gnomebot.app.Config;
import dev.gnomebot.app.data.WebToken;
import dev.gnomebot.app.server.handler.HTTPCodeException;
import dev.gnomebot.app.server.handler.Response;
import dev.gnomebot.app.server.html.RootTag;
import dev.gnomebot.app.server.html.Tag;
import dev.gnomebot.app.server.json.JsonServerPathHandler;
import dev.gnomebot.app.util.Ansi;
import dev.gnomebot.app.util.Pair;
import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import org.bson.Document;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class WebServer implements Consumer<JavalinConfig> {
	public static final HandlerType[] METHODS = {HandlerType.GET, HandlerType.POST, HandlerType.DELETE, HandlerType.PUT, HandlerType.PATCH};

	private final App app;
	public List<RequestHandler> handlerList;
	public List<Pair<String, WSHandler>> webSocketHandlerList;
	private Javalin javalin;

	public WebServer(App m) {
		app = m;
		handlerList = new ArrayList<>();
		webSocketHandlerList = new ArrayList<>();
	}

	public void start(int port) {
		if (javalin != null) {
			return;
		}

		javalin = Javalin.create(this);

		for (HandlerType method : METHODS) {
			MethodHandler w = new MethodHandler(this, method);
			int maxPathSize = 0;

			for (RequestHandler handler : handlerList) {
				if (handler.method == null) {
					throw new RuntimeException("Method for " + handler.path + " hasn't been set!");
				} else if (handler.method == method) {
					maxPathSize = Math.max(handler.splitPath.length, maxPathSize);
				}
			}

			if (maxPathSize > 0) {
				for (int i = 0; i < maxPathSize; i++) {
					w.handlers.add(new ArrayList<>());
				}

				for (RequestHandler handler : handlerList) {
					if (handler.method == method) {
						if (handler.path.indexOf(':') == -1) {
							w.staticHandlers.put(handler.path, handler);
						} else {
							w.handlers.get(handler.splitPath.length - 1).add(handler);
						}
					}
				}
			}

			javalin.addHandler(method, "*", w);
		}

		for (var h : webSocketHandlerList) {
			javalin.ws(h.a, h.b);
		}

		javalin.start(port);
	}

	public void stop() {
		for (var h : webSocketHandlerList) {
			h.b.closeAll(StatusCode.SHUTDOWN, "GnomeBot Restarting");
		}

		if (javalin != null) {
			javalin.stop();
			javalin = null;
		}

		handlerList.clear();
	}

	public RequestHandler add(String path, ServerPathHandler handler) {
		RequestHandler h = new RequestHandler(app, path, handler);
		handlerList.add(h);
		return h;
	}

	public RequestHandler add(String path, JsonServerPathHandler handler) {
		return add(path, (ServerPathHandler) handler);
	}

	public void addWS(String path, WSHandler consumer) {
		webSocketHandlerList.add(Pair.of(path, consumer));
	}

	@Override
	public void accept(JavalinConfig javalinConfig) {
		javalinConfig.showJavalinBanner = false;
	}

	public static String getPath(Context context) {
		String s = context.queryString();
		return context.path() + (s == null ? "" : ("?" + s));
	}

	public static class MethodHandler implements Handler {
		public final WebServer webServer;
		public final HandlerType method;
		public final Map<String, RequestHandler> staticHandlers;
		public final List<List<RequestHandler>> handlers;

		public MethodHandler(WebServer w, HandlerType m) {
			webServer = w;
			method = m;
			staticHandlers = new HashMap<>();
			handlers = new ArrayList<>();
		}

		@Override
		public void handle(Context ctx) {
			ctx.header("Access-Control-Allow-Origin", "*");
			ctx.header("Cache-Control", "max-age=0, private, no-cache");

			String p = ctx.path();

			while (p.startsWith("/")) {
				p = p.substring(1);
			}

			while (p.endsWith("/")) {
				p = p.substring(0, p.length() - 1);
			}

			String ip = ctx.header("CF-Connecting-IP");
			String country = ctx.header("CF-IPCountry");

			if (ip == null || ip.isEmpty()) {
				ip = "0.0.0.0";
			}

			if (country == null || country.isEmpty()) {
				country = "xx";
			}

			country = country.toLowerCase();
			WebToken[] tokenCallback = new WebToken[1];

			try {
				handle0(ctx, p, ip, country, tokenCallback);
			} catch (HTTPCodeException ex) {
				log(p, ip, country, ex.responseCode.code, tokenCallback[0]);
				Tag content = RootTag.createSimple(getPath(ctx), "Gnome Panel");
				content.p().span("red").string(ex.msg);
				content.asResponse(ex.responseCode).result(ctx);
			} catch (Exception ex) {
				ex.printStackTrace();
				log(p, ip, country, 500, tokenCallback[0]);
				Tag content = RootTag.createSimple(getPath(ctx), "Gnome Panel");
				content.p().span("red").string("Internal Error: " + ex);
				content.asResponse(HTTPResponseCode.INTERNAL_ERROR).result(ctx);
			}
		}

		private void handle0(Context ctx, String p, String ip, String country, WebToken[] tokenCallback) throws Exception {
			if (ip.equals("0.0.0.0") || country.equals("xx")) {
				throw HTTPResponseCode.BAD_REQUEST.error("Missing required headers");
			}

			WebToken token = webServer.app.db.getToken(ctx);
			tokenCallback[0] = token;

			RequestHandler sh = staticHandlers.get(p);

			if (sh != null) {
				ServerRequest serverRequest = new ServerRequest(webServer.app, ctx, ip, country, Collections.emptyMap());
				serverRequest.token = token;
				handle1(ctx, sh, p, serverRequest).result(ctx);
				return;
			}

			String[] pa = p.split("/");
			StringBuilder p1 = new StringBuilder(p);

			if (!ctx.queryParamMap().isEmpty()) {
				boolean first = true;

				for (Map.Entry<String, List<String>> qp : ctx.queryParamMap().entrySet()) {
					if (first) {
						p1.append('?');
						first = false;
					} else {
						p1.append('&');
					}

					p1.append(qp.getKey());
					p1.append('=');
					p1.append(qp.getValue().size() == 1 ? qp.getValue().get(0) : qp.getValue());
				}
			}

			if (pa.length <= handlers.size()) {
				Map<String, String> vars = new HashMap<>();

				for (RequestHandler dh : handlers.get(pa.length - 1)) {
					vars.clear();

					if (dh.matches(pa, vars)) {
						ServerRequest serverRequest = new ServerRequest(webServer.app, ctx, ip, country, vars);
						serverRequest.token = token;
						handle1(ctx, dh, p1.toString(), serverRequest).result(ctx);
						return;
					}
				}
			}

			RootTag.createSimple(getPath(ctx), "Gnome Panel").p().span("red").string("Page not found!").asResponse(HTTPResponseCode.NOT_FOUND).result(ctx);
		}

		private Response handle1(Context ctx, RequestHandler handler, String p, ServerRequest req) throws Exception {
			if (handler.authLevel != AuthLevel.NO_AUTH && req.token == null) {
				Tag content = RootTag.createSimple(req.getPath(), "Gnome Panel");
				content.p().span("red").string("You must be logged in to view this page!");
				content.p().string("Type ").span("green").string("/panel login").end().string(" command in any Discord server with this bot to generate login link.");
				content.p().string("You can refresh this page once you've logged in.");
				return content.asResponse(HTTPResponseCode.UNAUTHORIZED);
			} else if (handler.trusted && (req.token == null || !Config.get().isTrusted(req.token.userId))) {
				Tag content = RootTag.createSimple(req.getPath(), "Gnome Panel");
				content.p().span("red").string("You're not cool enough to view this page!");
				content.p().string("This page can only be viewed by bot owners!");

				if (req.token == null) {
					content.p().string("If you are one of the bot owners, type ").span("green").string("/panel login").end().string(" command in any Discord server with this bot to generate login link.");
					content.p().string("You can refresh this page once you've logged in.");
				}

				return content.asResponse(HTTPResponseCode.UNAUTHORIZED);
			}

			if (!req.variable("guild").isEmpty()) {
				req.gc = handler.app.db.guild(req.getSnowflake("guild"));

				if (req.token != null) {
					req.member = req.gc.getMember(req.token.userId);

					if (req.member != null) {
						req.token.authLevel = req.gc.getAuthLevel(req.member);
					}
				}

				if (!req.getAuthLevel().is(handler.authLevel)) {
					Tag content = RootTag.createSimple(req.getPath(), "Gnome Panel");
					content.p().span("red").string("You're not cool enough to view this page!");
					content.p().string("Required auth level is ").span("red").string(handler.authLevel.name.toLowerCase());
					content.p().string("Your auth level is ").span("red").string(req.getAuthLevel().name.toLowerCase());
					return content.asResponse(HTTPResponseCode.UNAUTHORIZED);
				}
			}

			Response r = handler.handler.handle(req);

			if (handler.cacheSeconds != 0 && r.getCode().isOK()) {
				ctx.header("Cache-Control", (handler.authLevel == AuthLevel.NO_AUTH ? "public" : "private") + ", max-age=" + Math.abs(handler.cacheSeconds));
			}

			if (handler.log) {
				log(p, req.ip, req.country, r.getCode().code, req.token);
			} else {
				App.LOGGER.webRequest();
			}

			return r;
		}

		private void log(String p, String ip, String country, int status, @Nullable WebToken token) {
			StringBuilder sout = new StringBuilder();
			Calendar date = Calendar.getInstance();
			String ip1 = String.format("%08X", ip.hashCode());

			sout.append(Ansi.GREEN);

			if (date.get(Calendar.HOUR_OF_DAY) < 10) {
				sout.append('0');
			}

			sout.append(date.get(Calendar.HOUR_OF_DAY));
			sout.append(':');

			if (date.get(Calendar.MINUTE) < 10) {
				sout.append('0');
			}

			sout.append(date.get(Calendar.MINUTE));
			sout.append(':');

			if (date.get(Calendar.SECOND) < 10) {
				sout.append('0');
			}

			sout.append(date.get(Calendar.SECOND));
			sout.append(Ansi.RESET);

			sout.append(' ');

			sout.append(Ansi.CYAN);
			sout.append(ip1);
			sout.append(Ansi.RESET);

			sout.append(' ');

			sout.append(Ansi.YELLOW);
			sout.append(country);
			sout.append(Ansi.RESET);

			sout.append(' ');

			if (token == null) {
				sout.append(Ansi.YELLOW);
				sout.append("anonymous");
				sout.append(Ansi.RESET);
			} else {
				sout.append(Ansi.GREEN);
				sout.append(token.getName());
				sout.append(Ansi.RESET);
			}

			sout.append(' ');

			sout.append((status / 100 == 4) ? Ansi.DARK_RED : Ansi.PURPLE);
			sout.append(status);
			sout.append(Ansi.RESET);

			sout.append(' ');

			sout.append(Ansi.YELLOW);
			sout.append(method.name().toUpperCase());
			sout.append(Ansi.RESET);

			sout.append(' ');

			sout.append(Ansi.CYAN);
			sout.append(p);
			sout.append(Ansi.RESET);

			App.info(sout.toString());

			webServer.app.queueBlockingTask(task -> {
				Document document = new Document();
				document.put("ip", ip1);
				document.put("country", country);
				document.put("status", status);
				document.put("method", method.name().toLowerCase());
				document.put("url", p);
				document.put("user", token == null ? 0L : token.userId.asLong());
				document.put("timestamp", Date.from(date.toInstant()));
				document.put("auth_level", (token == null ? AuthLevel.NO_AUTH : token.authLevel).name);
				webServer.app.db.webLog.insert(document);
			});
		}
	}
}