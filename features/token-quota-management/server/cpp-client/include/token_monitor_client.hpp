#pragma once

#include <chrono>
#include <cstdint>
#include <map>
#include <optional>
#include <string>

namespace token_monitor {

enum class Provider { kimi, claude, gpt, deepseek, minimax, grok, glm, other };
enum class CallStatus { succeeded, failed, cancelled, timeout };

/**
 * Exact usage returned by one provider call.
 * Do not estimate tokens locally. Use provider response fields after stream completion.
 */
struct TokenUsageEvent {
    std::string idempotency_key;
    std::string user_id;
    std::string conversation_id;
    std::string request_id;
    Provider provider{Provider::other};
    std::string model;
    std::int64_t input_content_length{};
    std::int64_t output_content_length{};
    std::int64_t input_tokens{};
    std::int64_t output_tokens{};
    std::int64_t cached_input_tokens{};
    std::int64_t total_tokens{};
    std::int64_t response_time_ms{};
    CallStatus status{CallStatus::failed};
    std::chrono::system_clock::time_point occurred_at;
    std::optional<std::string> provider_reported_cost;
    std::string cost_currency{"UNKNOWN"};
};

struct HttpRequest {
    std::string method;
    std::string path;
    std::map<std::string, std::string> headers;
    std::string body;
    std::chrono::milliseconds timeout{5'000};
};

struct HttpResponse {
    int status{};
    std::string body;
};

/**
 * Transport port. Adapt the workbench's existing HTTP stack here; this library does not force
 * libcurl, Boost.Beast, WinHTTP, or another networking dependency.
 */
class HttpTransport {
public:
    virtual ~HttpTransport() = default;
    virtual HttpResponse send(const HttpRequest& request) = 0;
};

struct ReportResult {
    bool accepted{};
    bool duplicate{};
    int http_status{};
    std::string error;
};

class TokenMonitorClient final {
public:
    TokenMonitorClient(HttpTransport& transport, std::string internal_api_key);

    /**
     * Reports to POST /api/v1/internal/token-usage/events.
     * Safe to retry with the same idempotency_key; server will not double count.
     */
    [[nodiscard]] ReportResult report(const TokenUsageEvent& event) const;

    /** Exposed for contract tests and custom transports. */
    [[nodiscard]] static std::string to_json(const TokenUsageEvent& event);

private:
    HttpTransport& transport_;
    std::string internal_api_key_;
};

}  // namespace token_monitor
