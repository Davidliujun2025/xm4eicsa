package com.example.aics.controller;

import com.example.aics.dto.FavoriteResponse;
import com.example.aics.dto.PageResponse;
import com.example.aics.dto.ToggleFavoriteRequest;
import com.example.aics.dto.ToggleFavoriteResponse;
import com.example.aics.service.ScriptFavoriteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/script-favorites")
public class ScriptFavoriteController {

    private final ScriptFavoriteService service;

    public ScriptFavoriteController(ScriptFavoriteService service) {
        this.service = service;
    }

    @PostMapping("/toggle")
    public ToggleFavoriteResponse toggle(
            @RequestHeader(value = "X-Staff-Id", required = false) String staffId,
            @Valid @RequestBody ToggleFavoriteRequest request
    ) {
        return service.toggle(staffId, request);
    }

    @GetMapping
    public PageResponse<FavoriteResponse> search(
            @RequestHeader(value = "X-Staff-Id", required = false) String staffId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.search(staffId, keyword, tag, page, size);
    }

    @PostMapping("/{favoriteId}/use")
    public FavoriteResponse markUsed(
            @RequestHeader(value = "X-Staff-Id", required = false) String staffId,
            @PathVariable UUID favoriteId
    ) {
        return service.markUsed(staffId, favoriteId);
    }

    @GetMapping("/tags")
    public List<String> listTags(@RequestHeader(value = "X-Staff-Id", required = false) String staffId) {
        return service.listTags(staffId);
    }

    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = "X-Staff-Id", required = false) String staffId,
            @PathVariable UUID favoriteId
    ) {
        service.delete(staffId, favoriteId);
        return ResponseEntity.noContent().build();
    }
}
