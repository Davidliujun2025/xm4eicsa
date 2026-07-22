package tokenmonitor;


import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

public final class TokenMonitorTests {
    private static int assertions;

    private TokenMonitorTests() {}

    public static void main(String[] args) throws Exception {
        jsonCodec();
        aggregationAndIdempotency();
        aggregationDoesNotTruncate();
        overviewTrendPaginationAndEmptyState();
        usageRateAndAnomalyState();
        providerCapabilityCatalog();
        httpContractAndIsolation();
        System.out.println("PASS: " + assertions + " assertions");
    }

    private static void jsonCodec() {
        Map<String, Object> parsed = Json.object("{\"s\":\"a\\nb\",\"n\":12.50,\"a\":[true,null]}");
        equal("a\nb", parsed.get("s"), "JSON escaped string");
        equal(new BigDecimal("12.50"), parsed.get("n"), "JSON decimal precision");
        equal(parsed, Json.parse(Json.stringify(parsed)), "JSON round trip");
    }

    private static void aggregationAndIdempotency() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-18T08:00:00Z"), ZoneId.of("UTC"));
        TokenUsageService service = new TokenUsageService(
                new InMemoryTokenUsageRepository(), ZoneId.of("Asia/Shanghai"), clock);
        TokenUsageEvent event = event("id-1", "u-1", 100, 20, 120, CallStatus.SUCCEEDED,
                Instant.parse("2026-07-18T07:59:59Z"));
        check(service.record(event), "first event inserted");
        check(!service.record(event), "duplicate event ignored");
        service.record(event("id-2", "u-1", 30, 10, 40, CallStatus.FAILED,
                Instant.parse("2026-07-18T08:00:00Z")));
        TokenUsageSummary summary = service.today("u-1");
        equal(130L, summary.inputTokens(), "today input aggregation");
        equal(30L, summary.outputTokens(), "today output aggregation");
        equal(160L, summary.totalTokens(), "today total aggregation");
        equal(1L, summary.successfulCalls(), "success count");
        equal(1L, summary.failedCalls(), "failure count");
        equal(250L, summary.averageResponseTimeMs(), "average response time");
    }

    private static void providerCapabilityCatalog() {
        ProviderQuotaService service = new ProviderQuotaService(Map.of(), Clock.systemUTC());
        var kimi = service.capabilities().stream().filter(c -> c.provider() == Provider.KIMI).findFirst().orElseThrow();
        var claude = service.capabilities().stream().filter(c -> c.provider() == Provider.CLAUDE).findFirst().orElseThrow();
        var minimax = service.capabilities().stream().filter(c -> c.provider() == Provider.MINIMAX).findFirst().orElseThrow();
        equal(CapabilityStatus.SUPPORTED, kimi.accountBalance(), "Kimi balance supported");
        equal(CapabilityStatus.USAGE_ONLY, claude.historicalUsage(), "Claude usage-only");
        equal(CapabilityStatus.SUPPORTED, minimax.tokenPlanRemaining(), "MiniMax plan supported");
    }

    private static void usageRateAndAnomalyState() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-18T08:00:00Z"), ZoneId.of("UTC"));
        TokenUsageService service = new TokenUsageService(new InMemoryTokenUsageRepository(),
                ZoneId.of("Asia/Shanghai"), clock, 200, new BigDecimal("0.80"), new BigDecimal("0.20"));
        service.record(event("rate-ok", "rate-user", 70, 20, 90, CallStatus.SUCCEEDED,
                Instant.parse("2026-07-18T07:00:00Z")));
        service.record(event("rate-failed", "rate-user", 70, 20, 90, CallStatus.FAILED,
                Instant.parse("2026-07-18T07:01:00Z")));
        TokenUsageStatus status = service.status("rate-user");
        equal(new BigDecimal("0.9000"), status.usageRate(), "daily usage rate");
        equal(20L, status.remainingTokens(), "daily remaining tokens");
        equal(TokenUsageStatus.Level.HIGH, status.level(), "high usage level");
        check(status.highUsage(), "high usage flag");
        equal(new BigDecimal("0.5000"), status.failureRate(), "failure rate");
        check(status.abnormal(), "failure anomaly detected");
        equal("HIGH_FAILURE_RATE", status.abnormalReason(), "failure anomaly reason");

        InMemoryTokenUsageRepository perUserRepository = new InMemoryTokenUsageRepository();
        TokenUsageService perUser = new TokenUsageService(perUserRepository, ZoneId.of("Asia/Shanghai"), clock,
                user -> user.equals("small-quota") ? 100L : 200L,
                new BigDecimal("0.80"), new BigDecimal("0.20"));
        perUser.record(event("small-quota-event", "small-quota", 70, 20, 90, CallStatus.SUCCEEDED,
                Instant.parse("2026-07-18T07:00:00Z")));
        perUser.record(event("large-quota-event", "large-quota", 70, 20, 90, CallStatus.SUCCEEDED,
                Instant.parse("2026-07-18T07:00:00Z")));
        equal(TokenUsageStatus.Level.HIGH, perUser.status("small-quota").level(), "per-user small quota high");
        equal(TokenUsageStatus.Level.NORMAL, perUser.status("large-quota").level(), "per-user large quota normal");
    }

    private static void aggregationDoesNotTruncate() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-18T08:00:00Z"), ZoneId.of("UTC"));
        TokenUsageService service = new TokenUsageService(
                new InMemoryTokenUsageRepository(), ZoneId.of("Asia/Shanghai"), clock);
        for (int i = 0; i < 1_005; i++) {
            service.record(event("bulk-" + i, "bulk-user", 1, 1, 2, CallStatus.SUCCEEDED,
                    Instant.parse("2026-07-18T07:00:00Z")));
        }
        equal(2_010L, service.today("bulk-user").totalTokens(), "aggregation is not limited to record page size");
    }

    private static void overviewTrendPaginationAndEmptyState() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-18T08:00:00Z"), ZoneId.of("UTC"));
        TokenUsageService service = new TokenUsageService(
                new InMemoryTokenUsageRepository(), ZoneId.of("Asia/Shanghai"), clock,
                1_000, new BigDecimal("0.80"), new BigDecimal("0.20"));
        for (int i = 0; i < 25; i++) {
            service.record(event("page-" + i, "page-user", 8, 2, 10, CallStatus.SUCCEEDED,
                    Instant.parse("2026-07-18T07:00:00Z").plusSeconds(i)));
        }
        service.record(event("previous-day", "page-user", 16, 4, 20, CallStatus.SUCCEEDED,
                Instant.parse("2026-07-17T07:00:00Z")));

        TokenUsageOverview overview = service.overview("page-user");
        equal(250L, overview.today().totalTokens(), "overview today total");
        equal(270L, overview.thisWeek().totalTokens(), "overview week total");
        equal(270L, overview.thisMonth().totalTokens(), "overview month total");
        equal(270L, overview.history().totalTokens(), "overview history total");
        check(!overview.empty(), "overview not empty");

        TrendSeries sevenDays = service.dailyTrendSeries("page-user", 7);
        equal(7, sevenDays.items().size(), "seven-day trend has fixed points");
        equal(250L, sevenDays.items().get(6).totalTokens(), "trend today total");
        check(!sevenDays.empty(), "trend not empty");

        TokenUsagePage first = service.recordsPage("page-user", Instant.EPOCH,
                Instant.parse("2026-07-19T00:00:00Z"), null, null, 1, 20);
        TokenUsagePage second = service.recordsPage("page-user", Instant.EPOCH,
                Instant.parse("2026-07-19T00:00:00Z"), null, null, 2, 20);
        equal(20, first.items().size(), "default page capacity");
        equal(6, second.items().size(), "second page remainder");
        equal(26L, first.totalElements(), "page total elements");
        equal(2, first.totalPages(), "page total pages");

        TokenUsageOverview emptyOverview = service.overview("empty-user");
        check(emptyOverview.empty(), "empty overview marked");
        TrendSeries emptyTrend = service.dailyTrendSeries("empty-user", 30);
        equal(30, emptyTrend.items().size(), "empty trend still has date axis");
        check(emptyTrend.empty(), "empty trend marked");
        check(service.recordsPage("empty-user", Instant.EPOCH,
                Instant.parse("2026-07-19T00:00:00Z"), null, null, 1, 20).empty(),
                "empty records marked");
    }

    private static void httpContractAndIsolation() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-07-18T08:00:00Z"), ZoneId.of("UTC"));
        TokenUsageService usage = new TokenUsageService(
                new InMemoryTokenUsageRepository(), ZoneId.of("Asia/Shanghai"), clock,
                20, new BigDecimal("0.80"), new BigDecimal("0.20"));
        try (TokenMonitorHttpServer server = new TokenMonitorHttpServer("127.0.0.1", 0, usage,
                new ProviderQuotaService(Map.of(), clock), "test-key")) {
            server.start();
            String base = "http://127.0.0.1:" + server.port();
            HttpClient client = HttpClient.newHttpClient();
            String body = """
                    {"idempotencyKey":"http-1","userId":"agent-a","conversationId":"c-1",
                     "requestId":"r-1","provider":"kimi","model":"moonshot-v1",
                     "inputContentLength":120,"outputContentLength":48,
                     "inputTokens":11,"outputTokens":7,"cachedInputTokens":0,"totalTokens":18,
                     "responseTimeMs":90,"status":"SUCCEEDED","occurredAt":"2026-07-18T07:00:00Z"}
                    """;
            HttpResponse<String> created = client.send(HttpRequest.newBuilder(URI.create(base + "/api/v1/internal/token-usage/events"))
                    .header("Content-Type", "application/json").header("X-Internal-Api-Key", "test-key")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
            equal(201, created.statusCode(), "ingest created");
            HttpResponse<String> duplicate = client.send(HttpRequest.newBuilder(URI.create(base + "/api/v1/internal/token-usage/events"))
                    .header("Content-Type", "application/json").header("X-Internal-Api-Key", "test-key")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
            equal(200, duplicate.statusCode(), "duplicate is safe retry");
            equal(true, Json.child(Json.object(duplicate.body()), "").isEmpty(), "JSON helper safe missing child");

            HttpResponse<String> mine = get(client, base + "/api/v1/token-usage/me/today", "agent-a");
            equal(200, mine.statusCode(), "today status");
            equal(18L, Json.number(Json.object(mine.body()), "totalTokens", -1), "today total via HTTP");
            equal(false, Json.object(mine.body()).get("empty"), "today non-empty flag");
            equal(20L, Json.number(Json.object(mine.body()), "dailyTokenLimit", -1), "today quota via HTTP");
            HttpResponse<String> overview = get(client, base + "/api/v1/token-usage/me/summary", "agent-a");
            equal(18L, Json.number(Json.child(Json.object(overview.body()), "today"), "totalTokens", -1),
                    "fixed overview today via HTTP");
            HttpResponse<String> summary = get(client, base + "/api/v1/token-usage/me/summary?from=2026-07-18T00%3A00%3A00Z&to=2026-07-19T00%3A00%3A00Z", "agent-a");
            equal(18L, Json.number(Json.object(summary.body()), "totalTokens", -1), "range summary via HTTP");
            HttpResponse<String> records = get(client, base + "/api/v1/token-usage/me/records?from=2026-07-18T00%3A00%3A00Z&to=2026-07-19T00%3A00%3A00Z", "agent-a");
            equal(1, Json.array(Json.object(records.body()), "items").size(), "records via HTTP");
            HttpResponse<String> detail = get(client, base + "/api/v1/token-usage/me/records/r-1", "agent-a");
            equal("r-1", Json.text(Json.object(detail.body()), "requestId", true), "call detail via HTTP");
            equal(120L, Json.number(Json.object(detail.body()), "inputContentLength", -1), "detail input length");
            equal(48L, Json.number(Json.object(detail.body()), "outputContentLength", -1), "detail output length");
            HttpResponse<String> trend = get(client, base + "/api/v1/token-usage/me/trend?from=2026-07-18T00%3A00%3A00Z&to=2026-07-19T00%3A00%3A00Z&bucket=HOUR", "agent-a");
            equal(1, Json.array(Json.object(trend.body()), "items").size(), "trend via HTTP");
            HttpResponse<String> sevenDayTrend = get(client,
                    base + "/api/v1/token-usage/me/trend?range=LAST_7_DAYS", "agent-a");
            equal(7, Json.array(Json.object(sevenDayTrend.body()), "items").size(), "seven-day trend via HTTP");
            HttpResponse<String> thirtyDayTrend = get(client,
                    base + "/api/v1/token-usage/me/trend?range=LAST_30_DAYS", "agent-a");
            equal(30, Json.array(Json.object(thirtyDayTrend.body()), "items").size(), "thirty-day trend via HTTP");
            HttpResponse<String> status = get(client, base + "/api/v1/token-usage/me/status", "agent-a");
            equal(new BigDecimal("0.9000"), Json.decimal(Json.object(status.body()), "usageRate", null), "usage rate via HTTP");
            equal("HIGH", Json.text(Json.object(status.body()), "level", true), "high usage via HTTP");
            HttpResponse<String> other = get(client, base + "/api/v1/token-usage/me/today", "agent-b");
            equal(0L, Json.number(Json.object(other.body()), "totalTokens", -1), "user isolation");
            equal(true, Json.object(other.body()).get("empty"), "empty today flag");
            HttpResponse<String> emptyOverview = get(client, base + "/api/v1/token-usage/me/summary", "agent-b");
            equal(true, Json.object(emptyOverview.body()).get("empty"), "empty overview flag");
            HttpResponse<String> emptyRecords = get(client, base + "/api/v1/token-usage/me/records", "agent-b");
            equal(true, Json.object(emptyRecords.body()).get("empty"), "empty records flag");
            HttpResponse<String> hiddenDetail = get(client, base + "/api/v1/token-usage/me/records/r-1", "agent-b");
            equal(404, hiddenDetail.statusCode(), "call detail owner isolation");
            HttpResponse<String> invalidRange = get(client, base + "/api/v1/token-usage/me/summary?from=2026-07-19T00%3A00%3A00Z&to=2026-07-18T00%3A00%3A00Z", "agent-a");
            equal(400, invalidRange.statusCode(), "invalid range handled");
            HttpResponse<String> unauthenticated = client.send(HttpRequest.newBuilder(
                    URI.create(base + "/api/v1/token-usage/me/today")).GET().build(), HttpResponse.BodyHandlers.ofString());
            equal(401, unauthenticated.statusCode(), "missing user rejected");
            HttpResponse<String> quotaUnauthenticated = client.send(HttpRequest.newBuilder(
                    URI.create(base + "/api/v1/provider-quotas/kimi")).GET().build(), HttpResponse.BodyHandlers.ofString());
            equal(401, quotaUnauthenticated.statusCode(), "provider balance requires service credential");

            for (int i = 0; i < 24; i++) {
                usage.record(event("http-page-" + i, "agent-a", 1, 1, 2, CallStatus.SUCCEEDED,
                        Instant.parse("2026-07-18T07:10:00Z").plusSeconds(i)));
            }
            HttpResponse<String> firstPage = get(client,
                    base + "/api/v1/token-usage/me/records?page=1&size=20", "agent-a");
            equal(20, Json.array(Json.object(firstPage.body()), "items").size(), "HTTP first page size");
            equal(25L, Json.number(Json.object(firstPage.body()), "totalElements", -1), "HTTP page total");
            HttpResponse<String> secondPage = get(client,
                    base + "/api/v1/token-usage/me/records?page=2&size=20", "agent-a");
            equal(5, Json.array(Json.object(secondPage.body()), "items").size(), "HTTP second page size");

            HttpResponse<String> refreshed = get(client, base + "/api/v1/token-usage/me/today", "agent-a");
            equal(66L, Json.number(Json.object(refreshed.body()), "totalTokens", -1),
                    "refresh reads latest records");
        }
    }

    private static HttpResponse<String> get(HttpClient client, String url, String user) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).header("X-User-Id", user).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static TokenUsageEvent event(String id, String user, long input, long output, long total,
                                         CallStatus status, Instant at) {
        return new TokenUsageEvent(id, user, "conversation", "request-" + id, Provider.GPT, "gpt-test",
                input, output, 0, total, 250, status, at, BigDecimal.ZERO, "USD");
    }

    private static void check(boolean condition, String label) {
        assertions++;
        if (!condition) throw new AssertionError(label);
    }

    private static void equal(Object expected, Object actual, String label) {
        assertions++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected=" + expected + ", actual=" + actual);
        }
    }
}
