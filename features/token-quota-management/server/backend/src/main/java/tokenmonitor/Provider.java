package tokenmonitor;

import java.util.Locale;

/** Canonical provider identifiers exposed by the public API. */
public enum Provider {
    KIMI,
    CLAUDE,
    GPT,
    DEEPSEEK,
    MINIMAX,
    GROK,
    GLM,
    OTHER;

    public static Provider parse(String value) {
        if (value == null || value.isBlank()) return OTHER;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MOONSHOT" -> KIMI;
            case "ANTHROPIC" -> CLAUDE;
            case "OPENAI" -> GPT;
            case "DS" -> DEEPSEEK;
            case "XAI" -> GROK;
            case "ZHIPU", "BIGMODEL" -> GLM;
            default -> {
                try { yield valueOf(normalized); }
                catch (IllegalArgumentException ignored) { yield OTHER; }
            }
        };
    }
}
