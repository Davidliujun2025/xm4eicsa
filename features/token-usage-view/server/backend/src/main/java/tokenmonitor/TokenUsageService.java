package tokenmonitor;


import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/** Shared aggregation logic used by REST and SSE; no controller-specific calculations. */
public final class TokenUsageService {
    public enum Bucket { HOUR, DAY }

    private final TokenUsageRepository repository;
    private final ZoneId businessZone;
    private final Clock clock;
    private final TokenQuotaResolver quotaResolver;
    private final BigDecimal highUsageThreshold;
    private final BigDecimal failureRateThreshold;
    private final CopyOnWriteArrayList<UsageUpdateListener> listeners = new CopyOnWriteArrayList<>();

    public TokenUsageService(TokenUsageRepository repository, ZoneId businessZone, Clock clock) {
        this(repository, businessZone, clock, 0, new BigDecimal("0.80"), new BigDecimal("0.20"));
    }

    public TokenUsageService(TokenUsageRepository repository, ZoneId businessZone, Clock clock,
                             long dailyTokenLimit, BigDecimal highUsageThreshold,
                             BigDecimal failureRateThreshold) {
        this(repository, businessZone, clock, ignored -> dailyTokenLimit,
                highUsageThreshold, failureRateThreshold);
        if (dailyTokenLimit < 0) throw new IllegalArgumentException("dailyTokenLimit must be >= 0");
    }

    public TokenUsageService(TokenUsageRepository repository, ZoneId businessZone, Clock clock,
                             TokenQuotaResolver quotaResolver, BigDecimal highUsageThreshold,
                             BigDecimal failureRateThreshold) {
        this.repository = repository;
        this.businessZone = businessZone;
        this.clock = clock;
        if (quotaResolver == null) throw new IllegalArgumentException("quotaResolver is required");
        validateThreshold(highUsageThreshold, "highUsageThreshold");
        validateThreshold(failureRateThreshold, "failureRateThreshold");
        this.quotaResolver = quotaResolver;
        this.highUsageThreshold = highUsageThreshold;
        this.failureRateThreshold = failureRateThreshold;
    }

    /** Stores once and publishes only after a new event is committed. */
    public boolean record(TokenUsageEvent event) {
        boolean inserted = repository.saveIfAbsent(event);
        if (inserted && !listeners.isEmpty()) {
            TokenUsageSummary summary = today(event.userId());
            for (UsageUpdateListener listener : listeners) listener.onUpdate(summary);
        }
        return inserted;
    }

    public AutoCloseable subscribe(UsageUpdateListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /** Publishes a fresh database-backed snapshot after another module writes the shared ledger. */
    public void publishCurrent(String userId) {
        if (listeners.isEmpty()) return;
        TokenUsageSummary summary = today(userId);
        for (UsageUpdateListener listener : listeners) listener.onUpdate(summary);
    }

    public TokenUsageSummary today(String userId) {
        LocalDate today = LocalDate.now(clock.withZone(businessZone));
        Instant from = today.atStartOfDay(businessZone).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(businessZone).toInstant();
        return summarize(userId, from, to);
    }

    public TokenUsageSummary summarize(String userId, Instant from, Instant to) {
        List<TokenUsageEvent> events = repository.find(userId, from, to, null, null, 0);
        long input = 0, output = 0, cached = 0, total = 0, ok = 0, failed = 0, responseMs = 0;
        for (TokenUsageEvent event : events) {
            input = Math.addExact(input, event.inputTokens());
            output = Math.addExact(output, event.outputTokens());
            cached = Math.addExact(cached, event.cachedInputTokens());
            total = Math.addExact(total, event.totalTokens());
            responseMs = Math.addExact(responseMs, event.responseTimeMs());
            if (event.status() == CallStatus.SUCCEEDED) ok++; else failed++;
        }
        long average = events.isEmpty() ? 0 : responseMs / events.size();
        return new TokenUsageSummary(userId, input, output, cached, total, ok, failed, average,
                from, to, clock.instant());
    }

    /** Fixed card aggregation: today, current week, current month and all history. */
    public TokenUsageOverview overview(String userId) {
        LocalDate today = LocalDate.now(clock.withZone(businessZone));
        Instant todayFrom = today.atStartOfDay(businessZone).toInstant();
        Instant tomorrow = today.plusDays(1).atStartOfDay(businessZone).toInstant();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthStart = today.withDayOfMonth(1);
        TokenUsageSummary todaySummary = summarize(userId, todayFrom, tomorrow);
        TokenUsageSummary weekSummary = summarize(userId,
                weekStart.atStartOfDay(businessZone).toInstant(), tomorrow);
        TokenUsageSummary monthSummary = summarize(userId,
                monthStart.atStartOfDay(businessZone).toInstant(), tomorrow);
        TokenUsageSummary historySummary = summarize(userId, Instant.EPOCH, tomorrow);
        long calls = Math.addExact(historySummary.successfulCalls(), historySummary.failedCalls());
        return new TokenUsageOverview(userId, todaySummary, weekSummary, monthSummary, historySummary,
                calls == 0, clock.instant());
    }

    public List<TokenUsageEvent> records(String userId, Instant from, Instant to,
                                         Provider provider, String model, int limit) {
        return repository.find(userId, from, to, provider, model, limit);
    }

    public TokenUsagePage recordsPage(String userId, Instant from, Instant to,
                                      Provider provider, String model, int page, int size) {
        if (page < 1) throw new IllegalArgumentException("page must be >= 1");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size must be between 1 and 100");
        long total = repository.count(userId, from, to, provider, model);
        int totalPages = total == 0 ? 0 : Math.toIntExact((total + size - 1) / size);
        int offset = Math.multiplyExact(page - 1, size);
        List<TokenUsageEvent> items = repository.findPage(userId, from, to, provider, model, offset, size);
        return new TokenUsagePage(items, page, size, total, totalPages, items.isEmpty());
    }

    public Optional<TokenUsageEvent> detail(String userId, String requestId) {
        return repository.findByRequestId(userId, requestId);
    }

    /** Reuses today's aggregate; no second token calculation path. */
    public TokenUsageStatus status(String userId) {
        TokenUsageSummary summary = today(userId);
        long calls = Math.addExact(summary.successfulCalls(), summary.failedCalls());
        BigDecimal failureRate = calls == 0 ? BigDecimal.ZERO : ratio(summary.failedCalls(), calls);
        boolean abnormal = summary.failedCalls() > 0 && failureRate.compareTo(failureRateThreshold) >= 0;
        Long resolvedLimit = quotaResolver.dailyLimit(userId);
        long dailyTokenLimit = resolvedLimit == null ? 0 : resolvedLimit;
        if (dailyTokenLimit < 0) throw new IllegalStateException("resolved daily token limit must be >= 0");
        if (dailyTokenLimit == 0) {
            return new TokenUsageStatus(userId, summary.totalTokens(), null, null, null,
                    TokenUsageStatus.Level.UNCONFIGURED, false,
                    summary.successfulCalls(), summary.failedCalls(), failureRate,
                    abnormal, abnormal ? "HIGH_FAILURE_RATE" : "NONE", clock.instant());
        }

        BigDecimal usageRate = ratio(summary.totalTokens(), dailyTokenLimit);
        boolean exhausted = usageRate.compareTo(BigDecimal.ONE) >= 0;
        boolean high = usageRate.compareTo(highUsageThreshold) >= 0;
        TokenUsageStatus.Level level = exhausted ? TokenUsageStatus.Level.EXHAUSTED
                : high ? TokenUsageStatus.Level.HIGH : TokenUsageStatus.Level.NORMAL;
        return new TokenUsageStatus(userId, summary.totalTokens(), dailyTokenLimit, usageRate,
                Math.max(0, dailyTokenLimit - summary.totalTokens()), level, high,
                summary.successfulCalls(), summary.failedCalls(), failureRate,
                abnormal, abnormal ? "HIGH_FAILURE_RATE" : "NONE", clock.instant());
    }

    public List<TrendPoint> trend(String userId, Instant from, Instant to, Bucket bucket) {
        List<TokenUsageEvent> events = repository.find(userId, from, to, null, null, 0);
        Map<Instant, MutableTrend> grouped = new LinkedHashMap<>();
        events.stream().sorted(java.util.Comparator.comparing(TokenUsageEvent::occurredAt)).forEach(event -> {
            ZonedDateTime local = event.occurredAt().atZone(businessZone);
            Instant start = bucket == Bucket.HOUR
                    ? local.truncatedTo(ChronoUnit.HOURS).toInstant()
                    : local.toLocalDate().atStartOfDay(businessZone).toInstant();
            MutableTrend point = grouped.computeIfAbsent(start, ignored -> new MutableTrend());
            point.input += event.inputTokens();
            point.output += event.outputTokens();
            point.total += event.totalTokens();
            point.calls++;
        });
        List<TrendPoint> result = new ArrayList<>();
        grouped.forEach((start, point) -> result.add(new TrendPoint(start, point.input, point.output, point.total, point.calls)));
        return List.copyOf(result);
    }

    /** Daily series with zero-filled gaps, suitable for 7/30-day line charts. */
    public List<TrendPoint> dailyTrend(String userId, Instant from, Instant to) {
        List<TrendPoint> existing = trend(userId, from, to, Bucket.DAY);
        Map<Instant, TrendPoint> byStart = new LinkedHashMap<>();
        for (TrendPoint point : existing) byStart.put(point.bucketStart(), point);
        LocalDate first = from.atZone(businessZone).toLocalDate();
        LocalDate last = to.minusNanos(1).atZone(businessZone).toLocalDate();
        List<TrendPoint> result = new ArrayList<>();
        for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
            Instant start = date.atStartOfDay(businessZone).toInstant();
            result.add(byStart.getOrDefault(start, new TrendPoint(start, 0, 0, 0, 0)));
        }
        return List.copyOf(result);
    }

    public TrendSeries dailyTrendSeries(String userId, int days) {
        if (days != 7 && days != 30) throw new IllegalArgumentException("days must be 7 or 30");
        LocalDate today = LocalDate.now(clock.withZone(businessZone));
        Instant from = today.minusDays(days - 1L).atStartOfDay(businessZone).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(businessZone).toInstant();
        return dailyTrendSeries(userId, from, to);
    }

    public TrendSeries dailyTrendSeries(String userId, Instant from, Instant to) {
        List<TrendPoint> items = dailyTrend(userId, from, to);
        boolean empty = items.stream().allMatch(point -> point.calls() == 0);
        return new TrendSeries(userId, from, to, items, empty);
    }

    private static final class MutableTrend {
        long input;
        long output;
        long total;
        long calls;
    }

    private static BigDecimal ratio(long numerator, long denominator) {
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private static void validateThreshold(BigDecimal threshold, String name) {
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) <= 0 || threshold.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(name + " must be > 0 and <= 1");
        }
    }
}
