package dev.gnomebot.app.server;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.WebToken;
import discord4j.core.object.entity.User;
import io.javalin.websocket.WsConfig;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CLISession {
	public Session session;
	public WebToken token;
	public User user;

	@Override
	public String toString() {
		return user.getUsername();
	}

	public static class Handler implements Consumer<WsConfig> {
		public static final Handler INSTANCE = new Handler();

		public final Map<Session, CLISession> sessions = new HashMap<>();

		@Override
		public void accept(WsConfig ws) {
			ws.onConnect(ctx -> {
				WebToken token = App.instance.getToken(ctx);

				if (token == null) {
					ctx.session.close(StatusCode.NORMAL, "Missing token!");
					return;
				}

				User user = App.instance.discordHandler.getUser(token.userId);

				if (user == null) {
					ctx.session.close(StatusCode.NORMAL, "User not found!");
					return;
				}

				synchronized (sessions) {
					CLISession session = new CLISession();
					session.session = ctx.session;
					session.token = token;
					session.user = user;
					sessions.put(session.session, session);
					App.warn("WS connected " + session + ": Port " + ctx.session.getRemoteAddress().getPort());
					ctx.send("Logged in as " + user.getUsername());
				}
			});

			ws.onMessage(ctx -> {
				synchronized (sessions) {
					CLISession session = sessions.get(ctx.session);

					if (session != null) {
						App.warn("WS message from " + session + ": " + ctx.message());
					}
				}
			});

			ws.onClose(ctx -> {
				synchronized (sessions) {
					CLISession session = sessions.remove(ctx.session);

					if (session != null) {
						App.warn("WS closed " + session + ": " + ctx.status() + " / " + ctx.reason());
					} else {
						App.error("Unknown WS session: #" + ctx.session.getRemoteAddress().getPort());
					}
				}
			});
		}

		public void broadcast(String message) {
			synchronized (sessions) {
				for (CLISession session : sessions.values()) {
					try {
						session.session.getRemote().sendString(message);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}
	}
}
