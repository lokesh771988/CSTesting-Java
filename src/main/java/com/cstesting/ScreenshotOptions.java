package com.cstesting;

import java.nio.file.Path;

/**
 * Options for capturing a screenshot. Optional: path (write to file), fullPage, selector/locator (crop to element), format, quality (JPEG).
 */
public final class ScreenshotOptions {

    private final String path;
    private final boolean fullPage;
    private final String selector;
    private final Locator locator;
    private final String format; // "png" or "jpeg"
    private final Integer quality; // 0-100 for jpeg

    private ScreenshotOptions(Builder b) {
        this.path = b.path;
        this.fullPage = b.fullPage;
        this.selector = b.selector;
        this.locator = b.locator;
        this.format = b.format != null ? b.format : "png";
        this.quality = b.quality;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getPath() { return path; }
    public boolean isFullPage() { return fullPage; }
    public String getSelector() { return selector; }
    public Locator getLocator() { return locator; }
    public String getFormat() { return format; }
    public Integer getQuality() { return quality; }

    public static final class Builder {
        private String path;
        private boolean fullPage;
        private String selector;
        private Locator locator;
        private String format;
        private Integer quality;

        public Builder path(String path) {
            this.path = path;
            return this;
        }
        public Builder path(Path path) {
            this.path = path != null ? path.toString() : null;
            return this;
        }
        public Builder fullPage(boolean fullPage) {
            this.fullPage = fullPage;
            return this;
        }
        public Builder selector(String selector) {
            this.selector = selector;
            this.locator = null;
            return this;
        }
        public Builder locator(Locator locator) {
            this.locator = locator;
            this.selector = null;
            return this;
        }
        public Builder format(String format) {
            this.format = format;
            return this;
        }
        public Builder quality(int quality) {
            this.quality = quality;
            return this;
        }
        public ScreenshotOptions build() {
            return new ScreenshotOptions(this);
        }
    }
}
