#include "token_monitor_client.hpp"

#include <cassert>
#include <chrono>
#include <string>

namespace {

class FakeTransport final : public token_monitor::HttpTransport {
public:
    token_monitor::HttpRequest last;
    token_monitor::HttpResponse response{201, "{\"accepted\":true,\"duplicate\":false}"};

    token_monitor::HttpResponse send(const token_monitor::HttpRequest& request) override {
        last = request;
        return response;
    }
};

}  // namespace

int main() {
    FakeTransport transport;
    token_monitor::TokenMonitorClient client{transport, "service-key"};
    token_monitor::TokenUsageEvent event{
        .idempotency_key = "provider-request-1",
        .user_id = "agent-7",
        .conversation_id = "conversation-3",
        .request_id = "request-1",
        .provider = token_monitor::Provider::deepseek,
        .model = "deepseek-chat",
        .input_content_length = 420,
        .output_content_length = 110,
        .input_tokens = 100,
        .output_tokens = 25,
        .cached_input_tokens = 10,
        .total_tokens = 125,
        .response_time_ms = 310,
        .status = token_monitor::CallStatus::succeeded,
        .occurred_at = std::chrono::system_clock::from_time_t(0),
        .provider_reported_cost = "0.0012",
        .cost_currency = "USD",
    };
    const auto result = client.report(event);
    assert(result.accepted);
    assert(!result.duplicate);
    assert(transport.last.path == "/api/v1/internal/token-usage/events");
    assert(transport.last.headers.at("X-Internal-Api-Key") == "service-key");
    assert(transport.last.body.find("\"provider\":\"DEEPSEEK\"") != std::string::npos);
    assert(transport.last.body.find("\"inputContentLength\":420") != std::string::npos);
    assert(transport.last.body.find("\"totalTokens\":125") != std::string::npos);
}
