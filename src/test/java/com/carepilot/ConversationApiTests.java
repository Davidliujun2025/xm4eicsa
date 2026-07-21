package com.carepilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 客服系统核心接口测试用例
 *
 * 测试范围：对话模块（Conversation）全部 5 个 API：
 *   3.1 查询对话历史列表
 *   3.2 查询单条对话详情
 *   3.3 创建对话并生成第 1 步
 *   3.4 生成下一步骤（2-5）
 *   3.5 静默保存编辑内容
 *
 * 分工：后端 carepilot-server，负责对话五步法流程 + AI 生成调度
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConversationApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long createdConversationId;

    // ============================================================
    // 3.1 查询对话历史列表
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("3.1-正常：默认分页查询，返回第1页20条")
    void listDefaultPage() throws Exception {
        mockMvc.perform(get("/api/v1/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @Order(2)
    @DisplayName("3.1-正常：自定义分页，第2页5条")
    void listCustomPage() throws Exception {
        mockMvc.perform(get("/api/v1/conversations")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(5));
    }

    @Test
    @Order(3)
    @DisplayName("3.1-正常：关键词搜索")
    void listWithKeyword() throws Exception {
        mockMvc.perform(get("/api/v1/conversations")
                        .param("keyword", "退货"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray());
        // 注：因 Stub AI 不按关键词过滤，仅验证接口不报错
    }

    @Test
    @Order(4)
    @DisplayName("3.1-边界：page=1 pageSize=1 只返回1条")
    void listMinimalPage() throws Exception {
        mockMvc.perform(get("/api/v1/conversations")
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(1));
    }

    // ============================================================
    // 3.3 创建对话并生成第 1 步
    // ============================================================

    @Test
    @Order(5)
    @DisplayName("3.3-正常：创建对话，返回完整五步法第1步")
    void createConversationNormal() throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("platform", "淘宝");
        body.put("question", "客户收到货后发现商品有划痕，要求退货退款");

        String resp = mockMvc.perform(post("/api/v1/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.platform").value("淘宝"))
                .andExpect(jsonPath("$.data.status").value("generating"))
                .andExpect(jsonPath("$.data.steps").isArray())
                .andExpect(jsonPath("$.data.steps.length()").value(1))
                .andExpect(jsonPath("$.data.steps[0].stepType").value("意图识别"))
                .andExpect(jsonPath("$.data.steps[0].stepOrder").value(1))
                .andExpect(jsonPath("$.data.tokenInfo.totalLimit").value(50000))
                .andReturn()
                .getResponse()
                .getContentAsString();

        createdConversationId = objectMapper.readTree(resp)
                .get("data").get("id").asLong();
    }

    @Test
    @Order(6)
    @DisplayName("3.3-正常：不同平台创建对话")
    void createConversationDifferentPlatforms() throws Exception {
        for (String platform : List.of("京东", "拼多多", "抖音", "天猫")) {
            Map<String, String> body = new LinkedHashMap<>();
            body.put("platform", platform);
            body.put("question", "物流信息三天没更新了，客户很着急");

            mockMvc.perform(post("/api/v1/conversations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.platform").value(platform));
        }
    }

    @Test
    @Order(7)
    @DisplayName("3.3-校验：platform 为空时返回参数错误")
    void createConversationNoPlatform() throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("question", "客户要求退货");

        mockMvc.perform(post("/api/v1/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    @Order(8)
    @DisplayName("3.3-校验：question 为空时返回参数错误")
    void createConversationNoQuestion() throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("platform", "淘宝");

        mockMvc.perform(post("/api/v1/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    @Order(9)
    @DisplayName("3.3-边界：question 超过 500 字符时返回参数错误")
    void createConversationQuestionTooLong() throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("platform", "淘宝");
        body.put("question", "A".repeat(501));

        mockMvc.perform(post("/api/v1/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    @Order(10)
    @DisplayName("3.3-边界：question 恰好 500 字符，创建成功")
    void createConversationQuestionExactly500() throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("platform", "京东");
        body.put("question", "Q".repeat(500));

        mockMvc.perform(post("/api/v1/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ============================================================
    // 3.2 查询单条对话详情
    // ============================================================

    @Test
    @Order(11)
    @DisplayName("3.2-正常：根据 ID 查询已创建的对话详情")
    void getConversationDetail() throws Exception {
        mockMvc.perform(get("/api/v1/conversations/" + createdConversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(createdConversationId))
                .andExpect(jsonPath("$.data.steps").isArray())
                .andExpect(jsonPath("$.data.steps[0].editable").value(true));
    }

    @Test
    @Order(12)
    @DisplayName("3.2-边界：查询不存在的对话返回业务异常")
    void getConversationNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/conversations/99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40401))
                .andExpect(jsonPath("$.message").value("对话不存在"));
    }

    // ============================================================
    // 3.4 生成下一步骤
    // ============================================================

    @Test
    @Order(13)
    @DisplayName("3.4-正常：依次生成第2~5步，第5步后状态变为 success")
    void generateAllSteps() throws Exception {
        // 生成第 2 步
        mockMvc.perform(post("/api/v1/conversations/" + createdConversationId + "/generate-next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.stepOrder").value(2))
                .andExpect(jsonPath("$.data.stepType").value("回复策略"));

        // 生成第 3 步
        mockMvc.perform(post("/api/v1/conversations/" + createdConversationId + "/generate-next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stepOrder").value(3))
                .andExpect(jsonPath("$.data.stepType").value("推荐话术"));

        // 生成第 4 步
        mockMvc.perform(post("/api/v1/conversations/" + createdConversationId + "/generate-next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stepOrder").value(4))
                .andExpect(jsonPath("$.data.stepType").value("钩子引导"));

        // 生成第 5 步（最后一步，status 变为 success）
        mockMvc.perform(post("/api/v1/conversations/" + createdConversationId + "/generate-next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stepOrder").value(5))
                .andExpect(jsonPath("$.data.stepType").value("成功收尾"));

        // 验证详情中 status 已变为 success，且包含全部 5 步
        mockMvc.perform(get("/api/v1/conversations/" + createdConversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("success"))
                .andExpect(jsonPath("$.data.steps.length()").value(5));
    }

    @Test
    @Order(14)
    @DisplayName("3.4-边界：对话不存在时返回业务异常")
    void generateNextNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/conversations/99999/generate-next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    @Order(15)
    @DisplayName("3.4-边界：已完成五步法后再生成返回限制异常")
    void generateNextExceedsFive() throws Exception {
        // 五步法完成后 status=success，不可继续生成
        mockMvc.perform(post("/api/v1/conversations/" + createdConversationId + "/generate-next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40003))
                .andExpect(jsonPath("$.message").value("当前对话状态不允许继续生成"));
    }

    // ============================================================
    // 3.5 静默保存编辑内容
    // ============================================================

    @Test
    @Order(16)
    @DisplayName("3.5-正常：编辑步骤的 body/label/tags 并保存")
    void saveStepsNormal() throws Exception {
        // 先拿 step1 的 id
        String detailJson = mockMvc.perform(get("/api/v1/conversations/" + createdConversationId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long stepId = objectMapper.readTree(detailJson)
                .get("data").get("steps").get(0).get("stepId").asLong();

        Map<String, Object> stepItem = new LinkedHashMap<>();
        stepItem.put("stepId", stepId);
        stepItem.put("stepOrder", 1);
        stepItem.put("body", "客服应首先安抚客户情绪，核实订单和商品信息，然后提供退货流程指引。");
        stepItem.put("label", "已优化");
        stepItem.put("tags", List.of("售后", "退货", "安抚"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("steps", List.of(stepItem));

        mockMvc.perform(put("/api/v1/conversations/" + createdConversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证保存结果
        mockMvc.perform(get("/api/v1/conversations/" + createdConversationId))
                .andExpect(jsonPath("$.data.steps[0].body").value("客服应首先安抚客户情绪，核实订单和商品信息，然后提供退货流程指引。"))
                .andExpect(jsonPath("$.data.steps[0].label").value("已优化"))
                .andExpect(jsonPath("$.data.steps[0].tags[0]").value("售后"))
                .andExpect(jsonPath("$.data.steps[0].tags.length()").value(3));
    }

    @Test
    @Order(17)
    @DisplayName("3.5-校验：steps 为空时返回参数错误")
    void saveStepsEmpty() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("steps", List.of());

        mockMvc.perform(put("/api/v1/conversations/" + createdConversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    @Order(18)
    @DisplayName("3.5-边界：保存到不存在的对话返回业务异常")
    void saveStepsNotFound() throws Exception {
        Map<String, Object> stepItem = new LinkedHashMap<>();
        stepItem.put("stepId", 1L);
        stepItem.put("stepOrder", 1);
        stepItem.put("body", "内容");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("steps", List.of(stepItem));

        mockMvc.perform(put("/api/v1/conversations/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40401));
    }

    // ============================================================
    // 全局错误处理
    // ============================================================

    @Test
    @Order(19)
    @DisplayName("全局-根路径返回 404 而非 500")
    void rootReturns404() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @Order(20)
    @DisplayName("全局-不存在的路径返回 404")
    void notFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
