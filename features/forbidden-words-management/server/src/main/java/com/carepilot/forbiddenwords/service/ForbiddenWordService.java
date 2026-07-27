package com.carepilot.forbiddenwords.service;

import com.carepilot.forbiddenwords.dto.CsvPreviewItem;
import com.carepilot.forbiddenwords.dto.PageResult;
import com.carepilot.forbiddenwords.model.ForbiddenWord;
import com.carepilot.forbiddenwords.model.OperationAuditLog;
import com.carepilot.forbiddenwords.model.Platform;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ForbiddenWordService {
    private final DataStore store;
    private final ForbiddenWordCacheService cacheService;

    public ForbiddenWordService(DataStore store, ForbiddenWordCacheService cacheService) {
        this.store = store;
        this.cacheService = cacheService;
    }

    @PostConstruct
    public void init() {
        seedDataIfEmpty();
        cacheService.refresh(store.listWords());
    }

    public PageResult<ForbiddenWord> list(Platform platform, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? 20 : size;

        List<ForbiddenWord> filtered = store.listWords().stream()
                .filter(w -> platform == null || platform == Platform.ALL || w.getPlatform() == platform || w.getPlatform() == Platform.ALL)
                .sorted(Comparator.comparing(ForbiddenWord::getCreatedAt).reversed())
                .collect(Collectors.toList());

        int from = (safePage - 1) * safeSize;
        if (from >= filtered.size()) {
            return new PageResult<>(List.of(), filtered.size(), safePage, safeSize);
        }
        int to = Math.min(from + safeSize, filtered.size());
        return new PageResult<>(filtered.subList(from, to), filtered.size(), safePage, safeSize);
    }

    public ForbiddenWord addWord(String word, Platform platform, String operator, String ip) {
        ForbiddenWord created = store.addWord(word.trim(), platform, operator, LocalDateTime.now());
        appendOperationLog("ADD", operator, ip, created.getWord(), platform);

        cacheService.refresh(store.listWords());
        return created;
    }

    public void removeWord(Long id, String operator, String ip) {
        var target = store.removeWordById(id);
        if (target.isEmpty()) {
            throw new IllegalArgumentException("Word not found");
        }
        ForbiddenWord word = target.get();
        appendOperationLog("DELETE", operator, ip, word.getWord(), word.getPlatform());

        cacheService.refresh(store.listWords());
    }

    public List<CsvPreviewItem> previewCsv(String content) {
        String[] lines = content == null ? new String[0] : content.split("\\r?\\n");
        List<CsvPreviewItem> result = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] columns = line.split(",");
            if (columns.length != 2) {
                result.add(new CsvPreviewItem(i + 1, lines[i], null, null, false, "列数错误，应为 word,platform"));
                continue;
            }

            String word = columns[0].trim();
            String platformText = columns[1].trim().toUpperCase(Locale.ROOT);
            if (word.isEmpty()) {
                result.add(new CsvPreviewItem(i + 1, lines[i], word, platformText, false, "违禁词为空"));
                continue;
            }

            try {
                Platform.valueOf(platformText);
                result.add(new CsvPreviewItem(i + 1, lines[i], word, platformText, true, null));
            } catch (IllegalArgumentException ex) {
                result.add(new CsvPreviewItem(i + 1, lines[i], word, platformText, false, "平台值不合法"));
            }
        }
        return result;
    }

    public int confirmImport(List<CsvPreviewItem> items, String operator, String ip) {
        List<CsvPreviewItem> validRows = items.stream().filter(CsvPreviewItem::isValid).collect(Collectors.toList());
        for (CsvPreviewItem row : validRows) {
            addWord(row.getWord(), Platform.valueOf(row.getPlatform()), operator, ip);
        }
        return validRows.size();
    }

    public PageResult<OperationAuditLog> listOperationLogs(String operator, String action, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? 20 : size;

        List<OperationAuditLog> filtered = store.listOperationLogs().stream()
                .filter(log -> operator == null || operator.isBlank() || log.getOperator().toLowerCase().contains(operator.toLowerCase()))
                .filter(log -> action == null || action.isBlank() || log.getAction().equalsIgnoreCase(action))
                .sorted(Comparator.comparing(OperationAuditLog::getOperationTime).reversed())
                .collect(Collectors.toList());

        int from = (safePage - 1) * safeSize;
        if (from >= filtered.size()) {
            return new PageResult<>(List.of(), filtered.size(), safePage, safeSize);
        }
        int to = Math.min(from + safeSize, filtered.size());
        return new PageResult<>(filtered.subList(from, to), filtered.size(), safePage, safeSize);
    }

    public void appendOperationLog(String action, String operator, String ip, String targetWord, Platform platform) {
        store.addOperationLog(
                action,
                operator,
                ip,
                targetWord,
                platform,
                LocalDateTime.now()
        );
    }

    public ForbiddenWordCacheService getCacheService() {
        return cacheService;
    }

    private void seedDataIfEmpty() {
        if (!store.listWords().isEmpty()) {
            return;
        }
        addWord("假一赔十", Platform.TAOBAO, "张管理员", "shared-xm4");
        addWord("绝对正品", Platform.JD, "李管理员", "shared-xm4");
        addWord("全网最低价", Platform.PINDUODUO, "张管理员", "shared-xm4");
        addWord("永久保修", Platform.DOUYIN, "王管理员", "shared-xm4");
        addWord("刷单奖励", Platform.KUAISHOU, "李管理员", "shared-xm4");
    }
}
