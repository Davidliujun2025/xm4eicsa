package com.example.aics.service;

import com.example.aics.dto.FavoriteResponse;
import com.example.aics.dto.PageResponse;
import com.example.aics.dto.ToggleFavoriteRequest;
import com.example.aics.dto.ToggleFavoriteResponse;
import com.example.aics.entity.ScriptFavorite;
import com.example.aics.exception.LibraryFullException;
import com.example.aics.exception.ResourceNotFoundException;
import com.example.aics.repository.ScriptFavoriteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class ScriptFavoriteService {

    private final ScriptFavoriteRepository repository;
    private final int maxSize;

    public ScriptFavoriteService(
            ScriptFavoriteRepository repository,
            @Value("${app.script-library.max-size:200}") int maxSize
    ) {
        this.repository = repository;
        this.maxSize = maxSize;
    }

    @Transactional
    public ToggleFavoriteResponse toggle(Long staffId, ToggleFavoriteRequest request) {
        Long normalizedStaffId = normalizeStaffId(staffId);
        String contentHash = sha256(request.content());

        Optional<ScriptFavorite> existing = findExisting(normalizedStaffId, request.sourceTalkId(), contentHash);
        if (existing.isPresent()) {
            repository.delete(existing.get());
            return new ToggleFavoriteResponse(false, null, "已取消收藏");
        }

        if (repository.countByStaffId(normalizedStaffId) >= maxSize) {
            throw new LibraryFullException("话术库已达上限，请清理旧话术");
        }

        Set<String> tags = normalizeTags(request.tags());
        Instant now = Instant.now();
        ScriptFavorite saved = repository.save(new ScriptFavorite(
                normalizedStaffId,
                blankToNull(request.sourceTalkId()),
                contentHash,
                request.content().trim(),
                request.scenario().trim(),
                request.generatedAt() == null ? now : request.generatedAt(),
                tags,
                now
        ));

        return new ToggleFavoriteResponse(true, FavoriteResponse.from(saved), "已收藏");
    }

    @Transactional(readOnly = true)
    public PageResponse<FavoriteResponse> search(Long staffId, String keyword, String tag, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        String normalizedKeyword = normalizeSearchText(keyword);
        String normalizedTag = normalizeSearchText(tag);

        List<FavoriteResponse> matched = repository.findByStaffId(normalizeStaffId(staffId)).stream()
                .filter(favorite -> matchesKeyword(favorite, normalizedKeyword))
                .filter(favorite -> matchesTag(favorite, normalizedTag))
                .sorted(Comparator.comparing(ScriptFavorite::getLastUsedAt).reversed())
                .map(FavoriteResponse::from)
                .toList();

        int fromIndex = Math.min(safePage * safeSize, matched.size());
        int toIndex = Math.min(fromIndex + safeSize, matched.size());
        return new PageResponse<>(matched.subList(fromIndex, toIndex), matched.size(), safePage, safeSize);
    }

    @Transactional
    public FavoriteResponse markUsed(Long staffId, Long favoriteId) {
        ScriptFavorite favorite = repository.findById(favoriteId)
                .filter(item -> item.getStaffId().equals(normalizeStaffId(staffId)))
                .orElseThrow(() -> new ResourceNotFoundException("话术不存在"));
        favorite.markUsed(Instant.now());
        return FavoriteResponse.from(favorite);
    }

    @Transactional(readOnly = true)
    public List<String> listTags(Long staffId) {
        return repository.findByStaffId(normalizeStaffId(staffId)).stream()
                .flatMap(favorite -> favorite.getTags().stream())
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional
    public void delete(Long staffId, Long favoriteId) {
        ScriptFavorite favorite = repository.findById(favoriteId)
                .filter(item -> item.getStaffId().equals(normalizeStaffId(staffId)))
                .orElseThrow(() -> new ResourceNotFoundException("话术不存在"));
        repository.delete(favorite);
    }

    private Optional<ScriptFavorite> findExisting(Long staffId, String sourceTalkId, String contentHash) {
        if (StringUtils.hasText(sourceTalkId)) {
            Optional<ScriptFavorite> bySourceTalkId = repository.findByStaffIdAndSourceTalkId(staffId, sourceTalkId.trim());
            if (bySourceTalkId.isPresent()) {
                return bySourceTalkId;
            }
        }
        return repository.findByStaffIdAndContentHash(staffId, contentHash);
    }

    private Set<String> normalizeTags(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            throw new IllegalArgumentException("收藏话术时至少选择1个标签");
        }

        Set<String> tags = new LinkedHashSet<>();
        for (String rawTag : rawTags) {
            if (!StringUtils.hasText(rawTag)) {
                continue;
            }
            String tag = rawTag.trim();
            if (tag.length() > 5) {
                throw new IllegalArgumentException("标签最多5个字符");
            }
            tags.add(tag);
        }

        if (tags.isEmpty()) {
            throw new IllegalArgumentException("收藏话术时至少选择1个标签");
        }
        return tags;
    }

    private boolean matchesKeyword(ScriptFavorite favorite, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String content = favorite.getContent().toLowerCase(Locale.ROOT);
        boolean contentMatched = content.contains(keyword);
        boolean tagMatched = favorite.getTags().stream()
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .anyMatch(tag -> tag.contains(keyword));
        return contentMatched || tagMatched;
    }

    private boolean matchesTag(ScriptFavorite favorite, String tag) {
        if (!StringUtils.hasText(tag)) {
            return true;
        }
        return favorite.getTags().stream()
                .map(item -> item.toLowerCase(Locale.ROOT))
                .anyMatch(item -> item.equals(tag));
    }

    private Long normalizeStaffId(Long staffId) {
        if (staffId == null || staffId <= 0) {
            throw new IllegalArgumentException("客服用户ID无效");
        }
        return staffId;
    }

    private String normalizeSearchText(String text) {
        return StringUtils.hasText(text) ? text.trim().toLowerCase(Locale.ROOT) : "";
    }

    private String blankToNull(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前环境不支持SHA-256", exception);
        }
    }
}
