package dev.gnomebot.app.server;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.WebToken;
import discord4j.core.object.entity.User;
import io.javalin.websocket.WsConfig;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class WSHandler extends Thread implements Consumer<WsConfig> {
	public static final WSHandler CLI = new WSHandler("CLI");

	public final Map<Session, WSSession> sessions = new HashMap<>();
	private boolean started = false;

	public WSHandler(String name) {
		super("WebSocket-" + name);
		setDaemon(true);
	}

	@Override
	public void run() {
		while (App.instance.running) {
			try {
				Thread.sleep(10000L);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}

			synchronized (sessions) {
				for (WSSession session : sessions.values()) {
					try {
						session.session().getRemote().sendPing(null);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}
	}

	@Override
	public void accept(WsConfig ws) {
		ws.onConnect(ctx -> {
			if (!started) {
				started = true;
				start();
			}

			WebToken token = App.instance.db.getToken(ctx);

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
				WSSession session = new WSSession(this, ctx.session, token, user);
				sessions.put(session.session(), session);
				App.warn(getName() + " connected: " + session);
				onLoggedIn(session);
			}
		});

		ws.onMessage(ctx -> {
			synchronized (sessions) {
				WSSession session = sessions.get(ctx.session);

				if (session != null) {
					onMessage(session, ctx.message());
				}
			}
		});

		ws.onBinaryMessage(ctx -> {
			synchronized (sessions) {
				WSSession session = sessions.get(ctx.session);

				if (session != null) {
					onDataMessage(session, ctx.data());
				}
			}
		});

		ws.onClose(ctx -> {
			synchronized (sessions) {
				WSSession session = sessions.remove(ctx.session);

				if (session != null) {
					App.warn(getName() + " closed: " + session + " [" + ctx.status() + (ctx.reason() == null ? "" : (" / " + ctx.reason())) + "]");
				} else if (ctx.session.getRemoteAddress() instanceof InetSocketAddress inet) {
					App.error("Unknown " + getName() + " session: #" + inet.getPort());
				} else {
					App.error("Unknown " + getName() + " session: #" + ctx.session.getRemoteAddress());
				}
			}
		});
	}

	public void onLoggedIn(WSSession session) {
		session.sendString("Logged in as " + session.user().getUsername());
	}

	public void onMessage(WSSession session, String message) {
		App.warn(getName() + " message from " + session + ": " + message);
	}

	public void onDataMessage(WSSession session, byte[] data) {
		StringBuilder sb = new StringBuilder(getName() + " binary message from " + session + ": [" + data.length + " bytes]");

		for (int i = 0; i < data.length; i++) {
			if (i % 40 == 0) {
				sb.append('\n');
			}

			sb.append(String.format("%02X ", data[i]));
		}

		App.warn(sb);
	}

	public void broadcast(String message) {
		synchronized (sessions) {
			for (WSSession session : sessions.values()) {
				session.sendString(message);
			}
		}
	}

	public void closeAll(int code, String reason) {
		synchronized (sessions) {
			for (WSSession session : sessions.values()) {
				session.close(code, reason);
			}
		}
	}
}