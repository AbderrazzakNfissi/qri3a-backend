package my.project.qri3a.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Préférences de contact
 */
public enum ContactPreference {
    EMAIL("email"),
    PHONE("phone");

    private final String code;

    ContactPreference(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static ContactPreference fromCode(String code) {
        for (ContactPreference preference : ContactPreference.values()) {
            if (preference.code.equals(code)) {
                return preference;
            }
        }
        return EMAIL;
    }
}