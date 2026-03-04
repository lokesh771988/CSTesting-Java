package com.cstesting.runner;

import com.cstesting.CSTestingOptions;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Options for {@link CSTestingRunner#run(Class, RunOptions)}.
 * Use to filter tests by tags and/or pass browser options.
 */
public final class RunOptions {

    private final Set<String> tags;
    private final CSTestingOptions browserOptions;
    /** Optional file/source name for report grouping (e.g. config file path or test class name). */
    private final String file;

    private RunOptions(Builder b) {
        this.tags = b.tags != null ? Collections.unmodifiableSet(new HashSet<>(b.tags)) : Collections.emptySet();
        this.browserOptions = b.browserOptions;
        this.file = b.file;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** If non-empty, only run tests whose @CSTest has at least one of these tags. */
    public Set<String> getTags() {
        return tags;
    }

    /** Browser options; null = use test class getBrowserOptions() or default. */
    public CSTestingOptions getBrowserOptions() {
        return browserOptions;
    }

    /** Optional file/source name for report grouping. */
    public String getFile() {
        return file;
    }

    public static final class Builder {
        private Set<String> tags;
        private CSTestingOptions browserOptions;
        private String file;

        public Builder tags(String... tag) {
            this.tags = tag != null && tag.length > 0 ? new HashSet<>(Arrays.asList(tag)) : null;
            return this;
        }

        public Builder tags(Set<String> tags) {
            this.tags = tags != null ? new HashSet<>(tags) : null;
            return this;
        }

        public Builder browserOptions(CSTestingOptions browserOptions) {
            this.browserOptions = browserOptions;
            return this;
        }

        /** Optional file/source name for report grouping (e.g. config path or test class name). */
        public Builder file(String file) {
            this.file = file;
            return this;
        }

        public RunOptions build() {
            return new RunOptions(this);
        }
    }
}
