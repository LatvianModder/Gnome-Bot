package dev.gnomebot.app.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.gnomebot.app.App;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class URLRequest<T> {
	@FunctionalInterface
	public interface OutputData {

		void write(OutputStream out) throws Exception;
	}

	@FunctionalInterface
	public interface Mapper<T> {

		T map(InputStream stream) throws Exception;

		default boolean autoclose() {
			return true;
		}

	}

	public static class UnsuccesfulRequestException extends IllegalStateException {
		public final int code;
		public final String response;

		public UnsuccesfulRequestException(int code, String response) {
			super("Error " + code + ": " + response);
			this.code = code;
			this.response = response;
		}
	}

	public static URLRequest<InputStream> of(String url) {
		return new URLRequest<>(url, new Mapper<>() {
			@Override
			public InputStream map(InputStream stream) {
				return stream;
			}

			@Override
			public boolean autoclose() {
				return false;
			}
		});
	}

	public static class SetCookie {
		public final String key;
		public final String value;
		public int maxAge;
		public String domain;
		public String path;
		public boolean secure;
		public boolean httpOnly;

		public SetCookie(String k, @Nullable Object v) {
			key = k;
			value = String.valueOf(v);
			maxAge = 0;
			domain = "";
			path = "";
			secure = false;
			httpOnly = false;
		}

	}

	private final String url;

	private Map<String, String> query;
	private List<SetCookie> setCookies;
	private Map<String, String> cookies;
	private final Mapper<T> mapper;
	private String method;
	private String contentType;
	private OutputData out;
	private Consumer<HttpURLConnection> connectionProcessor;
	private HttpURLConnection connection;
	private String hiddenUrlPart;
	private int timeout;

	private URLRequest(String u, Mapper<T> m) {
		url = u;
		query = new LinkedHashMap<>();
		setCookies = new ArrayList<>();
		cookies = new LinkedHashMap<>();
		mapper = m;
		method = "";
		contentType = "";
		out = null;
		connectionProcessor = connection -> {
		};
		hiddenUrlPart = "";
		timeout = 10000;
	}

	@Override
	public String toString() {
		return getFullUrl();
	}

	public String getFullUrl() {
		StringBuilder u = new StringBuilder(url);

		if (!query.isEmpty()) {
			boolean first = true;

			for (Map.Entry<String, String> entry : query.entrySet()) {
				if (first) {
					first = false;
					u.append('?');
				} else {
					u.append('&');
				}

				u.append(entry.getKey());
				u.append('=');
				u.append(Utils.encode(entry.getValue()));
			}
		}

		return u.toString();
	}

	public URLRequest<T> query(String name, @Nullable Object value) {
		if (value != null) {
			query.put(name, value.toString());
		}

		return this;
	}

	public URLRequest<T> setCookie(SetCookie cookie) {
		setCookies.add(cookie);
		return this;
	}

	public URLRequest<T> cookie(String name, @Nullable Object value) {
		cookies.put(name, String.valueOf(value));
		return this;
	}

	public URLRequest<T> method(String m) {
		method = m;
		return this;
	}

	public URLRequest<T> contentType(String c) {
		contentType = c;
		return this;
	}

	public URLRequest<T> out(OutputData data) {
		out = data;
		return this;
	}

	public URLRequest<T> outBytes(byte[] data) {
		return addHeader("Content-Length", Integer.toString(data.length)).out(out -> out.write(data));
	}

	public URLRequest<T> outString(String text) {
		return outBytes(text.getBytes(StandardCharsets.UTF_8));
	}

	public URLRequest<T> outForm(Map<String, String> map) {
		return outString(map.entrySet().stream().map(entry -> entry.getKey() + "=" + Utils.encode(entry.getValue())).collect(Collectors.joining("&"))).contentType("application/x-www-form-urlencoded");
	}

	public URLRequest<T> process(Consumer<HttpURLConnection> processor) {
		connectionProcessor = connectionProcessor.andThen(processor);
		return this;
	}

	public URLRequest<T> addHeader(String header, Object value) {
		return process(c -> c.setRequestProperty(header, String.valueOf(value)));
	}

	public URLRequest<T> hiddenUrlPart(String part) {
		hiddenUrlPart = part;
		return this;
	}

	public URLRequest<T> timeout(int t) {
		timeout = t;
		return this;
	}

	private static BufferedReader getReader(InputStream stream) {
		return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
	}

	public <M> URLRequest<M> map(Mapper<M> mapper) {
		URLRequest<M> request = new URLRequest<>(url, mapper);
		request.query = query;
		request.setCookies = setCookies;
		request.cookies = cookies;
		request.method = method;
		request.contentType = contentType;
		request.out = out;
		request.connectionProcessor = connectionProcessor;
		return request;
	}

	public URLRequest<URLRequest<?>> toSelf() {
		return map(stream -> URLRequest.this);
	}

	public URLRequest<byte[]> toBytes(int bufferSize) {
		return map(stream -> {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			byte[] buffer = new byte[bufferSize];
			int len;

			while ((len = stream.read(buffer)) > 0) {
				bytes.write(buffer, 0, len);
			}

			return bytes.toByteArray();
		});
	}

	public URLRequest<byte[]> toBytes() {
		return toBytes(8192);
	}

	public URLRequest<BufferedImage> toImage() {
		return map(ImageIO::read);
	}

	public URLRequest<JsonElement> toJson() {
		return map(stream -> Utils.GSON.fromJson(getReader(stream), JsonElement.class));
	}

	public URLRequest<JsonObject> toJsonObject() {
		return map(stream -> Utils.GSON.fromJson(getReader(stream), JsonObject.class));
	}

	public URLRequest<JsonArray> toJsonArray() {
		return map(stream -> Utils.GSON.fromJson(getReader(stream), JsonArray.class));
	}

	public URLRequest<List<String>> toStringList() {
		return map(stream -> {
			BufferedReader reader = getReader(stream);
			List<String> list = new ArrayList<>();
			String l;

			while ((l = reader.readLine()) != null) {
				list.add(l);
			}

			return list;
		});
	}

	public URLRequest<String> toJoinedString() {
		return map(stream -> {
			BufferedReader reader = getReader(stream);
			List<String> list = new ArrayList<>();
			String l;

			while ((l = reader.readLine()) != null) {
				list.add(l);
			}

			return String.join("\n", list);
		});
	}

	public T block() throws Exception {
		String m = method.isEmpty() ? (out == null ? "GET" : "POST") : method;
		// App.log("Opening " + m + " connection to " + url + "...");

		String fullUrl = getFullUrl();
		connection = (HttpURLConnection) new URL(fullUrl).openConnection();
		connection.setRequestMethod(m);
		connection.setDoInput(true);
		connection.setDoOutput(out != null);
		connection.setConnectTimeout(timeout);
		connection.setReadTimeout(timeout);

		connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36");

		if (!cookies.isEmpty()) {
			connection.setRequestProperty("Cookie", Utils.encode(cookies, "; "));
		}

		for (SetCookie cookie : setCookies) {
			Map<String, String> map = new LinkedHashMap<>();
			map.put(cookie.key, cookie.value);

			if (cookie.maxAge > 0) {
				map.put("Max-Age", String.valueOf(cookie.maxAge));
			}

			if (!cookie.domain.isEmpty()) {
				map.put("Domain", cookie.domain);
			}

			if (!cookie.path.isEmpty()) {
				map.put("Path", cookie.path);
			}

			if (cookie.secure) {
				map.put("Secure", "");
			}

			if (cookie.httpOnly) {
				map.put("HttpOnly", "");
			}

			connection.addRequestProperty("Set-Cookie", Utils.encode(map, "; "));
		}

		connectionProcessor.accept(connection);

		if (!contentType.isEmpty()) {
			connection.setRequestProperty("Content-Type", contentType);
		}

		if (out != null) {
			try (OutputStream stream = connection.getOutputStream()) {
				out.write(stream);
			}
		}

		int code = connection.getResponseCode();

		if (code / 100 == 2) {
			if (mapper.autoclose()) {
				try (InputStream stream = connection.getInputStream()) {
					return mapper.map(stream);
				}
			} else {
				return mapper.map(connection.getInputStream());
			}
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
			StringBuilder sb = new StringBuilder();
			String l;

			while ((l = reader.readLine()) != null) {
				sb.append(l);
				sb.append('\n');
			}

			if (code == 429 && sb.charAt(0) == '{') {
				JsonObject json = Utils.GSON.fromJson(sb.toString(), JsonObject.class);

				if (json.has("retry_after")) {
					App.warn("Request " + m + " from " + (hiddenUrlPart.isEmpty() ? fullUrl : fullUrl.replace(hiddenUrlPart, "***")) + " rate limited for " + json.get("retry_after").getAsLong() + "ms");
					Thread.sleep(json.get("retry_after").getAsLong() + 50L);
					return block();
				}
			}

			throw new UnsuccesfulRequestException(code, sb.toString().trim());
		}
	}

	public Either<T, Exception> blockEither() {
		try {
			return Either.left(block());
		} catch (Exception ex) {
			return Either.right(ex);
		}
	}

	public void subscribe(Consumer<T> contentCallback, Consumer<Exception> errorCallback) {
		Thread thread = new Thread(() -> {
			try {
				contentCallback.accept(block());
			} catch (Exception ex) {
				errorCallback.accept(ex);
			}
		});

		thread.setDaemon(true);
		thread.start();
	}

	public void subscribeContent(Consumer<T> callback) {
		subscribe(callback, Exception::printStackTrace);
	}

	public String getHeader(String header, String def) {
		String s = connection.getHeaderField(header);
		return s == null || s.isEmpty() ? def : s;
	}

	public String getHeader(String header) {
		return getHeader(header, "");
	}

	public Map<String, List<String>> getHeaderFields() {
		return connection.getHeaderFields();
	}
}
