package com.carepilot.chatworkbench.service;

/** Signals that the shared token ledger changed for one customer-service user. */
public record TokenUsageRecordedEvent(String userId) {
}
