package my.project.qri3a.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Raisons pour signaler une arnaque
 */
public enum ReportReason {
    SUSPICIOUS_BEHAVIOR("suspicious-behavior"),
    FINANCIAL_LOSS("financial-loss"),
    PREVENT_OTHERS("prevent-others"),
    NOTICED_SCAM("noticed-scam");

    private final String code;

    ReportReason(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static ReportReason fromCode(String code) {
        for (ReportReason reason : ReportReason.values()) {
            if (reason.code.equals(code)) {
                return reason;
            }
        }
        return SUSPICIOUS_BEHAVIOR;
    }
}