package com.carepilot.chatworkbench.controller;

import com.carepilot.chatworkbench.dto.request.CreateConversationRequest;
import com.carepilot.chatworkbench.dto.response.ApiResponse;
import com.carepilot.chatworkbench.dto.response.ConversationResponse;
import com.carepilot.chatworkbench.service.AIService;
import com.carepilot.chatworkbench.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final AIService aiService;
    private final ConversationService conversationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversationList(
            @RequestHeader("X-Customer-Id") String customerId) {
        log.info("Get conversation list: customerId={}", customerId);
        List<ConversationResponse> conversations = conversationService.getConversationList(customerId);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversationById(
            @RequestHeader("X-Customer-Id") String customerId,
            @PathVariable("id") String conversationId) {
        log.info("Get conversation by id: customerId={}, conversationId={}", customerId, conversationId);
        ConversationResponse conversation = conversationService.getConversationById(customerId, conversationId);
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @RequestHeader("X-Customer-Id") String customerId,
            @Valid @RequestBody CreateConversationRequest request) {
        log.info("Create conversation: customerId={}, question={}, platform={}",
                customerId, request.getQuestion(), request.getPlatform());
        ConversationResponse response = aiService.createConversation(
                customerId,
                request.getQuestion(),
                request.getPlatform(),
                request.getCustomerType()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<ConversationResponse>> addMessage(
            @RequestHeader("X-Customer-Id") String customerId,
            @PathVariable("id") String conversationId,
            @Valid @RequestBody CreateConversationRequest request) {
        log.info("Add message to conversation: customerId={}, conversationId={}, question={}",
                customerId, conversationId, request.getQuestion());
        ConversationResponse response = aiService.addMessage(
                customerId,
                conversationId,
                request.getQuestion(),
                request.getCustomerType()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @RequestHeader("X-Customer-Id") String customerId,
            @PathVariable("id") String conversationId) {
        log.info("Delete conversation: customerId={}, conversationId={}", customerId, conversationId);
        conversationService.deleteConversation(customerId, conversationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
