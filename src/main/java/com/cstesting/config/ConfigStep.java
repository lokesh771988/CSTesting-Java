package com.cstesting.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single step from a config file (e.g. goto, type, click). Step type and string arguments for the runner.
 */
public final class ConfigStep {

    public enum Type {
        GOTO,
        TYPE,
        CLICK,
        DOUBLE_CLICK,
        RIGHT_CLICK,
        HOVER,
        WAIT,
        SCREENSHOT,
        FRAME,
        DIALOG_ACCEPT,
        DIALOG_ACCEPT_PROMPT,
        DIALOG_DISMISS,
        CHECK,
        UNCHECK,
        SELECT,
        VERIFY_TEXT,
        ASSERT_ATTRIBUTE,
        /** Assert that text of element (textSelector) equals attribute (attr) of element (attrSelector). If attrSelector omitted, use textSelector for both. */
        ASSERT_TEXT_EQUALS_ATTRIBUTE,
        CLOSE
    }

    private final Type type;
    private final Map<String, String> args;

    public ConfigStep(Type type, Map<String, String> args) {
        this.type = type;
        this.args = args != null ? new LinkedHashMap<>(args) : new LinkedHashMap<>();
    }

    public Type getType() {
        return type;
    }

    /** Get argument by key (e.g. "url", "selector", "text", "timeout", "path"). */
    public String get(String key) {
        return args.get(key);
    }

    public Map<String, String> getArgs() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(args));
    }

    public static Builder builder(Type type) {
        return new Builder(type);
    }

    public static final class Builder {
        private final Type type;
        private final Map<String, String> args = new LinkedHashMap<>();

        Builder(Type type) {
            this.type = type;
        }

        public Builder arg(String key, String value) {
            if (value != null) args.put(key, value);
            return this;
        }

        public ConfigStep build() {
            return new ConfigStep(type, args);
        }
    }
}
