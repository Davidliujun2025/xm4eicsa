package com.carepilot.platform.token;

import com.acme.aicslogin.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tokenmonitor.Provider;
import tokenmonitor.TokenUsageOverview;
import tokenmonitor.TokenUsagePage;
import tokenmonitor.TokenUsageService;
import tokenmonitor.TokenUsageStatus;
import tokenmonitor.TokenUsageSummary;
import tokenmonitor.TokenUsageEvent;
import tokenmonitor.TrendPoint;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/v1/token-usage/me")
public class TokenUsageController {
    private final TokenUsageService service;

    public TokenUsageController(TokenUsageService service) {
        this.service = service;
    }

    @GetMapping("/today")
    public TokenUsageSummary today(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.today(user.id().toString());
    }

    @GetMapping("/summary")
    public TokenUsageOverview summary(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.overview(user.id().toString());
    }

    @GetMapping("/status")
    public TokenUsageStatus status(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.status(user.id().toString());
    }

    @GetMapping("/records")
    public TokenUsagePage records(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model
    ) {
        Provider parsedProvider = provider == null || provider.isBlank() ? null : Provider.parse(provider);
        return service.recordsPage(
                user.id().toString(),
                Instant.EPOCH,
                Instant.now().plusSeconds(1),
                parsedProvider,
                model,
                page,
                size);
    }

    @GetMapping("/records/{requestId}")
    public TokenUsageEvent detail(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String requestId
    ) {
        return service.detail(user.id().toString(), requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "调用记录不存在"));
    }

    @GetMapping("/trend")
    public List<TrendPoint> trend(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "DAY") TokenUsageService.Bucket bucket
    ) {
        Instant end = to == null ? Instant.now().plusSeconds(1) : to;
        Instant start = from == null ? end.minus(7, ChronoUnit.DAYS) : from;
        return service.trend(user.id().toString(), start, end, bucket);
    }

    @GetMapping("/stream")
    public SseEmitter stream(@AuthenticationPrincipal AuthenticatedUser user) throws IOException {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        AutoCloseable subscription = service.subscribe(summary -> {
            try {
                if (user.id().toString().equals(summary.userId())) {
                    emitter.send(SseEmitter.event().name("usage").data(summary));
                }
            } catch (IOException error) {
                emitter.completeWithError(error);
            }
        });
        emitter.onCompletion(() -> close(subscription));
        emitter.onTimeout(() -> close(subscription));
        emitter.send(SseEmitter.event().name("usage").data(service.today(user.id().toString())));
        return emitter;
    }

    private void close(AutoCloseable subscription) {
        try {
            subscription.close();
        } catch (Exception ignored) {
            // Subscription cleanup must not fail the request thread.
        }
    }
}
