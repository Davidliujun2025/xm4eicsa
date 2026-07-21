package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.dto.request.FavoriteScriptRequest;
import com.carepilot.chatworkbench.dto.response.FavoriteScriptResponse;
import com.carepilot.chatworkbench.entity.FavoriteScript;
import com.carepilot.chatworkbench.repository.FavoriteScriptRepository;
import com.carepilot.chatworkbench.util.ScriptHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteScriptService {

    private final FavoriteScriptRepository favoriteScriptRepository;

    @Transactional
    public FavoriteScriptResponse favoriteScript(String customerId, FavoriteScriptRequest request) {
        String scriptHash = ScriptHashUtil.generateHash(request.getScriptContent());

        if (favoriteScriptRepository.existsByCustomerIdAndScriptHash(customerId, scriptHash)) {
            throw new IllegalStateException("该话术已被收藏");
        }

        FavoriteScript favorite = FavoriteScript.builder()
                .customerId(customerId)
                .conversationId(request.getConversationId())
                .platform(request.getPlatform())
                .customerType(request.getCustomerType())
                .scriptStep(request.getScriptStep())
                .scriptContent(request.getScriptContent())
                .scriptHash(scriptHash)
                .question(request.getQuestion())
                .build();

        FavoriteScript saved = favoriteScriptRepository.save(favorite);
        log.info("Script favorited: customerId={}, scriptStep={}", customerId, request.getScriptStep());
        return toResponse(saved);
    }

    @Transactional
    public void unfavoriteScript(String customerId, String scriptHash) {
        if (!favoriteScriptRepository.existsByCustomerIdAndScriptHash(customerId, scriptHash)) {
            throw new IllegalStateException("该话术未被收藏");
        }

        favoriteScriptRepository.deleteByCustomerIdAndScriptHash(customerId, scriptHash);
        log.info("Script unfavorited: customerId={}, scriptHash={}", customerId, scriptHash);
    }

    public boolean isFavorited(String customerId, String scriptContent) {
        String scriptHash = ScriptHashUtil.generateHash(scriptContent);
        return favoriteScriptRepository.existsByCustomerIdAndScriptHash(customerId, scriptHash);
    }

    public String generateScriptHash(String scriptContent) {
        return ScriptHashUtil.generateHash(scriptContent);
    }

    public List<FavoriteScriptResponse> getFavoriteScripts(String customerId) {
        return favoriteScriptRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<FavoriteScriptResponse> getFavoriteScriptsByPlatform(String customerId, String platform) {
        return favoriteScriptRepository.findByCustomerIdAndPlatformOrderByCreatedAtDesc(customerId, platform)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private FavoriteScriptResponse toResponse(FavoriteScript favorite) {
        return FavoriteScriptResponse.builder()
                .id(favorite.getId())
                .conversationId(favorite.getConversationId())
                .platform(favorite.getPlatform())
                .customerType(favorite.getCustomerType())
                .scriptStep(favorite.getScriptStep())
                .scriptContent(favorite.getScriptContent())
                .question(favorite.getQuestion())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}