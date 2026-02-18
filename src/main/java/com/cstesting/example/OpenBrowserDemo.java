package com.cstesting.example;

import com.cstesting.CSTesting;
import com.cstesting.CSTestingBrowser;
import com.cstesting.CSTestingOptions;

/**
 * Simple demo to verify the browser opens and CSTesting client works.
 * Run: mvn compile exec:java -Dexec.mainClass="com.cstesting.example.OpenBrowserDemo"
 * Or run main() from your IDE.
 */
public class OpenBrowserDemo {

    public static void main(String[] args) {
        System.out.println("Opening browser with CSTesting...");
        CSTestingBrowser browser = null;
        try {
            browser = CSTesting.createBrowser(
                CSTestingOptions.builder()
                    .headless(false)  // set true to run without visible window
                    .build()
            );
            browser.gotoUrl("https://example.com");
            String url = browser.url();
            System.out.println("Browser opened. Current URL: " + url);
            String content = browser.content();
            boolean hasExample = content != null && content.toLowerCase().contains("example");
            System.out.println("Page contains 'example': " + hasExample);
            System.out.println("SUCCESS: Browser is working.");
        } catch (Throwable t) {
            System.err.println("FAILED: " + t.getMessage());
            t.printStackTrace();
        } finally {
            if (browser != null) {
                browser.close();
                System.out.println("Browser closed.");
            }
        }
    }
}
