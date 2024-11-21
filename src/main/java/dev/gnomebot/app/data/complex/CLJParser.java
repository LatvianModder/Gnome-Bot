package dev.gnomebot.app.data.complex;

import dev.gnomebot.app.util.SimpleStringReader;
import dev.latvian.apps.webutils.misc.StringParser;
import dev.latvian.apps.webutils.misc.StringToken;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CLJParser {
	public static final StringToken LP = StringToken.of("(");
	public static final StringToken RP = StringToken.of(")");
	public static final StringToken ARROW = StringToken.of("->");
	public static final StringToken QUOTE = StringToken.of("\"");
	public static final StringToken BREAK = RP.or(StringToken.SPACE);

	public record CLJObject(String name, List<Object> args) {
		@Nullable
		public Object get(int i) {
			return i >= 0 && i < args.size() ? args.get(i) : null;
		}

		@Override
		public String toString() {
			var sb = new StringBuilder("(");
			sb.append(name);

			for (var arg : args) {
				sb.append(' ');
				if (arg instanceof String) {
					sb.append(SimpleStringReader.escape(arg.toString()));
				} else {
					sb.append(arg);
				}
			}

			sb.append(')');
			return sb.toString();
		}

		public List<CLJObject> unwrap() {
			var list = new ArrayList<CLJObject>();
			var obj = this;

			while (true) {
				list.addFirst(obj);

				if (obj.get(0) instanceof CLJObject o) {
					obj.args.removeFirst();
					obj = o;
				} else {
					break;
				}
			}

			return list;
		}
	}

	public static Object parse(StringParser reader) {
		if (reader.ifRead(LP)) {
			reader.skipSpace();

			if (reader.ifRead(ARROW)) {
				reader.skipSpace();

				var obj = parse(reader);
				reader.skipSpace();

				while (!reader.ifRead(RP)) {
					var n = parse(reader);
					reader.skipSpace();

					if (n instanceof CLJObject cljo) {
						cljo.args.addFirst(obj);
						obj = cljo;
					} else {
						break;
					}
				}

				return obj;
			}

			var clj = new CLJObject(reader.readUntil(BREAK, false), new ArrayList<>());

			while (true) {
				reader.skipSpace();

				if (reader.ifRead(RP)) {
					break;
				}

				clj.args.add(parse(reader));
			}

			if (clj.name.equals("str")) {
				if (clj.get(0) == null) {
					return "";
				}

				var sb = new StringBuilder();

				for (var arg : clj.args) {
					sb.append(arg);
				}

				return sb.toString();
			}

			return clj;
		} else if (reader.ifRead(QUOTE)) {
			return reader.readUntil(QUOTE, true).replace("\\n", "\n");
		} else {
			var arg = reader.readUntil(BREAK, false);

			return switch (arg) {
				case "true" -> Boolean.TRUE;
				case "false" -> Boolean.FALSE;
				case "nil" -> null;
				default -> {
					try {
						yield Integer.parseInt(arg);
					} catch (Exception ex) {
						yield arg;
					}
				}
			};
		}
	}
}
