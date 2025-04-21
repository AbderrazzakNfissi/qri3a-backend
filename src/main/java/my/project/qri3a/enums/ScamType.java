package my.project.qri3a.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Types d'arnaques possibles
 */
public enum ScamType {
    FAKE_ITEM("fake-item"),
    ADVANCE_PAYMENT("advance-payment"),
    NON_DELIVERY("non-delivery"),
    COUNTERFEIT("counterfeit"),
    IDENTITY_THEFT("identity-theft"),
    PHISHING("phishing"),
    OTHER("other");

    private final String code;

    ScamType(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static ScamType fromCode(String code) {
        for (ScamType type : ScamType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return OTHER;
    }
}