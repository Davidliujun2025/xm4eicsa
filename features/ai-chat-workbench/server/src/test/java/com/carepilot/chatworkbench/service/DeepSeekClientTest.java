package com.carepilot.chatworkbench.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DeepSeekClientTest {

    @Test
    void sendsIntentPromptAndReturnsActualUsage() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DeepSeekClient client = new DeepSeekClient(
                builder, "https://api.deepseek.com", "test-key",
                "deepseek-v4-flash", 1400, false);

        server.expect(requestTo("https://api.deepseek.com/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "model": "deepseek-v4-flash",
                          "thinking": {"type": "disabled"},
                          "stream": false,
                          "max_tokens": 1400
                        }
                        """, false))
                .andRespond(withSuccess("""
                        {
                          "id": "request-1",
                          "model": "deepseek-v4-flash",
                          "choices": [{
                            "message": {
                              "role": "assistant",
                              "content": "消费者情绪/心情：平和"
                            }
                          }],
                          "usage": {
                            "prompt_tokens": 121,
                            "completion_tokens": 53,
                            "total_tokens": 174
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        DeepSeekClient.DeepSeekResult result = client.recognizeIntent("意图识别提示词");

        assertThat(result.content()).isEqualTo("消费者情绪/心情：平和");
        assertThat(result.promptTokens()).isEqualTo(121);
        assertThat(result.completionTokens()).isEqualTo(53);
        assertThat(result.totalTokens()).isEqualTo(174);
        assertThat(result.requestId()).isEqualTo("request-1");
        server.verify();
    }

    @Test
    void refusesToCallApiWithoutKey() {
        DeepSeekClient client = new DeepSeekClient(
                RestClient.builder(), "https://api.deepseek.com", " ",
                "deepseek-v4-flash", 1400, false);

        assertThatThrownBy(() -> client.recognizeIntent("prompt"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEEPSEEK_API_KEY");
    }
}
