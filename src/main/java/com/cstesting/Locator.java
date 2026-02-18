package com.cstesting;

/**
 * Locator for resolving one or more elements. Supports CSS, XPath, and attribute shortcuts
 * (e.g. {@code id="myId"}, {@code name="submit"}, {@code data-testid="foo"}).
 * When a locator matches multiple elements, use {@link #first()}, {@link #last()}, or {@link #nth(int)}
 * to target one; otherwise using it in an action throws if more than one match.
 * <p>
 * Use {@link CSTestingBrowser#locator(String)} to create a locator, then chain {@link #first()},
 * {@link #last()}, or {@link #nth(int)} and call actions: {@code browser.locator("button").first().click()},
 * {@code browser.locator("name=user").type("admin")}.
 */
public final class Locator {

    private final CSTestingBrowser browser;
    private final String rawSelector;
    private final String resolvedSelector;
    private final boolean isXPath;
    private final Integer index;

    /**
     * Create a locator for the given browser and selector.
     * Formats: CSS (default), "xpath=//...", "id=value", "name=value", or any "attr=value" → [attr="value"].
     * Typically created via {@link CSTestingBrowser#locator(String)}.
     */
    public Locator(CSTestingBrowser browser, String selector) {
        this(browser, selector, null);
    }

    private Locator(CSTestingBrowser browser, String rawSelector, Integer index) {
        this.browser = browser;
        this.rawSelector = rawSelector;
        this.index = index;
        Parsed p = parseSelector(rawSelector);
        this.resolvedSelector = p.selector;
        this.isXPath = p.xpath;
    }

    /** Select the first matching element (index 0). */
    public Locator first() {
        return new Locator(browser, rawSelector, 0);
    }

    /** Select the last matching element. */
    public Locator last() {
        return new Locator(browser, rawSelector, -1);
    }

    /** Select the element at the given index (0-based). */
    public Locator nth(int index) {
        if (index < 0) throw new IllegalArgumentException("nth index must be >= 0");
        return new Locator(browser, rawSelector, index);
    }

    /** Click the element(s) targeted by this locator. */
    public void click() {
        browser.click(this);
    }

    /** Move mouse to the element (hover). */
    public void hover() {
        browser.hover(this);
    }

    /** Double-click the element. */
    public void doubleClick() {
        browser.doubleClick(this);
    }

    /** Right-click (context menu) the element. */
    public void rightClick() {
        browser.rightClick(this);
    }

    /** Drag this element to the target element. */
    public void dragAndDrop(Locator to) {
        browser.dragAndDrop(this, to);
    }

    /** Scroll until this element is in view. */
    public void scrollToSelector() {
        browser.scrollToSelector(this);
    }

    /** Type text into the element(s) targeted by this locator. */
    public void type(String text) {
        browser.type(this, text);
    }

    /** Select one option in a single-select dropdown (by value, label, or 0-based index). */
    public void select(Object option) {
        browser.select(this, option);
    }

    /** Select one or more options (single or multi-select). Options can be value, label, or 0-based index. */
    public void selectOptions(Object... options) {
        browser.selectOptions(this, options);
    }

    /** Deselect the given options in a multi-select dropdown. */
    public void deselectOptions(Object... options) {
        browser.deselectOptions(this, options);
    }

    /** Check the checkbox(es) targeted by this locator. */
    public void check() {
        browser.check(this);
    }

    /** Uncheck the checkbox(es) targeted by this locator. */
    public void uncheck() {
        browser.uncheck(this);
    }

    /** Wait until element(s) targeted by this locator are present. */
    public void waitForSelector(Integer timeoutMs) {
        browser.waitForSelector(this, timeoutMs);
    }

    /** Return whether the element(s) targeted by this locator are visible. */
    public boolean isVisible() {
        return browser.isVisible(this);
    }

    /** Return whether the element(s) targeted by this locator are disabled. */
    public boolean isDisabled() {
        return browser.isDisabled(this);
    }

    /** Return whether the option/checkbox targeted by this locator is selected. */
    public boolean isSelected() {
        return browser.isSelected(this);
    }

    /** Return the text content of the element(s) targeted by this locator. */
    public String getTextContent() {
        return browser.getTextContent(this);
    }

    public String getResolvedSelector() {
        return resolvedSelector;
    }

    /** Raw selector string as passed to {@link CSTestingBrowser#locator(String)}. */
    public String getRawSelector() {
        return rawSelector;
    }

    public boolean isXPath() {
        return isXPath;
    }

    /** Null = expect single match (error if multiple). 0-based index, or -1 for last. */
    public Integer getIndex() {
        return index;
    }

    /** Returns a locator that targets the same selector but uses the given browser (e.g. for delegation). */
    public Locator forBrowser(CSTestingBrowser other) {
        return new Locator(other, rawSelector, index);
    }

    private static final class Parsed {
        final String selector;
        final boolean xpath;

        Parsed(String selector, boolean xpath) {
            this.selector = selector;
            this.xpath = xpath;
        }
    }

    private static Parsed parseSelector(String s) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException("Locator selector cannot be null or blank");
        }
        s = s.trim();
        if (s.startsWith("xpath=")) {
            return new Parsed(s.substring(6).trim(), true);
        }
        if (s.startsWith("//") || s.startsWith("/") && !s.startsWith("/*")) {
            return new Parsed(s, true);
        }
        if (s.startsWith("css=")) {
            return new Parsed(s.substring(4).trim(), false);
        }
        // Already a CSS selector (e.g. [id='monday'], #foo, .bar) – use as-is
        if (s.startsWith("[") || s.startsWith("#") || s.startsWith(".")) {
            return new Parsed(s, false);
        }
        int eq = s.indexOf('=');
        if (eq > 0 && !s.substring(0, eq).contains(" ") && eq < s.length() - 1) {
            String key = s.substring(0, eq).trim();
            String value = s.substring(eq + 1).trim();
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                value = value.substring(1, value.length() - 1);
            } else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
                value = value.substring(1, value.length() - 1);
            }
            if ("id".equalsIgnoreCase(key)) {
                return new Parsed("#" + cssEscapeId(value), false);
            }
            return new Parsed("[" + key + "=\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"]", false);
        }
        return new Parsed(s, false);
    }

    private static String cssEscapeId(String id) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c == '\\' || c == ':' || c == ' ' || c == '#' || c == '.' || c == '[' || c == ']' || c == '=' || c == '"' || c == '\'') {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
