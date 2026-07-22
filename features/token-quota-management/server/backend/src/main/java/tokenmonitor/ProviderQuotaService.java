package tokenmonitor;


import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Capability catalog plus configured provider endpoint registry. */
public final class ProviderQuotaService {
    private final Map<Provider, QuotaEndpoint> endpoints = new EnumMap<>(Provider.class);
    private final HttpJsonQuotaClient client;

    public ProviderQuotaService(Map<String, String> environment, Clock clock) {
        this.client = new HttpJsonQuotaClient(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).build(), clock);
        add(Provider.KIMI, "https://api.moonshot.cn/v1/users/me/balance",
                environment.get("KIMI_API_KEY"), ProviderQuotaService::mapKimi);
        add(Provider.DEEPSEEK, "https://api.deepseek.com/user/balance",
                environment.get("DEEPSEEK_API_KEY"), ProviderQuotaService::mapDeepSeek);
        add(Provider.MINIMAX, "https://www.minimax.io/v1/token_plan/remains",
                environment.get("MINIMAX_TOKEN_PLAN_API_KEY"), ProviderQuotaService::mapMiniMax);
        String teamId = environment.get("XAI_TEAM_ID");
        if (teamId != null && teamId.matches("[A-Za-z0-9_-]+")) {
            add(Provider.GROK, "https://management-api.x.ai/v1/billing/teams/" + teamId + "/prepaid/balance",
                    environment.get("XAI_MANAGEMENT_API_KEY"), ProviderQuotaService::mapXai);
        }
    }

    private void add(Provider provider, String uri, String key, QuotaResponseMapper mapper) {
        endpoints.put(provider, new QuotaEndpoint(provider, URI.create(uri), key, mapper));
    }

    public ProviderQuotaSnapshot fetch(Provider provider) {
        QuotaEndpoint endpoint = endpoints.get(provider);
        if (endpoint == null) {
            throw new ProviderQuotaException("provider has no public balance/token-plan endpoint or required configuration", 0);
        }
        return client.fetch(endpoint);
    }

    public List<ProviderCapability> capabilities() {
        return List.of(
                new ProviderCapability(Provider.KIMI, CapabilityStatus.SUPPORTED,
                        CapabilityStatus.NOT_PUBLICLY_DOCUMENTED, CapabilityStatus.NOT_PUBLICLY_DOCUMENTED,
                        "GET /v1/users/me/balance returns available, voucher and cash balance.",
                        "https://platform.kimi.com/docs/api/balance"),
                new ProviderCapability(Provider.CLAUDE, CapabilityStatus.NOT_PUBLICLY_DOCUMENTED,
                        CapabilityStatus.NOT_PUBLICLY_DOCUMENTED, CapabilityStatus.USAGE_ONLY,
                        "Admin Usage and Cost API reports organization usage/cost; no public credit or Claude plan remaining endpoint.",
                        "https://platform.claude.com/docs/en/api/admin"),
                new ProviderCapability(Provider.GPT, CapabilityStatus.NOT_PUBLICLY_DOCUMENTED,
                        CapabilityStatus.NOT_PUBLICLY_DOCUMENTED, CapabilityStatus.USAGE_ONLY,
                        "Admin Usage/Costs endpoints report organization consumption; no public account credit or ChatGPT plan remaining endpoint.",
                        "https://platform.openai.com/docs/api-reference/usage"),
                new ProviderCapability(Provider.DEEPSEEK, CapabilityStatus.SUPPORTED,
                        CapabilityStatus.NOT_PUBLICLY_DOCUMENTED, CapabilityStatus.NOT_PUBLICLY_DOCUMENTED,
                        "GET /user/balance returns CNY/USD balance components.",
                        "https://api-docs.deepseek.com/api/get-user-balance/"),
                new ProviderCapability(Provider.MINIMAX, CapabilityStatus.NOT_PUBLICLY_DOCUMENTED,
                        CapabilityStatus.SUPPORTED, CapabilityStatus.NOT_PUBLICLY_DOCUMENTED,
                        "GET /v1/token_plan/remains returns Token Plan remaining quota.",
                        "https://platform.minimax.io/subscribe/token-plan"),
                new ProviderCapability(Provider.GROK, CapabilityStatus.SUPPORTED,
                        CapabilityStatus.NOT_PUBLICLY_DOCUMENTED, CapabilityStatus.USAGE_ONLY,
                        "Management API exposes team prepaid balance and historical usage; requires separate management key and team ID.",
                        "https://docs.x.ai/developers/rest-api-reference/management/billing"),
                new ProviderCapability(Provider.GLM, CapabilityStatus.NOT_PUBLICLY_DOCUMENTED,
                        CapabilityStatus.NOT_PUBLICLY_DOCUMENTED, CapabilityStatus.NOT_PUBLICLY_DOCUMENTED,
                        "No public official API endpoint located for cash balance or resource-package remaining quota.",
                        "https://docs.bigmodel.cn/"));
    }

    private static ProviderQuotaSnapshot mapKimi(Provider provider, Map<String, Object> body, Instant fetchedAt) {
        Map<String, Object> data = Json.child(body, "data");
        BigDecimal available = Json.decimal(data, "available_balance", null);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("cashBalance", Json.decimal(data, "cash_balance", null));
        details.put("voucherBalance", Json.decimal(data, "voucher_balance", null));
        return new ProviderQuotaSnapshot(provider, "ACCOUNT_BALANCE", available, "CNY", details, fetchedAt);
    }

    private static ProviderQuotaSnapshot mapDeepSeek(Provider provider, Map<String, Object> body, Instant fetchedAt) {
        List<Object> balances = Json.array(body, "balance_infos");
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("isAvailable", body.get("is_available"));
        details.put("balances", balances);
        BigDecimal available = null;
        String currency = "MULTI";
        if (balances.size() == 1 && balances.getFirst() instanceof Map<?, ?> raw) {
            @SuppressWarnings("unchecked") Map<String, Object> balance = (Map<String, Object>) raw;
            available = Json.decimal(balance, "total_balance", null);
            currency = Json.text(balance, "currency", false);
        }
        return new ProviderQuotaSnapshot(provider, "ACCOUNT_BALANCE", available, currency, details, fetchedAt);
    }

    private static ProviderQuotaSnapshot mapMiniMax(Provider provider, Map<String, Object> body, Instant fetchedAt) {
        // MiniMax response evolves with plan modalities. Preserve normalized type and full structured fields.
        return new ProviderQuotaSnapshot(provider, "TOKEN_PLAN_REMAINING", null, "REQUEST_OR_MODALITY_QUOTA",
                java.util.Collections.unmodifiableMap(new LinkedHashMap<>(body)), fetchedAt);
    }

    private static ProviderQuotaSnapshot mapXai(Provider provider, Map<String, Object> body, Instant fetchedAt) {
        BigDecimal cents = Json.decimal(Json.child(body, "total"), "val", null);
        BigDecimal dollars = cents == null ? null : cents.abs().movePointLeft(2);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("changes", Json.array(body, "changes"));
        details.put("rawSignedCents", cents);
        return new ProviderQuotaSnapshot(provider, "PREPAID_BALANCE", dollars, "USD", details, fetchedAt);
    }
}
