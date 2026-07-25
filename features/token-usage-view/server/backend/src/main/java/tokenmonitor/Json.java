package tokenmonitor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small dependency-free JSON codec for the service boundary and provider adapters. */
public final class Json {
    private Json() {}

    public static Object parse(String source) {
        Parser parser = new Parser(source);
        Object value = parser.value();
        parser.space();
        if (!parser.end()) throw new IllegalArgumentException("unexpected JSON content at " + parser.pos);
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(String source) {
        Object value = parse(source);
        if (!(value instanceof Map<?, ?>)) throw new IllegalArgumentException("JSON object required");
        return (Map<String, Object>) value;
    }

    public static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out);
        return out.toString();
    }

    private static void write(Object value, StringBuilder out) {
        if (value == null) out.append("null");
        else if (value instanceof String text) quote(text, out);
        else if (value instanceof Number || value instanceof Boolean) out.append(value);
        else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) out.append(',');
                first = false;
                quote(String.valueOf(entry.getKey()), out);
                out.append(':');
                write(entry.getValue(), out);
            }
            out.append('}');
        } else if (value instanceof Iterable<?> values) {
            out.append('[');
            boolean first = true;
            for (Object item : values) {
                if (!first) out.append(',');
                first = false;
                write(item, out);
            }
            out.append(']');
        } else throw new IllegalArgumentException("unsupported JSON type: " + value.getClass());
    }

    private static void quote(String text, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        out.append('"');
    }

    public static String text(Map<String, Object> map, String key, boolean required) {
        Object value = map.get(key);
        if (value == null) {
            if (required) throw new IllegalArgumentException(key + " is required");
            return null;
        }
        if (!(value instanceof String result)) throw new IllegalArgumentException(key + " must be a string");
        return result;
    }

    public static long number(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String text) return new BigDecimal(text).longValueExact();
        throw new IllegalArgumentException(key + " must be a number");
    }

    public static BigDecimal decimal(Map<String, Object> map, String key, BigDecimal defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return new BigDecimal(number.toString());
        if (value instanceof String text) return new BigDecimal(text);
        throw new IllegalArgumentException(key + " must be a decimal");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> child(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> array(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof List<?> ? (List<Object>) value : List.of();
    }

    private static final class Parser {
        private final String source;
        private int pos;

        Parser(String source) { this.source = source == null ? "" : source; }
        boolean end() { return pos >= source.length(); }
        void space() { while (!end() && Character.isWhitespace(source.charAt(pos))) pos++; }

        Object value() {
            space();
            if (end()) throw new IllegalArgumentException("unexpected end of JSON");
            return switch (source.charAt(pos)) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", true);
                case 'f' -> literal("false", false);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        Map<String, Object> object() {
            pos++;
            Map<String, Object> result = new LinkedHashMap<>();
            space();
            if (take('}')) return result;
            while (true) {
                space();
                String key = string();
                space();
                expect(':');
                result.put(key, value());
                space();
                if (take('}')) return result;
                expect(',');
            }
        }

        List<Object> array() {
            pos++;
            List<Object> result = new ArrayList<>();
            space();
            if (take(']')) return result;
            while (true) {
                result.add(value());
                space();
                if (take(']')) return result;
                expect(',');
            }
        }

        String string() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (!end()) {
                char c = source.charAt(pos++);
                if (c == '"') return out.toString();
                if (c != '\\') { out.append(c); continue; }
                if (end()) throw new IllegalArgumentException("invalid JSON escape");
                char escaped = source.charAt(pos++);
                switch (escaped) {
                    case '"', '\\', '/' -> out.append(escaped);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (pos + 4 > source.length()) throw new IllegalArgumentException("invalid unicode escape");
                        out.append((char) Integer.parseInt(source.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    default -> throw new IllegalArgumentException("invalid JSON escape");
                }
            }
            throw new IllegalArgumentException("unterminated JSON string");
        }

        Object number() {
            int start = pos;
            if (take('-')) {}
            digits();
            if (take('.')) digits();
            if (!end() && (source.charAt(pos) == 'e' || source.charAt(pos) == 'E')) {
                pos++;
                if (!end() && (source.charAt(pos) == '+' || source.charAt(pos) == '-')) pos++;
                digits();
            }
            try { return new BigDecimal(source.substring(start, pos)); }
            catch (NumberFormatException error) { throw new IllegalArgumentException("invalid JSON number", error); }
        }

        void digits() {
            int start = pos;
            while (!end() && Character.isDigit(source.charAt(pos))) pos++;
            if (start == pos) throw new IllegalArgumentException("digit expected at " + pos);
        }

        Object literal(String expected, Object value) {
            if (!source.startsWith(expected, pos)) throw new IllegalArgumentException("invalid JSON literal at " + pos);
            pos += expected.length();
            return value;
        }

        boolean take(char expected) {
            if (!end() && source.charAt(pos) == expected) { pos++; return true; }
            return false;
        }

        void expect(char expected) {
            if (!take(expected)) throw new IllegalArgumentException("expected '" + expected + "' at " + pos);
        }
    }
}
