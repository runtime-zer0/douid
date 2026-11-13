package kr.douid.brand.ai.application;

import java.time.Duration;

public record AiChatPolicy(int maxRequestBytes, int maxMessageCodePoints, int maxHistoryMessages,
        int maxHistoryMessageCodePoints, int maxContextCodePoints, int rateRequests, Duration rateWindow,
        int maxConcurrentStreams, Duration executionTimeout, Duration emitterTimeout,
        long maxOutputBytes, int maxDeltaEvents) {}
