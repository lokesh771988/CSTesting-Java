package com.cstesting;

/**
 * Entry point for CSTesting Java client.
 * When useChromeDirect is true, launches Chrome via our own CDP implementation (no Playwright, no npx).
 * Optional: connect to existing CSTesting server via serverUrl.
 */
public final class CSTesting {

    private CSTesting() {}

    /**
     * Create a browser with default options (headless Chrome via CDP).
     */
    public static CSTestingBrowser createBrowser() {
        return createBrowser(CSTestingOptions.builder().build());
    }

    /**
     * Create a browser with the given options.
     * If serverUrl is set, connects to that server. Otherwise useChromeDirect(true) launches Chrome via CDP (our own code, no Playwright/npx).
     */
    public static CSTestingBrowser createBrowser(CSTestingOptions options) {
        if (options.getServerUrl() != null && !options.getServerUrl().isEmpty()) {
            return CSTestingBrowserImpl.connect(options.getServerUrl());
        }
        if (options.isUseChromeDirect()) {
            return com.cstesting.impl.cdp.CSTestingBrowserCDP.create(options);
        }
        throw new IllegalArgumentException("Set serverUrl to connect to an existing server, or useChromeDirect(true) to launch Chrome via CDP.");
    }

    /**
     * Create a browser that automatically records every action (gotoUrl, click, type, waitFor*, etc.)
     * into the given {@link com.cstesting.report.HtmlReport}. No need to call {@link com.cstesting.report.HtmlReport#recordPass}
     * manually – the report will contain one row per step. When done, call {@link com.cstesting.report.HtmlReport#write(String)}
     * (e.g. from a finally block) to generate the HTML report.
     */
    public static CSTestingBrowser createBrowserWithReport(CSTestingOptions options, com.cstesting.report.HtmlReport report) {
        CSTestingBrowser browser = createBrowser(options);
        return new com.cstesting.report.ReportingBrowser(browser, report);
    }
}
