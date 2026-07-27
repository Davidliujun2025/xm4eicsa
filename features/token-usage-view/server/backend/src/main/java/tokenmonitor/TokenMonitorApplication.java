package tokenmonitor;


import javax.sql.DataSource;
import java.time.Clock;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public final class TokenMonitorApplication {
    private TokenMonitorApplication() {}

    public static void main(String[] args) throws Exception {
        Map<String, String> env = System.getenv();
        String host = env.getOrDefault("TOKEN_MONITOR_HOST", "0.0.0.0");
        int port = Integer.parseInt(env.getOrDefault("TOKEN_MONITOR_PORT", "8080"));
        ZoneId zone = ZoneId.of(env.getOrDefault("TOKEN_MONITOR_ZONE", "Asia/Shanghai"));
        String internalKey = env.getOrDefault("TOKEN_MONITOR_INTERNAL_API_KEY", "");
        if (internalKey.isBlank()) {
            throw new IllegalStateException("TOKEN_MONITOR_INTERNAL_API_KEY must be configured");
        }

        Clock clock = Clock.systemUTC();
        long dailyTokenLimit = Long.parseLong(env.getOrDefault("TOKEN_MONITOR_DAILY_TOKEN_LIMIT", "0"));
        DataSource dataSource = dataSource(env);
        TokenUsageRepository repository = dataSource == null
                ? new InMemoryTokenUsageRepository()
                : new JdbcTokenUsageRepository(dataSource);
        TokenQuotaResolver quotaResolver = dataSource == null
                ? ignored -> dailyTokenLimit
                : new JdbcTokenQuotaResolver(dataSource, dailyTokenLimit);
        BigDecimal highUsageThreshold = new BigDecimal(env.getOrDefault("TOKEN_MONITOR_HIGH_USAGE_THRESHOLD", "0.80"));
        BigDecimal failureRateThreshold = new BigDecimal(env.getOrDefault("TOKEN_MONITOR_FAILURE_RATE_THRESHOLD", "0.20"));
        TokenUsageService usage = new TokenUsageService(repository, zone, clock,
                quotaResolver, highUsageThreshold, failureRateThreshold);
        ProviderQuotaService quotas = new ProviderQuotaService(env, clock);
        String uiRoot = env.getOrDefault("TOKEN_MONITOR_UI_ROOT", "../../token-usage-view/ui");
        TokenMonitorHttpServer server = new TokenMonitorHttpServer(host, port, usage, quotas, internalKey, uiRoot);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        server.start();
        System.out.printf("Personal Token Monitor listening on http://%s:%d%n", host, server.port());
        new CountDownLatch(1).await();
    }

    private static DataSource dataSource(Map<String, String> env) {
        String jdbcUrl = env.get("TOKEN_MONITOR_JDBC_URL");
        if (jdbcUrl == null || jdbcUrl.isBlank()) return null;
        return new DriverManagerDataSource(
                jdbcUrl,
                env.getOrDefault("TOKEN_MONITOR_JDBC_USER", ""),
                env.getOrDefault("TOKEN_MONITOR_JDBC_PASSWORD", ""));
    }
}
