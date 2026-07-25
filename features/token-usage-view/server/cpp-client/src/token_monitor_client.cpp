#include "token_monitor_client.hpp"

#include <ctime>
#include <iomanip>
#include <sstream>
#include <stdexcept>
#include <utility>

namespace token_monitor {
namespace {

std::string provider_name(const Provider provider) {
    switch (provider) {
        case Provider::kimi: return "KIMI";
        case Provider::claude: return "CLAUDE";
        case Provider::gpt: return "GPT";
        case Provider::deepseek: return "DEEPSEEK";
        case Provider::minimax: return "MINIMAX";
        case Provider::grok: return "GROK";
        case Provider::glm: return "GLM";
        case Provider::other: return "OTHER";
    }
    throw std::logic_error("unhandled provider");
}

std::string status_name(const CallStatus status) {
    switch (status) {
        case CallStatus::succeeded: return "SUCCEEDED";
        case CallStatus::failed: return "FAILED";
        case CallStatus::cancelled: return "CANCELLED";
        case CallStatus::timeout: return "TIMEOUT";
    }
    throw std::logic_error("unhandled call status");
}

std::string escape_json(const std::string& value) {
    std::ostringstream out;
    for (const unsigned char c : value) {
        switch (c) {
            case '"': out << "\\\""; break;
            case '\\': out << "\\\\"; break;
            case '\b': out << "\\b"; break;
            case '\f': out << "\\f"; break;
            case '\n': out << "\\n"; break;
            case '\r': out << "\\r"; break;
            case '\t': out << "\\t"; break;
            default:
                if (c < 0x20U) out << "\\u" << std::hex << std::setw(4) << std::setfill('0') << static_cast<int>(c);
                else out << static_cast<char>(c);
        }
    }
    return out.str();
}

std::string iso8601(const std::chrono::system_clock::time_point point) {
    const std::time_t value = std::chrono::system_clock::to_time_t(point);
    std::tm utc{};
#if defined(_WIN32)
    gmtime_s(&utc, &value);
#else
    gmtime_r(&value, &utc);
#endif
    std::ostringstream out;
    out << std::put_time(&utc, "%Y-%m-%dT%H:%M:%SZ");
    return out.str();
}

void validate(const TokenUsageEvent& event) {
    if (event.idempotency_key.empty() || event.user_id.empty() || event.request_id.empty() || event.model.empty()) {
        throw std::invalid_argument("idempotency_key, user_id, request_id and model are required");
    }
    if (event.input_content_length < 0 || event.output_content_length < 0 ||
        event.input_tokens < 0 || event.output_tokens < 0 || event.cached_input_tokens < 0 ||
        event.total_tokens < event.input_tokens + event.output_tokens || event.response_time_ms < 0) {
        throw std::invalid_argument("invalid token usage values");
    }
}

}  // namespace

TokenMonitorClient::TokenMonitorClient(HttpTransport& transport, std::string internal_api_key)
    : transport_(transport), internal_api_key_(std::move(internal_api_key)) {
    if (internal_api_key_.empty()) throw std::invalid_argument("internal_api_key is required");
}

ReportResult TokenMonitorClient::report(const TokenUsageEvent& event) const {
    validate(event);
    const HttpRequest request{
        .method = "POST",
        .path = "/api/v1/internal/token-usage/events",
        .headers = {{"Content-Type", "application/json"}, {"X-Internal-Api-Key", internal_api_key_}},
        .body = to_json(event),
        .timeout = std::chrono::milliseconds{5'000},
    };
    const HttpResponse response = transport_.send(request);
    const bool accepted = response.status >= 200 && response.status < 300;
    const bool duplicate = accepted && response.body.find("\"duplicate\":true") != std::string::npos;
    return ReportResult{accepted, duplicate, response.status, accepted ? "" : response.body};
}

std::string TokenMonitorClient::to_json(const TokenUsageEvent& event) {
    validate(event);
    std::ostringstream out;
    out << '{'
        << "\"idempotencyKey\":\"" << escape_json(event.idempotency_key) << "\","
        << "\"userId\":\"" << escape_json(event.user_id) << "\","
        << "\"conversationId\":\"" << escape_json(event.conversation_id) << "\","
        << "\"requestId\":\"" << escape_json(event.request_id) << "\","
        << "\"provider\":\"" << provider_name(event.provider) << "\","
        << "\"model\":\"" << escape_json(event.model) << "\","
        << "\"inputContentLength\":" << event.input_content_length << ','
        << "\"outputContentLength\":" << event.output_content_length << ','
        << "\"inputTokens\":" << event.input_tokens << ','
        << "\"outputTokens\":" << event.output_tokens << ','
        << "\"cachedInputTokens\":" << event.cached_input_tokens << ','
        << "\"totalTokens\":" << event.total_tokens << ','
        << "\"responseTimeMs\":" << event.response_time_ms << ','
        << "\"status\":\"" << status_name(event.status) << "\","
        << "\"occurredAt\":\"" << iso8601(event.occurred_at) << "\","
        << "\"providerReportedCost\":";
    if (event.provider_reported_cost.has_value()) out << *event.provider_reported_cost;
    else out << '0';
    out << ",\"costCurrency\":\"" << escape_json(event.cost_currency) << "\"}";
    return out.str();
}

}  // namespace token_monitor
