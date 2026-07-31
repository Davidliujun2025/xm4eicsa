package com.example.aics.controller;

import com.acme.aicslogin.security.AuthenticatedUser;
import com.example.aics.dto.FavoriteResponse;
import com.example.aics.dto.PageResponse;
import com.example.aics.dto.ToggleFavoriteRequest;
import com.example.aics.dto.ToggleFavoriteResponse;
import com.example.aics.service.ScriptFavoriteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/script-favorites")
public class ScriptFavoriteController {

    private final ScriptFavoriteService service;

    public ScriptFavoriteController(ScriptFavoriteService service) {
        this.service = service;
    }

    @PostMapping("/toggle")
    public ToggleFavoriteResponse toggle(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ToggleFavoriteRequest request
    ) {
        return service.toggle(user.id(), request);
    }

    @GetMapping
    public PageResponse<FavoriteResponse> search(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.search(user.id(), keyword, tag, page, size);
    }

    @PostMapping("/{favoriteId}/use")
    public FavoriteResponse markUsed(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long favoriteId
    ) {
        return service.markUsed(user.id(), favoriteId);
    }

    @GetMapping("/tags")
    public List<String> listTags(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.listTags(user.id());
    }

    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long favoriteId
    ) {
        service.delete(user.id(), favoriteId);
        return ResponseEntity.noContent().build();
    }
}
