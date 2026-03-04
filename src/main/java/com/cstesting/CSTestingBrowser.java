package com.cstesting;

/**
 * Browser automation API (mirrors CSTesting Node API).
 * Commands are sent to the CSTesting Node server over the wire protocol.
 */
public interface CSTestingBrowser {

    void gotoUrl(String url);

    /** Navigate back in history. */
    void back();

    /** Navigate forward in history. */
    void forward();

    /** Reload the current page. */
    void refresh();

    void click(String selector);

    void click(Locator locator);

    /** Move mouse to the element (center). */
    void hover(String selector);

    void hover(Locator locator);

    /** Double-click the element. */
    void doubleClick(String selector);

    void doubleClick(Locator locator);

    /** Right-click (context menu) the element. */
    void rightClick(String selector);

    void rightClick(Locator locator);

    /** Drag from source element to target element. */
    void dragAndDrop(String fromSelector, String toSelector);

    void dragAndDrop(Locator from, Locator to);

    void type(String selector, String text);

    void type(Locator locator, String text);

    /** Press a single key (e.g. "Enter", "Tab", "Backspace", "Escape"). Dispatches to the focused element or page. */
    void pressKey(String key);

    /**
     * Select one option in a single-select dropdown.
     * Option can be String (value or visible label) or Integer (0-based index).
     */
    void select(String selector, Object option);

    void select(Locator locator, Object option);

    /**
     * Select one or more options (single or multi-select).
     * For single-select, selects the first option in the list.
     * For multi-select, selects all given options (by value, label, or 0-based index).
     */
    void selectOptions(String selector, Object... options);

    void selectOptions(Locator locator, Object... options);

    /**
     * Deselect the given options in a multi-select dropdown.
     * Options can be String (value or label) or Integer (0-based index).
     */
    void deselectOptions(String selector, Object... options);

    void deselectOptions(Locator locator, Object... options);

    /** Selected option values (single returns one element, multi returns all). */
    java.util.List<String> getSelectedValues(String selector);

    java.util.List<String> getSelectedValues(Locator locator);

    /** Selected option visible text/labels. */
    java.util.List<String> getSelectedLabels(String selector);

    java.util.List<String> getSelectedLabels(Locator locator);

    void check(String selector);

    void check(Locator locator);

    void uncheck(String selector);

    void uncheck(Locator locator);

    /** Wait until element matching selector is present. Exits as soon as found or when timeoutMs is reached. */
    void waitForSelector(String selector, Integer timeoutMs);

    void waitForSelector(Locator locator, Integer timeoutMs);

    /** Wait until URL matches (supports * pattern). Exits as soon as matched or when timeoutMs is reached. */
    void waitForURL(String urlOrPattern, Integer timeoutMs);

    /** Wait until document.readyState is complete. timeoutMs optional (default 30s). */
    void waitForLoad();

    /** Wait until document.readyState is complete. Exits as soon as complete or when timeoutMs is reached. */
    void waitForLoad(Integer timeoutMs);

    /** Wait until page/document is ready (same as waitForLoad with timeout). */
    void waitForPage(Integer timeoutMs);

    /** Wait until network is idle (page loaded and no pending requests). timeoutMs optional (default 30s). */
    void waitForNetworkLoad(Integer timeoutMs);

    /** Fixed delay in milliseconds. Use for explicit pauses (e.g. waitForTime(5000)). */
    void waitForTime(long millis);

    /** Alias for {@link #waitForTime(long)} – fixed delay in milliseconds. */
    default void sleep(long millis) {
        waitForTime(millis);
    }

    /**
     * Convenience locator for an element with the given attribute value: [attr="value"].
     * Use for data-testid, aria-*, or any attribute. Value is escaped for CSS.
     */
    default Locator getByAttribute(String attr, String value) {
        if (attr == null || attr.isBlank()) throw new IllegalArgumentException("attr cannot be null or blank");
        String escaped = value != null ? value.replace("\\", "\\\\").replace("\"", "\\\"") : "";
        return locator("[" + attr + "=\"" + escaped + "\"]");
    }

    /** Scroll to the top of the page. */
    void scrollToPageTop();

    /** Scroll to the bottom of the page. */
    void scrollToPageBottom();

    /** Scroll up by one viewport height. */
    void scrollUp();

    /** Scroll down by one viewport height. */
    void scrollDown();

    /** Scroll by a pixel offset (positive = down/right, negative = up/left). */
    void scrollBy(int deltaX, int deltaY);

    /** Scroll until the element is in view. */
    void scrollToSelector(String selector);

    void scrollToSelector(Locator locator);

    /** Next JavaScript dialog (alert/confirm/prompt) will be accepted (OK). For prompt, use {@link #acceptNextAlert(String)}. */
    void acceptNextAlert();

    /** Next JavaScript prompt will be accepted with the given text. */
    void acceptNextAlert(String promptText);

    /** Next JavaScript confirm/prompt will be dismissed (Cancel). */
    void dismissNextAlert();

    /** Message of the last JavaScript dialog that opened (for assertions). */
    String getLastAlertMessage();

    String url();

    String content();

    Object evaluate(String expression);

    boolean isVisible(String selector);

    boolean isVisible(Locator locator);

    boolean isDisabled(String selector);

    boolean isDisabled(Locator locator);

    boolean isSelected(String selector);

    boolean isSelected(Locator locator);

    String getTextContent(String selector);

    String getTextContent(Locator locator);

    /** Value of input/textarea/select (empty string if not found or not an input). */
    String getValue(String selector);

    String getValue(Locator locator);

    /** Attribute value or null if missing. */
    String getAttribute(String selector, String attrName);

    String getAttribute(Locator locator, String attrName);

    /** True if element is an editable input/textarea (not disabled, not readonly). */
    boolean isEditable(String selector);

    boolean isEditable(Locator locator);

    /** Number of elements matching the locator. */
    int locatorCount(Locator locator);

    /** Page title. */
    String title();

    CSTestingBrowser frame(String iframeSelector);

    /** List of window/tab handles (opaque IDs; use with switchToWindow). Empty if not supported. */
    java.util.List<String> getWindowHandles();

    /** Current window/tab handle, or empty string if not supported. */
    String getCurrentWindowHandle();

    /** Switch to the tab/window with the given handle (from getWindowHandles). */
    void switchToWindow(String handle);

    /**
     * Open a new tab (about:blank) and return a browser for it. This browser stays on the current tab.
     * Use the returned reference for the new tab – no need to switch back to the parent.
     */
    CSTestingBrowser newTab();

    /**
     * All open tabs as separate browser instances. Use any reference without switching (Playwright-style).
     */
    java.util.List<CSTestingBrowser> getPages();

    /**
     * Wait for a new tab to appear (e.g. after a click that opens a link in a new tab). Returns a browser for the new tab.
     * Timeout in ms; null = default (e.g. 30s). Throws if no new tab within timeout.
     */
    CSTestingBrowser waitForNewTab(Integer timeoutMs);

    /**
     * Capture a screenshot of the page. Returns PNG bytes. Use {@link #getScreenshot(ScreenshotOptions)} for options.
     */
    byte[] getScreenshot();

    /**
     * Capture a screenshot with options (path, fullPage, selector/locator, format, quality).
     * If path is set, bytes are also written to the file. Returns the screenshot bytes.
     */
    byte[] getScreenshot(ScreenshotOptions options);

    /**
     * Create a locator for the given selector. Supports CSS, XPath ({@code xpath=//...} or {@code //...}),
     * and attribute shortcuts: {@code id="value"}, {@code name="value"}, any {@code attr="value"}.
     * If multiple elements match, use {@link Locator#first()}, {@link Locator#last()}, or {@link Locator#nth(int)}
     * before performing an action; otherwise an error is thrown.
     */
    Locator locator(String selector);

    /** Assertions on an element (by locator). Throws AssertionError on failure. */
    Assertion assertThat(Locator locator);

    /** Assertions on the page (title, URL). Throws AssertionError on failure. */
    Assertion assertThat();

    void close();
}
