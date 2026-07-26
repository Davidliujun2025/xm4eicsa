package tokenmonitor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Dependency-free Java 21 HTTP adapter. Domain logic stays in TokenUsageService. */
public final class TokenMonitorHttpServer implements AutoCloseable {
    private static final String JSON = "application/json; charset=utf-8";

    private final HttpServer server;
    private final TokenUsageService usage;
    private final ProviderQuotaService quotas;
    private final String internalApiKey;
    private final Path uiRoot;

    public TokenMonitorHttpServer(String host, int port, TokenUsageService usage,
                                  ProviderQuotaService quotas, String internalApiKey,
                                  String uiDirPath) throws IOException {
        this.usage = usage;
        this.quotas = quotas;
        this.internalApiKey = internalApiKey == null ? "" : internalApiKey;
        this.uiRoot = Path.of(uiDirPath == null || uiDirPath.isBlank() ? "../token-usage-view/ui" : uiDirPath)
                .toAbsolutePath().normalize();
        this.server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/", this::dispatchSafely);
    }

    public void start() { server.start(); }
    public int port() { return server.getAddress().getPort(); }
    @Override public void close() { server.stop(1); }

    private void dispatchSafely(HttpExchange exchange) throws IOException {
        try {
            dispatch(exchange);
        } catch (ApiException error) {
            sendError(exchange, error.status(), error.code(), error.getMessage());
        } catch (ProviderQuotaException error) {
            sendError(exchange, 502, "PROVIDER_QUOTA_ERROR",
                    error.getMessage() + (error.upstreamStatus() == 0 ? "" : " (upstream " + error.upstreamStatus() + ")"));
        } catch (IllegalArgumentException error) {
            sendError(exchange, 400, "INVALID_REQUEST", error.getMessage());
        } catch (Exception error) {
            error.printStackTrace(System.err);
            sendError(exchange, 500, "INTERNAL_ERROR", "internal server error");
        } finally {
            if (!exchange.getResponseHeaders().containsKey("Content-Type") ||
                    !exchange.getResponseHeaders().getFirst("Content-Type").startsWith("text/event-stream")) {
                exchange.close();
            }
        }
    }

    private void dispatch(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        if (method.equals("GET") && path.equals("/health")) {
            send(exchange, 200, Map.of("status", "UP", "time", Instant.now().toString()));
        } else if (method.equals("POST") && path.equals("/api/v1/internal/token-usage/events")) {
            ingest(exchange);
        } else if (method.equals("GET") && path.equals("/api/v1/token-usage/me/today")) {
            String user = currentUser(exchange);
            send(exchange, 200, ApiJson.today(usage.today(user), usage.status(user)));
        } else if (method.equals("GET") && path.equals("/api/v1/token-usage/me/summary")) {
            summary(exchange);
        } else if (method.equals("GET") && path.equals("/api/v1/token-usage/me/status")) {
            send(exchange, 200, ApiJson.status(usage.status(currentUser(exchange))));
        } else if (method.equals("GET") && path.equals("/api/v1/token-usage/me/records")) {
            records(exchange);
        } else if (method.equals("GET") && path.startsWith("/api/v1/token-usage/me/records/")) {
            detail(exchange, path.substring("/api/v1/token-usage/me/records/".length()));
        } else if (method.equals("GET") && path.equals("/api/v1/token-usage/me/trend")) {
            trend(exchange);
        } else if (method.equals("GET") && path.equals("/api/v1/token-usage/me/stream")) {
            stream(exchange);
        } else if (method.equals("GET") && path.equals("/api/v1/provider-quotas/capabilities")) {
            send(exchange, 200, Map.of("items", quotas.capabilities().stream().map(ApiJson::capability).toList()));
        } else if (method.equals("GET") && path.startsWith("/api/v1/provider-quotas/")) {
            requireInternal(exchange);
            String providerText = path.substring("/api/v1/provider-quotas/".length());
            Provider provider = Provider.parse(providerText);
            if (provider == Provider.OTHER) throw new ApiException(404, "UNKNOWN_PROVIDER", "unknown provider");
            send(exchange, 200, ApiJson.quota(quotas.fetch(provider)));
        } else if (method.equals("GET")) {
            serveStatic(exchange, path);
        } else {
            throw new ApiException(404, "NOT_FOUND", "route not found");
        }
    }

    private void ingest(HttpExchange exchange) throws IOException {
        requireInternal(exchange);
        Map<String, Object> body = Json.object(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        TokenUsageEvent event = new TokenUsageEvent(
                Json.text(body, "idempotencyKey", true),
                Json.text(body, "userId", true),
                Json.text(body, "conversationId", false),
                Json.text(body, "requestId", true),
                Provider.parse(Json.text(body, "provider", true)),
                Json.text(body, "model", true),
                Json.number(body, "inputContentLength", 0),
                Json.number(body, "outputContentLength", 0),
                Json.number(body, "inputTokens", 0),
                Json.number(body, "outputTokens", 0),
                Json.number(body, "cachedInputTokens", 0),
                Json.number(body, "totalTokens", 0),
                Json.number(body, "responseTimeMs", 0),
                CallStatus.parse(Json.text(body, "status", true)),
                Instant.parse(Json.text(body, "occurredAt", true)),
                Json.decimal(body, "providerReportedCost", BigDecimal.ZERO),
                Json.text(body, "costCurrency", false));
        boolean inserted = usage.record(event);
        send(exchange, inserted ? 201 : 200, Map.of("accepted", true, "duplicate", !inserted));
    }

    private void records(HttpExchange exchange) throws IOException {
        String user = currentUser(exchange);
        Map<String, String> query = query(exchange);
        Range range = query.containsKey("from") || query.containsKey("to")
                ? range(query) : new Range(Instant.EPOCH, Instant.now().plus(1, ChronoUnit.SECONDS));
        Provider provider = query.containsKey("provider") ? Provider.parse(query.get("provider")) : null;
        String model = query.get("model");
        int page = integer(query, "page", 1, 1, Integer.MAX_VALUE);
        int size = integer(query, "size", integer(query, "limit", 20, 1, 100), 1, 100);
        send(exchange, 200, ApiJson.page(usage.recordsPage(
                user, range.from(), range.to(), provider, model, page, size)));
    }

    private void summary(HttpExchange exchange) throws IOException {
        String user = currentUser(exchange);
        Map<String, String> query = query(exchange);
        if (!query.containsKey("from") && !query.containsKey("to")) {
            send(exchange, 200, ApiJson.overview(usage.overview(user)));
            return;
        }
        Range range = range(query);
        send(exchange, 200, ApiJson.summary(usage.summarize(user, range.from(), range.to())));
    }

    private void detail(HttpExchange exchange, String requestId) throws IOException {
        String user = currentUser(exchange);
        if (requestId.isBlank() || requestId.length() > 160) {
            throw new ApiException(400, "INVALID_REQUEST_ID", "request id is required and must not exceed 160 characters");
        }
        TokenUsageEvent event = usage.detail(user, requestId)
                .orElseThrow(() -> new ApiException(404, "USAGE_RECORD_NOT_FOUND", "token usage record not found"));
        send(exchange, 200, ApiJson.event(event));
    }

    private void trend(HttpExchange exchange) throws IOException {
        String user = currentUser(exchange);
        Map<String, String> query = query(exchange);
        TokenUsageService.Bucket bucket;
        try { bucket = TokenUsageService.Bucket.valueOf(query.getOrDefault("bucket", "DAY").toUpperCase()); }
        catch (IllegalArgumentException error) { throw new ApiException(400, "INVALID_BUCKET", "bucket must be HOUR or DAY"); }
        if (bucket == TokenUsageService.Bucket.HOUR) {
            Range range = range(query);
            List<Map<String, Object>> items = usage.trend(user, range.from(), range.to(), bucket)
                    .stream().map(ApiJson::trend).toList();
            send(exchange, 200, Map.of("items", items, "empty", items.isEmpty(),
                    "from", range.from().toString(), "to", range.to().toString()));
            return;
        }

        String preset = query.getOrDefault("range", "LAST_7_DAYS").toUpperCase();
        TrendSeries series;
        if (preset.equals("LAST_7_DAYS") || preset.equals("7D")) {
            series = usage.dailyTrendSeries(user, 7);
        } else if (preset.equals("LAST_30_DAYS") || preset.equals("30D")) {
            series = usage.dailyTrendSeries(user, 30);
        } else if (preset.equals("CUSTOM")) {
            if (!query.containsKey("from") || !query.containsKey("to")) {
                throw new ApiException(400, "CUSTOM_RANGE_REQUIRED", "from and to are required for CUSTOM range");
            }
            Range range = range(query);
            series = usage.dailyTrendSeries(user, range.from(), range.to());
        } else {
            throw new ApiException(400, "INVALID_TREND_RANGE", "range must be LAST_7_DAYS, LAST_30_DAYS or CUSTOM");
        }
        send(exchange, 200, ApiJson.trendSeries(series));
    }

    private void stream(HttpExchange exchange) throws Exception {
        String user = currentUser(exchange);
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-transform");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.sendResponseHeaders(200, 0);
        ArrayBlockingQueue<TokenUsageSummary> queue = new ArrayBlockingQueue<>(32);
        AutoCloseable subscription = usage.subscribe(summary -> {
            if (summary.userId().equals(user)) {
                if (!queue.offer(summary)) { queue.poll(); queue.offer(summary); }
            }
        });
        try (OutputStream output = exchange.getResponseBody()) {
            writeSse(output, "summary", ApiJson.summary(usage.today(user)));
            while (true) {
                TokenUsageSummary update = queue.poll(15, TimeUnit.SECONDS);
                if (update == null) output.write(": heartbeat\n\n".getBytes(StandardCharsets.UTF_8));
                else writeSse(output, "summary", ApiJson.summary(update));
                output.flush();
            }
        } catch (IOException ignored) {
            // Normal SSE disconnect.
        } finally {
            subscription.close();
        }
    }

    private static void writeSse(OutputStream output, String event, Object data) throws IOException {
        String frame = "event: " + event + "\ndata: " + Json.stringify(data) + "\n\n";
        output.write(frame.getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    private String currentUser(HttpExchange exchange) {
        // Integration point: replace this trusted gateway header with verified JWT/Session principal.
        String userId = exchange.getRequestHeaders().getFirst("X-User-Id");
        if (userId == null || userId.isBlank()) throw new ApiException(401, "UNAUTHENTICATED", "X-User-Id is required");
        if (userId.length() > 128) throw new ApiException(400, "INVALID_USER", "user id is too long");
        return userId;
    }

    private void requireInternal(HttpExchange exchange) {
        String supplied = exchange.getRequestHeaders().getFirst("X-Internal-Api-Key");
        if (internalApiKey.isBlank() || supplied == null || !MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(401, "INVALID_SERVICE_CREDENTIAL", "invalid internal service credential");
        }
    }

    private void serveStatic(HttpExchange exchange, String path) throws IOException {
        if (path == null || path.isBlank() || path.equals("/")) {
            path = "/index.html";
        }
        Path target = uiRoot.resolve(path.startsWith("/") ? path.substring(1) : path).normalize();
        if (!target.startsWith(uiRoot) || !Files.exists(target) || Files.isDirectory(target)) {
            throw new ApiException(404, "NOT_FOUND", "route not found");
        }
        String contentType = guessContentType(target);
        byte[] bytes = Files.readAllBytes(target);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String guessContentType(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".json")) return "application/json; charset=utf-8";
        if (name.endsWith(".svg")) return "image/svg+xml";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".woff")) return "font/woff";
        if (name.endsWith(".woff2")) return "font/woff2";
        return "application/octet-stream";
    }

    private static void validateRange(Instant from, Instant to) {
        if (!from.isBefore(to)) throw new ApiException(400, "INVALID_RANGE", "from must be before to");
        if (from.isBefore(to.minus(366, ChronoUnit.DAYS))) {
            throw new ApiException(400, "RANGE_TOO_LARGE", "range must not exceed 366 days");
        }
    }

    private static Map<String, String> query(HttpExchange exchange) {
        Map<String, String> result = new LinkedHashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isBlank()) return result;
        for (String part : raw.split("&")) {
            String[] pair = part.split("=", 2);
            result.put(decode(pair[0]), pair.length == 2 ? decode(pair[1]) : "");
        }
        return result;
    }

    private static String decode(String value) { return URLDecoder.decode(value, StandardCharsets.UTF_8); }
    private static Instant instant(Map<String, String> query, String key, Instant defaultValue) {
        String value = query.get(key);
        return value == null || value.isBlank() ? defaultValue : Instant.parse(value);
    }

    private static Range range(Map<String, String> query) {
        Instant to = instant(query, "to", Instant.now().plus(1, ChronoUnit.SECONDS));
        Instant from = instant(query, "from", to.minus(7, ChronoUnit.DAYS));
        validateRange(from, to);
        return new Range(from, to);
    }

    private static int integer(Map<String, String> query, String key, int defaultValue, int min, int max) {
        String value = query.get(key);
        int parsed = value == null ? defaultValue : Integer.parseInt(value);
        if (parsed < min || parsed > max) throw new ApiException(400, "INVALID_" + key.toUpperCase(), key + " out of range");
        return parsed;
    }

    private static void send(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = Json.stringify(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", JSON);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static void sendError(HttpExchange exchange, int status, String code, String message) throws IOException {
        try { send(exchange, status, Map.of("error", Map.of("code", code, "message", message))); }
        catch (IOException ignored) { exchange.close(); }
    }

    private record Range(Instant from, Instant to) {}
}
