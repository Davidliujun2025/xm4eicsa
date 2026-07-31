package com.carepilot.chatworkbench.controller;

import com.acme.aicslogin.security.AuthenticatedUser;
import com.carepilot.chatworkbench.dto.request.FavoriteScriptRequest;
import com.carepilot.chatworkbench.dto.response.ApiResponse;
import com.carepilot.chatworkbench.dto.response.FavoriteScriptResponse;
import com.carepilot.chatworkbench.service.FavoriteScriptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteScriptController {

    private final FavoriteScriptService favoriteScriptService;

    @PostMapping
    public ResponseEntity<ApiResponse<FavoriteScriptResponse>> favoriteScript(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody FavoriteScriptRequest request) {
        String customerId = user.id().toString();
        log.info("Favorite script: customerId={}, scriptStep={}", customerId, request.getScriptStep());
        FavoriteScriptResponse response = favoriteScriptService.favoriteScript(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("已收藏到话术库", response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> unfavoriteScript(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, String> request) {
        String customerId = user.id().toString();
        String scriptHash = request.get("scriptHash");
        log.info("Unfavorite script: customerId={}, scriptHash={}", customerId, scriptHash);
        favoriteScriptService.unfavoriteScript(customerId, scriptHash);
        return ResponseEntity.ok(ApiResponse.success("已取消收藏", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FavoriteScriptResponse>>> getFavoriteScripts(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(value = "platform", required = false) String platform) {
        String customerId = user.id().toString();
        log.info("Get favorite scripts: customerId={}, platform={}", customerId, platform);
        List<FavoriteScriptResponse> favorites;
        if (platform != null && !platform.isEmpty()) {
            favorites = favoriteScriptService.getFavoriteScriptsByPlatform(customerId, platform);
        } else {
            favorites = favoriteScriptService.getFavoriteScripts(customerId);
        }
        return ResponseEntity.ok(ApiResponse.success(favorites));
    }

    @PostMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> checkFavorited(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, String> request) {
        String customerId = user.id().toString();
        String scriptContent = request.get("scriptContent");
        boolean isFavorited = favoriteScriptService.isFavorited(customerId, scriptContent);
        return ResponseEntity.ok(ApiResponse.success(isFavorited));
    }

    @PostMapping("/hash")
    public ResponseEntity<ApiResponse<String>> generateHash(
            @RequestBody Map<String, String> request) {
        String scriptContent = request.get("scriptContent");
        String hash = favoriteScriptService.generateScriptHash(scriptContent);
        return ResponseEntity.ok(ApiResponse.success(hash));
    }
}
