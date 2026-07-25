package tokenmonitor;


import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;

/** One HTTP implementation shared by every supported provider descriptor. */
final class HttpJsonQuotaClient {
    private final HttpClient http;
    private final Clock clock;

    HttpJsonQuotaClient(HttpClient http, Clock clock) {
        this.http = http;
        this.clock = clock;
    }

    ProviderQuotaSnapshot fetch(QuotaEndpoint endpoint) {
        if (endpoint.apiKey() == null || endpoint.apiKey().isBlank()) {
            throw new ProviderQuotaException("provider credential is not configured", 0);
        }
        HttpRequest request = HttpRequest.newBuilder(endpoint.uri())
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + endpoint.apiKey())
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ProviderQuotaException("provider quota request failed", response.statusCode());
            }
            Map<String, Object> body = Json.object(response.body());
            return endpoint.mapper().map(endpoint.provider(), body, clock.instant());
        } catch (ProviderQuotaException error) {
            throw error;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new ProviderQuotaException("provider quota request interrupted", 0);
        } catch (Exception error) {
            throw new ProviderQuotaException("provider quota request error: " + error.getMessage(), 0);
        }
    }
}
