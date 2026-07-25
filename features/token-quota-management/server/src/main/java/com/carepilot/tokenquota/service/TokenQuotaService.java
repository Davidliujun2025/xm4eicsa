package com.carepilot.tokenquota.service;

import com.carepilot.tokenquota.common.BusinessException;
import com.carepilot.tokenquota.common.ErrorCode;
import com.carepilot.tokenquota.config.TokenQuotaProperties;
import com.carepilot.tokenquota.domain.TokenQuotaAccount;
import com.carepilot.tokenquota.domain.TokenQuotaAdjustmentLog;
import com.carepilot.tokenquota.domain.TokenUsageStatus;
import com.carepilot.tokenquota.dto.AdjustmentLogResponse;
import com.carepilot.tokenquota.dto.BatchUpdateDailyQuotaRequest;
import com.carepilot.tokenquota.dto.BatchUpdateDailyQuotaResponse;
import com.carepilot.tokenquota.dto.PageResponse;
import com.carepilot.tokenquota.dto.RecordTokenUsageRequest;
import com.carepilot.tokenquota.dto.RecordTokenUsageResponse;
import com.carepilot.tokenquota.dto.TokenQuotaItemResponse;
import com.carepilot.tokenquota.dto.TokenQuotaSummaryResponse;
import com.carepilot.tokenquota.dto.UpdateDailyQuotaRequest;
import com.carepilot.tokenquota.dto.UpdateDailyQuotaResponse;
import com.carepilot.tokenquota.repository.TokenQuotaAdjustmentLogRepository;
import com.carepilot.tokenquota.repository.TokenQuotaRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TokenQuotaService {
    private final TokenQuotaRepository tokenQuotaRepository;
    private final TokenQuotaAdjustmentLogRepository adjustmentLogRepository;
    private final TokenQuotaProperties properties;
    private final TokenQuotaCalculator calculator;
    private final Clock clock;

    public TokenQuotaService(
            TokenQuotaRepository tokenQuotaRepository,
            TokenQuotaAdjustmentLogRepository adjustmentLogRepository,
            TokenQuotaProperties properties,
            TokenQuotaCalculator calculator,
            Clock clock
    ) {
        this.tokenQuotaRepository = tokenQuotaRepository;
        this.adjustmentLogRepository = adjustmentLogRepository;
        this.properties = properties;
        this.calculator = calculator;
        this.clock = clock;
    }

    public PageResponse<TokenQuotaItemResponse> listAccounts(
            String accountNo,
            String statusCode,
            Integer page,
            Integer pageSize
    ) {
        List<TokenQuotaItemResponse> records = tokenQuotaRepository.findAllAccounts()
                .stream()
                .filter(account -> !StringUtils.hasText(accountNo)
                        || account.accountNo().contains(normalizeAccountNo(accountNo)))
                .map(calculator::toItemResponse)
                .filter(item -> !StringUtils.hasText(statusCode)
                        || item.statusCode().equalsIgnoreCase(statusCode.trim()))
                .toList();
        return PageUtils.page(records, page, pageSize, properties);
    }

    public TokenQuotaItemResponse getAccount(String accountNo) {
        return calculator.toItemResponse(getRequiredAccount(accountNo));
    }

    public TokenQuotaSummaryResponse getSummary() {
        List<TokenQuotaAccount> accounts = tokenQuotaRepository.findAllAccounts();
        long totalAccounts = accounts.size();
        long totalDailyQuota = accounts.stream().mapToLong(TokenQuotaAccount::dailyQuota).sum();
        long totalUsedTokens = accounts.stream().mapToLong(TokenQuotaAccount::usedTokensToday).sum();
        long highUsageCount = accounts.stream()
                .filter(account -> calculator.calculateStatus(account.dailyQuota(), account.usedTokensToday())
                        == TokenUsageStatus.HIGH_USAGE)
                .count();
        long exceededCount = accounts.stream()
                .filter(account -> calculator.calculateStatus(account.dailyQuota(), account.usedTokensToday())
                        == TokenUsageStatus.EXCEEDED_RECOMMENDED)
                .count();
        double overallUsageRatePercent = totalDailyQuota <= 0
                ? 0D
                : Math.round(totalUsedTokens * 10000D / totalDailyQuota) / 100D;

        return new TokenQuotaSummaryResponse(
                totalAccounts,
                totalDailyQuota,
                totalUsedTokens,
                overallUsageRatePercent,
                highUsageCount,
                exceededCount
        );
    }

    public synchronized UpdateDailyQuotaResponse updateDailyQuota(
            String accountNo,
            UpdateDailyQuotaRequest request
    ) {
        long newQuota = request.dailyQuota();
        validateDailyQuota(newQuota);

        TokenQuotaAccount account = getRequiredAccount(accountNo);
        return updateDailyQuotaForAccount(account, newQuota, request.reason(), request.operatorId(), request.operatorName());
    }

    public synchronized BatchUpdateDailyQuotaResponse batchUpdateDailyQuota(BatchUpdateDailyQuotaRequest request) {
        long newQuota = request.dailyQuota();
        validateDailyQuota(newQuota);

        Set<String> accountNos = new LinkedHashSet<>();
        for (String accountNo : request.accountNos()) {
            accountNos.add(normalizeAccountNo(accountNo));
        }

        List<TokenQuotaAccount> accounts = accountNos.stream()
                .map(this::getRequiredAccount)
                .toList();

        List<UpdateDailyQuotaResponse> results = accounts.stream()
                .map(account -> updateDailyQuotaForAccount(
                        account,
                        newQuota,
                        request.reason(),
                        request.operatorId(),
                        request.operatorName()
                ))
                .toList();

        return new BatchUpdateDailyQuotaResponse(accountNos.size(), results.size(), results);
    }

    private UpdateDailyQuotaResponse updateDailyQuotaForAccount(
            TokenQuotaAccount account,
            long newQuota,
            String reason,
            String operatorId,
            String operatorName
    ) {
        TokenQuotaAccount saved = tokenQuotaRepository.save(account.withDailyQuota(newQuota));
        TokenQuotaAdjustmentLog savedLog = adjustmentLogRepository.save(new TokenQuotaAdjustmentLog(
                null,
                saved.accountNo(),
                saved.accountName(),
                account.dailyQuota(),
                saved.dailyQuota(),
                operatorId.trim(),
                operatorName.trim(),
                reason.trim(),
                LocalDateTime.now(clock)
        ));

        return new UpdateDailyQuotaResponse(
                saved.accountNo(),
                savedLog.beforeQuota(),
                savedLog.afterQuota(),
                savedLog.adjustedAt()
        );
    }

    public synchronized RecordTokenUsageResponse recordUsage(RecordTokenUsageRequest request) {
        TokenQuotaAccount account = getRequiredAccount(request.accountNo());
        long consumedTokens = request.consumedTokens();
        if (consumedTokens <= 0) {
            throw new BusinessException(ErrorCode.USAGE_INVALID, "Token 消耗量必须为正整数");
        }

        long aiCallCount = safeNonNegative(request.aiCallCount());
        long businessCount = safeNonNegative(request.businessCount());
        TokenQuotaAccount updated = account.withUsage(
                account.usedTokensToday() + consumedTokens,
                account.aiCallCountToday() + aiCallCount,
                account.businessCountToday() + businessCount,
                LocalDate.now(clock)
        );
        TokenQuotaAccount saved = tokenQuotaRepository.recordUsage(
                updated,
                consumedTokens,
                aiCallCount,
                businessCount,
                LocalDateTime.now(clock)
        );
        return calculator.toUsageResponse(saved);
    }

    public PageResponse<AdjustmentLogResponse> listAdjustmentLogs(
            String accountNo,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer page,
            Integer pageSize
    ) {
        List<AdjustmentLogResponse> records = adjustmentLogRepository.findLogs(
                        normalizeNullableAccountNo(accountNo),
                        startTime,
                        endTime
                )
                .stream()
                .map(this::toLogResponse)
                .toList();
        return PageUtils.page(records, page, pageSize, properties);
    }

    private AdjustmentLogResponse toLogResponse(TokenQuotaAdjustmentLog log) {
        return new AdjustmentLogResponse(
                log.id(),
                log.accountNo(),
                log.accountName(),
                log.beforeQuota(),
                log.afterQuota(),
                log.operatorId(),
                log.operatorName(),
                log.reason(),
                log.adjustedAt()
        );
    }

    private TokenQuotaAccount getRequiredAccount(String accountNo) {
        return tokenQuotaRepository.findByAccountNo(normalizeAccountNo(accountNo))
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "客服账号不存在"));
    }

    private void validateDailyQuota(long dailyQuota) {
        if (dailyQuota <= 0) {
            throw new BusinessException(ErrorCode.QUOTA_INVALID, "每日 Token 配额必须为正整数");
        }
        if (dailyQuota > properties.getMaxDailyQuota()) {
            throw new BusinessException(
                    ErrorCode.QUOTA_INVALID,
                    "每日 Token 配额不能超过系统允许的最大额度"
            );
        }
    }

    private long safeNonNegative(Long value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private String normalizeAccountNo(String accountNo) {
        return accountNo == null ? "" : accountNo.trim().toUpperCase();
    }

    private String normalizeNullableAccountNo(String accountNo) {
        return StringUtils.hasText(accountNo) ? accountNo.trim().toUpperCase() : null;
    }
}
