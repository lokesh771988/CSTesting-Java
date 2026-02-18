package com.cstesting;

/**
 * Fluent assertions for elements and page. Obtain via {@link CSTestingBrowser#assertThat(Locator)}
 * (element) or {@link CSTestingBrowser#assertThat()} (page). Throws {@link AssertionError} on failure.
 */
public final class Assertion {

    private final CSTestingBrowser browser;
    private final Locator locator;

    /** Create an assertion for an element (locator) or page (locator null). */
    public Assertion(CSTestingBrowser browser, Locator locator) {
        this.browser = browser;
        this.locator = locator;
    }

    private void fail(String message) {
        throw new AssertionError(message);
    }

    private void requireElement() {
        if (locator == null) {
            throw new IllegalStateException("Page-level assertion: use hasTitle() or hasURL()");
        }
    }

    /** Asserts the element is visible. */
    public Assertion isVisible() {
        requireElement();
        if (!browser.isVisible(locator)) {
            fail("Expected element to be visible: " + locator.getResolvedSelector());
        }
        return this;
    }

    /** Asserts the element is editable (input/textarea not disabled and not readonly). */
    public Assertion isEditable() {
        requireElement();
        if (!browser.isEditable(locator)) {
            fail("Expected element to be editable: " + locator.getResolvedSelector());
        }
        return this;
    }

    /** Asserts the element's value (input/textarea) or text is empty. */
    public Assertion isEmpty() {
        requireElement();
        String v = getValueOrText();
        if (v != null && !v.trim().isEmpty()) {
            fail("Expected element to be empty but had: " + v);
        }
        return this;
    }

    /** Asserts the element is enabled (not disabled). */
    public Assertion isEnabled() {
        requireElement();
        if (browser.isDisabled(locator)) {
            fail("Expected element to be enabled: " + locator.getResolvedSelector());
        }
        return this;
    }

    /** Asserts the element is disabled. */
    public Assertion isDisabled() {
        requireElement();
        if (!browser.isDisabled(locator)) {
            fail("Expected element to be disabled: " + locator.getResolvedSelector());
        }
        return this;
    }

    /** Asserts text content or value contains the given string. */
    public Assertion contains(String expected) {
        requireElement();
        String v = getValueOrText();
        if (v == null || !v.contains(expected)) {
            fail("Expected element to contain '" + expected + "' but had: " + v);
        }
        return this;
    }

    /** Asserts the number of matching elements equals the expected value. */
    public Assertion count(int expected) {
        requireElement();
        int n = browser.locatorCount(locator);
        if (n != expected) {
            fail("Expected count " + expected + " but was " + n + " for: " + locator.getResolvedSelector());
        }
        return this;
    }

    /** Asserts the count of matching elements equals the expected value (same as {@link #count(int)}). */
    public Assertion hasCount(int expected) {
        return count(expected);
    }

    /** Asserts element text content (trimmed) contains or equals the given text. */
    public Assertion hasText(String expected) {
        requireElement();
        String text = browser.getTextContent(locator);
        if (text == null) text = "";
        text = text.trim();
        if (!text.contains(expected) && !text.equals(expected)) {
            fail("Expected element to have text containing '" + expected + "' but had: " + text);
        }
        return this;
    }

    /** Asserts element text content contains the given string. */
    public Assertion containText(String expected) {
        requireElement();
        String text = browser.getTextContent(locator);
        if (text == null) text = "";
        if (!text.contains(expected)) {
            fail("Expected text to contain '" + expected + "' but had: " + text);
        }
        return this;
    }

    /** Asserts input/select value equals the given value. */
    public Assertion hasValue(String expected) {
        requireElement();
        String value = browser.getValue(locator);
        if (value == null) value = "";
        if (!value.equals(expected)) {
            fail("Expected value '" + expected + "' but was: " + value);
        }
        return this;
    }

    /** Asserts input/select value contains the given string. */
    public Assertion containValue(String expected) {
        requireElement();
        String value = browser.getValue(locator);
        if (value == null) value = "";
        if (!value.contains(expected)) {
            fail("Expected value to contain '" + expected + "' but was: " + value);
        }
        return this;
    }

    /** Asserts the element has the attribute (any value). */
    public Assertion hasAttribute(String attrName) {
        requireElement();
        String v = browser.getAttribute(locator, attrName);
        if (v == null || v.isEmpty()) {
            fail("Expected element to have attribute '" + attrName + "'");
        }
        return this;
    }

    /** Asserts the element's attribute equals the expected value. */
    public Assertion hasAttribute(String attrName, String expectedValue) {
        requireElement();
        String v = browser.getAttribute(locator, attrName);
        if (v == null ? expectedValue != null : !v.equals(expectedValue)) {
            fail("Expected attribute '" + attrName + "' to be '" + expectedValue + "' but was: " + v);
        }
        return this;
    }

    /** Asserts page title equals or contains the expected string. */
    public Assertion hasTitle(String expected) {
        if (locator != null) {
            throw new IllegalStateException("Use assertThat() for page assertions (hasTitle, hasURL)");
        }
        String title = browser.title();
        if (title == null) title = "";
        if (!title.equals(expected) && !title.contains(expected)) {
            fail("Expected title to contain '" + expected + "' but was: " + title);
        }
        return this;
    }

    /** Asserts page URL equals or matches pattern (use * as wildcard). */
    public Assertion hasURL(String expectedOrPattern) {
        if (locator != null) {
            throw new IllegalStateException("Use assertThat() for page assertions (hasTitle, hasURL)");
        }
        String url = browser.url();
        if (url == null) url = "";
        if (expectedOrPattern.contains("*")) {
            String regex = "^" + expectedOrPattern.replace("\\", "\\\\").replace(".", "\\.").replace("*", ".*") + "$";
            if (!url.matches(regex)) {
                fail("Expected URL to match '" + expectedOrPattern + "' but was: " + url);
            }
        } else {
            if (!url.equals(expectedOrPattern) && !url.contains(expectedOrPattern)) {
                fail("Expected URL to contain '" + expectedOrPattern + "' but was: " + url);
            }
        }
        return this;
    }

    private String getValueOrText() {
        String value = browser.getValue(locator);
        if (value != null && !value.isEmpty()) return value;
        return browser.getTextContent(locator);
    }
}
