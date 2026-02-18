package com.cstesting.report;

import com.cstesting.Assertion;
import com.cstesting.CSTestingBrowser;
import com.cstesting.Locator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Wraps a {@link CSTestingBrowser} and records every action (gotoUrl, click, type, waitFor*, etc.)
 * into an {@link HtmlReport} so that an HTML report is generated with the same number of steps
 * as the test, without manually calling {@link HtmlReport#recordPass(String, String)}.
 * <p>
 * Use {@link CSTesting#createBrowserWithReport(CSTestingOptions, HtmlReport)} or wrap an existing
 * browser: {@code new ReportingBrowser(browser, report)}. Use the returned browser as usual;
 * at the end call {@link HtmlReport#write(String)} to generate the HTML.
 */
public final class ReportingBrowser implements CSTestingBrowser {

    private final CSTestingBrowser delegate;
    private final HtmlReport report;

    public ReportingBrowser(CSTestingBrowser delegate, HtmlReport report) {
        this.delegate = delegate;
        this.report = report;
    }

    /** Returns the report so you can call {@link HtmlReport#write(String)} when done. */
    public HtmlReport getReport() {
        return report;
    }

    private void record(String action, String description, Runnable runnable) {
        try {
            runnable.run();
            report.recordPass(action, description);
        } catch (Exception e) {
            report.recordFail(action, description, e.getMessage());
            throw e;
        }
    }

    private <T> T record(String action, String description, java.util.function.Supplier<T> supplier) {
        try {
            T result = supplier.get();
            report.recordPass(action, description);
            return result;
        } catch (Exception e) {
            report.recordFail(action, description, e.getMessage());
            throw e;
        }
    }

    private static String desc(Locator loc) {
        return loc != null ? loc.getRawSelector() : "";
    }

    @Override
    public void gotoUrl(String url) {
        record("gotoUrl", url != null ? url : "", () -> delegate.gotoUrl(url));
    }

    @Override
    public void back() {
        record("back", "", delegate::back);
    }

    @Override
    public void forward() {
        record("forward", "", delegate::forward);
    }

    @Override
    public void refresh() {
        record("refresh", "", delegate::refresh);
    }

    @Override
    public void click(String selector) {
        record("click", selector != null ? selector : "", () -> delegate.click(selector));
    }

    @Override
    public void click(Locator locator) {
        record("click", desc(locator), () -> delegate.click(locator != null ? locator.forBrowser(delegate) : null));
    }

    @Override
    public void hover(String selector) {
        record("hover", selector != null ? selector : "", () -> delegate.hover(selector));
    }

    @Override
    public void hover(Locator locator) {
        record("hover", desc(locator), () -> delegate.hover(locator != null ? locator.forBrowser(delegate) : null));
    }

    @Override
    public void doubleClick(String selector) {
        record("doubleClick", selector != null ? selector : "", () -> delegate.doubleClick(selector));
    }

    @Override
    public void doubleClick(Locator locator) {
        record("doubleClick", desc(locator), () -> delegate.doubleClick(locator != null ? locator.forBrowser(delegate) : null));
    }

    @Override
    public void rightClick(String selector) {
        record("rightClick", selector != null ? selector : "", () -> delegate.rightClick(selector));
    }

    @Override
    public void rightClick(Locator locator) {
        record("rightClick", desc(locator), () -> delegate.rightClick(locator != null ? locator.forBrowser(delegate) : null));
    }

    @Override
    public void dragAndDrop(String fromSelector, String toSelector) {
        record("dragAndDrop", (fromSelector != null ? fromSelector : "") + " -> " + (toSelector != null ? toSelector : ""),
            () -> delegate.dragAndDrop(fromSelector, toSelector));
    }

    @Override
    public void dragAndDrop(Locator from, Locator to) {
        record("dragAndDrop", desc(from) + " -> " + desc(to),
            () -> delegate.dragAndDrop(from != null ? from.forBrowser(delegate) : null, to != null ? to.forBrowser(delegate) : null));
    }

    @Override
    public void type(String selector, String text) {
        record("type", (selector != null ? selector : "") + " | " + (text != null ? text : ""),
            () -> delegate.type(selector, text));
    }

    @Override
    public void type(Locator locator, String text) {
        record("type", desc(locator) + " | " + (text != null ? text : ""),
            () -> delegate.type(locator != null ? locator.forBrowser(delegate) : null, text));
    }

    @Override
    public void select(String selector, Object option) {
        record("select", (selector != null ? selector : "") + " | " + option,
            () -> delegate.select(selector, option));
    }

    @Override
    public void select(Locator locator, Object option) {
        record("select", desc(locator) + " | " + option,
            () -> delegate.select(locator != null ? locator.forBrowser(delegate) : null, option));
    }

    @Override
    public void selectOptions(String selector, Object... options) {
        record("selectOptions", selector != null ? selector : "",
            () -> delegate.selectOptions(selector, options));
    }

    @Override
    public void selectOptions(Locator locator, Object... options) {
        record("selectOptions", desc(locator), () -> delegate.selectOptions(locator != null ? locator.forBrowser(delegate) : null, options));
    }

    @Override
    public void deselectOptions(String selector, Object... options) {
        record("deselectOptions", selector != null ? selector : "", () -> delegate.deselectOptions(selector, options));
    }

    @Override
    public void deselectOptions(Locator locator, Object... options) {
        record("deselectOptions", desc(locator), () -> delegate.deselectOptions(locator != null ? locator.forBrowser(delegate) : null, options));
    }

    @Override
    public List<String> getSelectedValues(String selector) {
        return record("getSelectedValues", selector != null ? selector : "", () -> delegate.getSelectedValues(selector));
    }

    @Override
    public List<String> getSelectedValues(Locator locator) {
        return record("getSelectedValues", desc(locator), () -> delegate.getSelectedValues(locator != null ? locator.forBrowser(delegate) : null));
    }

    @Override
    public List<String> getSelectedLabels(String selector) {
        return record("getSelectedLabels", selector != null ? selector : "", () -> delegate.getSelectedLabels(selector));
    }

    @Override
    public List<String> getSelectedLabels(Locator locator) {
        return record("getSelectedLabels", desc(locator), () -> delegate.getSelectedLabels(locator != null ? locator.forBrowser(delegate) : null));
    }

    @Override
    public void check(String selector) {
        record("check", selector != null ? selector : "", () -> delegate.check(selector));
    }

    @Override
    public void check(Locator locator) {
        record("check", desc(locator), () -> delegate.check(locator != null ? locator.forBrowser(delegate) : null));
    }

    @Override
    public void uncheck(String selector) {
        record("uncheck", selector != null ? selector : "", () -> delegate.uncheck(selector));
    }

    @Override
    public void uncheck(Locator locator) {
        record("uncheck", desc(locator), () -> delegate.uncheck(locator != null ? locator.forBrowser(delegate) : null));
    }

    @Override
    public void waitForSelector(String selector, Integer timeoutMs) {
        record("waitForSelector", (selector != null ? selector : "") + (timeoutMs != null ? " (" + timeoutMs + "ms)" : ""),
            () -> delegate.waitForSelector(selector, timeoutMs));
    }

    @Override
    public void waitForSelector(Locator locator, Integer timeoutMs) {
        record("waitForSelector", desc(locator) + (timeoutMs != null ? " (" + timeoutMs + "ms)" : ""),
            () -> delegate.waitForSelector(locator != null ? locator.forBrowser(delegate) : null, timeoutMs));
    }

    @Override
    public void waitForURL(String urlOrPattern, Integer timeoutMs) {
        record("waitForURL", (urlOrPattern != null ? urlOrPattern : "") + (timeoutMs != null ? " (" + timeoutMs + "ms)" : ""),
            () -> delegate.waitForURL(urlOrPattern, timeoutMs));
    }

    @Override
    public void waitForLoad() {
        record("waitForLoad", "", (Runnable) delegate::waitForLoad);
    }

    @Override
    public void waitForLoad(Integer timeoutMs) {
        record("waitForLoad", timeoutMs != null ? timeoutMs + "ms" : "", () -> delegate.waitForLoad(timeoutMs));
    }

    @Override
    public void waitForPage(Integer timeoutMs) {
        record("waitForPage", timeoutMs != null ? timeoutMs + "ms" : "", () -> delegate.waitForPage(timeoutMs));
    }

    @Override
    public void waitForNetworkLoad(Integer timeoutMs) {
        record("waitForNetworkLoad", timeoutMs != null ? timeoutMs + "ms" : "", () -> delegate.waitForNetworkLoad(timeoutMs));
    }

    @Override
    public void waitForTime(long millis) {
        record("waitForTime", millis + "ms", () -> delegate.waitForTime(millis));
    }

    @Override
    public void scrollToPageTop() {
        record("scrollToPageTop", "", delegate::scrollToPageTop);
    }

    @Override
    public void scrollToPageBottom() {
        record("scrollToPageBottom", "", delegate::scrollToPageBottom);
    }

    @Override
    public void scrollUp() {
        record("scrollUp", "", delegate::scrollUp);
    }

    @Override
    public void scrollDown() {
        record("scrollDown", "", delegate::scrollDown);
    }

    @Override
    public void scrollBy(int deltaX, int deltaY) {
        record("scrollBy", deltaX + "," + deltaY, () -> delegate.scrollBy(deltaX, deltaY));
    }

    @Override
    public void scrollToSelector(String selector) {
        record("scrollToSelector", selector != null ? selector : "", () -> delegate.scrollToSelector(selector));
    }

    @Override
    public void scrollToSelector(Locator locator) {
        record("scrollToSelector", desc(locator), () -> delegate.scrollToSelector(locator != null ? locator.forBrowser(delegate) : null));
    }

    @Override
    public void acceptNextAlert() {
        record("acceptNextAlert", "", (Runnable) delegate::acceptNextAlert);
    }

    @Override
    public void acceptNextAlert(String promptText) {
        record("acceptNextAlert", promptText != null ? promptText : "", () -> delegate.acceptNextAlert(promptText));
    }

    @Override
    public void dismissNextAlert() {
        record("dismissNextAlert", "", delegate::dismissNextAlert);
    }

    @Override
    public String getLastAlertMessage() {
        return record("getLastAlertMessage", "", delegate::getLastAlertMessage);
    }

    @Override
    public String url() {
        return delegate.url();
    }

    @Override
    public String content() {
        return delegate.content();
    }

    @Override
    public Object evaluate(String expression) {
        return record("evaluate", expression != null ? expression : "", () -> delegate.evaluate(expression));
    }

    @Override
    public boolean isVisible(String selector) {
        return record("isVisible", selector != null ? selector : "", () -> delegate.isVisible(selector));
    }

    @Override
    public boolean isVisible(Locator locator) {
        return record("isVisible", desc(locator), () -> delegate.isVisible(locator != null ? locator.forBrowser(delegate) : null));
    }

    @Override
    public boolean isDisabled(String selector) {
        return delegate.isDisabled(selector);
    }

    @Override
    public boolean isDisabled(Locator locator) {
        return delegate.isDisabled(locator != null ? locator.forBrowser(delegate) : null);
    }

    @Override
    public boolean isSelected(String selector) {
        return delegate.isSelected(selector);
    }

    @Override
    public boolean isSelected(Locator locator) {
        return delegate.isSelected(locator != null ? locator.forBrowser(delegate) : null);
    }

    @Override
    public String getTextContent(String selector) {
        return record("getTextContent", selector != null ? selector : "", () -> delegate.getTextContent(selector));
    }

    @Override
    public String getTextContent(Locator locator) {
        return record("getTextContent", desc(locator), () -> delegate.getTextContent(locator != null ? locator.forBrowser(delegate) : null));
    }

    @Override
    public String getValue(String selector) {
        return record("getValue", selector != null ? selector : "", () -> delegate.getValue(selector));
    }

    @Override
    public String getValue(Locator locator) {
        return record("getValue", desc(locator), () -> delegate.getValue(locator != null ? locator.forBrowser(delegate) : null));
    }

    @Override
    public String getAttribute(String selector, String attrName) {
        return record("getAttribute", (selector != null ? selector : "") + " @" + (attrName != null ? attrName : ""),
            () -> delegate.getAttribute(selector, attrName));
    }

    @Override
    public String getAttribute(Locator locator, String attrName) {
        return record("getAttribute", desc(locator) + " @" + (attrName != null ? attrName : ""),
            () -> delegate.getAttribute(locator != null ? locator.forBrowser(delegate) : null, attrName));
    }

    @Override
    public boolean isEditable(String selector) {
        return delegate.isEditable(selector);
    }

    @Override
    public boolean isEditable(Locator locator) {
        return delegate.isEditable(locator != null ? locator.forBrowser(delegate) : null);
    }

    @Override
    public int locatorCount(Locator locator) {
        return delegate.locatorCount(locator != null ? locator.forBrowser(delegate) : null);
    }

    @Override
    public String title() {
        return delegate.title();
    }

    @Override
    public CSTestingBrowser frame(String iframeSelector) {
        return record("frame", iframeSelector != null ? iframeSelector : "",
            () -> new ReportingBrowser(delegate.frame(iframeSelector), report));
    }

    @Override
    public List<String> getWindowHandles() {
        return delegate.getWindowHandles();
    }

    @Override
    public String getCurrentWindowHandle() {
        return delegate.getCurrentWindowHandle();
    }

    @Override
    public void switchToWindow(String handle) {
        record("switchToWindow", handle != null ? handle : "", () -> delegate.switchToWindow(handle));
    }

    @Override
    public CSTestingBrowser newTab() {
        return record("newTab", "", () -> new ReportingBrowser(delegate.newTab(), report));
    }

    @Override
    public List<CSTestingBrowser> getPages() {
        List<CSTestingBrowser> pages = delegate.getPages();
        return pages.stream()
            .map(p -> (CSTestingBrowser) new ReportingBrowser(p, report))
            .collect(Collectors.toList());
    }

    @Override
    public Locator locator(String selector) {
        return new Locator(this, selector);
    }

    @Override
    public Assertion assertThat(Locator locator) {
        return delegate.assertThat(locator != null ? locator.forBrowser(delegate) : null);
    }

    @Override
    public Assertion assertThat() {
        return delegate.assertThat();
    }

    @Override
    public void close() {
        record("close", "", delegate::close);
    }
}
