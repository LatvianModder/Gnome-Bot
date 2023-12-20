package dev.gnomebot.app.server;

import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.BrainEvents;
import dev.gnomebot.app.Config;
import dev.gnomebot.app.data.WebToken;
import dev.gnomebot.app.server.json.JsonServerPathHandler;
import dev.latvian.apps.webutils.ansi.Ansi;
import dev.latvian.apps.webutils.data.Pair;
import dev.latvian.apps.webutils.net.Response;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.http.Header;
import io.javalin.http.HttpResponseException;
import io.javalin.http.HttpStatus;
import org.bson.Document;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class WebServer implements Consumer<JavalinConfig> {
	public static final HandlerType[] METHODS = {HandlerType.GET, HandlerType.POST, HandlerType.DELETE, HandlerType.PUT, HandlerType.PATCH};

	private final App app;
	public List<RequestHandler> handlerList;
	public List<Pair<String, WSHandler>> webSocketHandlerList;
	private Javalin javalin;
	private final Set<String> staticFilesSet;
	private final Set<String> staticFilesSetNoCache;

	public WebServer(App m) {
		app = m;
		handlerList = new ArrayList<>();
		webSocketHandlerList = new ArrayList<>();

		this.staticFilesSet = new HashSet<>();

		this.staticFilesSet.add("/apple-touch-icon.png");
		this.staticFilesSet.add("/apple-touch-icon-precomposed.png");
		this.staticFilesSet.add("/apple-touch-icon-152x152.png");
		this.staticFilesSet.add("/apple-touch-icon-152x152-precomposed.png");
		this.staticFilesSet.add("/apple-touch-icon-120x120.png");
		this.staticFilesSet.add("/apple-touch-icon-120x120-precomposed.png");

		try (var stream = Files.walk(AppPaths.RESOURCES)) {
			stream.filter(Files::isRegularFile)
					.map(AppPaths.RESOURCES::relativize)
					.map(path -> "/" + path.toString().replace('\\', '/'))
					.forEach(this.staticFilesSet::add);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		this.staticFilesSetNoCache = Set.of(
				"/assets/script.js",
				"/assets/style.css"
		);
	}

	public void start(int port) {
		if (javalin != null) {
			return;
		}

		javalin = Javalin.create(this);

		for (var method : METHODS) {
			var w = new MethodHandler(this, method);
			int maxPathSize = 0;

			for (var handler : handlerList) {
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

				for (var handler : handlerList) {
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
			javalin.ws(h.a(), h.b());
		}

		javalin.start(port);
	}

	public void stop() {
		for (var h : webSocketHandlerList) {
			h.b().closeAll(StatusCode.SHUTDOWN, "GnomeBot Restarting");
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
			ctx.header(Header.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			ctx.header(Header.CACHE_CONTROL, "max-age=0, private, no-cache");

			if (ctx.method() == HandlerType.GET && webServer.staticFilesSet.contains(ctx.path())) {
				try {
					var path = AppPaths.RESOURCES.resolve(ctx.path().substring(1));
					ctx.result(Files.readAllBytes(path));
					ctx.contentType(Files.probeContentType(path));
					ctx.status(HttpStatus.OK);

					if (!webServer.staticFilesSetNoCache.contains(ctx.path())) {
						ctx.header(Header.CACHE_CONTROL, "public, max-age=3600");
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					ctx.status(HttpStatus.NOT_FOUND);
				}

				return;
			}

			var p = ctx.path();

			while (p.startsWith("/")) {
				p = p.substring(1);
			}

			while (p.endsWith("/")) {
				p = p.substring(0, p.length() - 1);
			}

			var ip = ctx.header("CF-Connecting-IP");

			if (ip == null || ip.isEmpty()) {
				ip = "0.0.0.0";
			}

			var country = ctx.header("CF-IPCountry");

			if (country == null || country.isEmpty()) {
				country = "xx";
			}

			country = country.toLowerCase();
			WebToken[] tokenCallback = new WebToken[1];

			try {
				handle0(ctx, p, ip, country, tokenCallback);
			} catch (HttpResponseException ex) {
				var status = HttpStatus.forStatus(ex.getStatus());
				log(p, ip, country, status, tokenCallback[0]);
				var root = GnomeRootTag.createSimple(getPath(ctx), "Error " + ex.getStatus());
				root.content.p().span("red").string(ex.getMessage());
				root.asResponse(status, true).result(ctx);
			} catch (Exception ex) {
				ex.printStackTrace();
				log(p, ip, country, HttpStatus.INTERNAL_SERVER_ERROR, tokenCallback[0]);
				var root = GnomeRootTag.createSimple(getPath(ctx), "Internal Server Error");
				root.content.p().span("red").string("Internal Error: " + ex);
				root.asResponse(HttpStatus.INTERNAL_SERVER_ERROR, true).result(ctx);
			}
		}

		private void handle0(Context ctx, String p, String ip, String country, WebToken[] tokenCallback) throws Exception {
			if (Config.get().require_cloudflare && (ip.equals("0.0.0.0") || country.equals("xx"))) {
				throw HTTPResponseCode.BAD_REQUEST.error("Missing required headers");
			}

			var token = webServer.app.db.getToken(ctx);
			tokenCallback[0] = token;

			var sh = staticHandlers.get(p);

			if (sh != null) {
				var serverRequest = new ServerRequest(webServer.app, ctx, ip, country, Collections.emptyMap());
				serverRequest.token = token;
				handle1(ctx, sh, p, serverRequest).result(ctx);
				return;
			}

			var pa = p.split("/");
			var p1 = new StringBuilder(p);

			if (!ctx.queryParamMap().isEmpty()) {
				boolean first = true;

				for (var qp : ctx.queryParamMap().entrySet()) {
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
				var vars = new HashMap<String, String>();

				for (var dh : handlers.get(pa.length - 1)) {
					vars.clear();

					if (dh.matches(pa, vars)) {
						ServerRequest serverRequest = new ServerRequest(webServer.app, ctx, ip, country, vars);
						serverRequest.token = token;
						handle1(ctx, dh, p1.toString(), serverRequest).result(ctx);
						return;
					}
				}
			}

			var root = GnomeRootTag.createSimple(getPath(ctx), "Not Found");
			root.content.p().span("red").string("Page not found!");
			root.asResponse(HttpStatus.NOT_FOUND, true).result(ctx);
		}

		private Response handle1(Context ctx, RequestHandler handler, String p, ServerRequest req) throws Exception {
			if (handler.authLevel != AuthLevel.NO_AUTH && req.token == null) {
				var root = GnomeRootTag.createSimple(req.getPath(), "Unauthorized");
				root.content.p().span("red").string("You must be logged in to view this page!");
				root.content.p().string("Type ").span("green").string("/panel login").end().string(" command in any Discord server with this bot to generate login link.");
				root.content.p().string("You can refresh this page once you've logged in.");
				return root.asResponse(HttpStatus.UNAUTHORIZED, true);
			} else if (handler.trusted && (req.token == null || !Config.get().isTrusted(req.token.userId))) {
				var root = GnomeRootTag.createSimple(req.getPath(), "Forbidden");
				root.content.p().span("red").string("You're not cool enough to view this page!");
				root.content.p().string("This page can only be viewed by bot owners!");

				if (req.token == null) {
					root.content.p().string("If you are one of the bot owners, type ").span("green").string("/panel login").end().string(" command in any Discord server with this bot to generate login link.");
					root.content.p().string("You can refresh this page once you've logged in.");
				}

				return root.asResponse(HttpStatus.UNAUTHORIZED, true);
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
					var root = GnomeRootTag.createSimple(req.getPath(), "Unauthorized");
					root.content.p().span("red").string("You're not cool enough to view this page!");
					root.content.p().string("Required auth level is ").span("red").string(handler.authLevel.name.toLowerCase());
					root.content.p().string("Your auth level is ").span("red").string(req.getAuthLevel().name.toLowerCase());
					return root.asResponse(HttpStatus.UNAUTHORIZED, true);
				}
			}

			var r = handler.handler.handle(req);

			if (handler.cacheSeconds != 0 && r.getStatus().getCode() / 100 == 2) {
				ctx.header(Header.CACHE_CONTROL, (handler.authLevel == AuthLevel.NO_AUTH ? "public" : "private") + ", max-age=" + Math.abs(handler.cacheSeconds));
			}

			if (handler.log) {
				log(p, req.ip, req.country, r.getStatus(), req.token);
			} else {
				App.LOGGER.event(BrainEvents.WEB_REQUEST);
			}

			return r;
		}

		private void log(String p, String ip, String country, HttpStatus status, @Nullable WebToken token) {
			var sout = Ansi.of();
			var ip1 = String.format("%08X", ip.hashCode());

			sout.append(Ansi.cyan(ip1));
			sout.append(' ');
			sout.append(Ansi.yellow(country));
			sout.append(' ');

			if (token == null) {
				sout.append(Ansi.yellow("anonymous"));
			} else {
				sout.append(Ansi.green(token.getName()));
			}

			sout.append(' ');
			sout.append((status.getCode() / 100 == 4) ? Ansi.darkRed(status) : Ansi.purple(status));
			sout.append(' ');
			sout.append(Ansi.yellow(method.name().toUpperCase()));
			sout.append(' ');
			sout.append(Ansi.cyan(p));

			App.info(sout);

			var date = new Date();

			webServer.app.queueBlockingTask(task -> {
				var document = new Document();
				document.put("ip", ip1);
				document.put("country", country);
				document.put("status", status);
				document.put("method", method.name().toLowerCase());
				document.put("url", p);
				document.put("user", token == null ? 0L : token.userId.asLong());
				document.put("timestamp", date);
				document.put("auth_level", (token == null ? AuthLevel.NO_AUTH : token.authLevel).name);
				webServer.app.db.webLog.insert(document);
			});
		}
	}
}