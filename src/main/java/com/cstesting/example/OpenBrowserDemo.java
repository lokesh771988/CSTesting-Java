package com.cstesting.example;

import com.cstesting.CSTesting;
import com.cstesting.CSTestingBrowser;
import com.cstesting.CSTestingOptions;
import com.cstesting.report.HtmlReport;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

/**
 * Simple demo to verify the browser opens and CSTesting client works.
 * Run: mvn compile exec:java -Dexec.mainClass="com.cstesting.example.OpenBrowserDemo"
 * Or run main() from your IDE.
 * Uses createBrowserWithReport so every step is auto-recorded; no need to call report.recordPass().
 * Generates an HTML report at target/cstesting-report.html when execution completes (one row per step).
 */
public class OpenBrowserDemo {

    private static final String REPORT_PATH = "target/cstesting-report.html";

    public static void main(String[] args) {
        System.out.println("Opening browser with CSTesting...");
        HtmlReport report = new HtmlReport().setTestName("OpenBrowserDemo");
        CSTestingBrowser browser = null;
        try {
            // createBrowserWithReport: every gotoUrl, click, type, waitFor*, etc. is recorded automatically
            browser = CSTesting.createBrowserWithReport(
                CSTestingOptions.builder().headless(false).build(),
                report
            );
            //browser.gotoUrl("https://testautomationpractice.blogspot.com/");
            //browser.waitForSelector("#name", 10000);
            //browser.locator("id=name").type("John Doe");
            //browser.waitForSelector("[value='monday']", 10000);
            //browser.locator("id=monday").check();
            //browser.locator("#monday").check();
            //browser.locator("#male").click();
            // Page
            /*browser.assertThat().hasTitle("Automation Testing Practice");
            browser.assertThat().hasURL("https://testautomationpractice.blogspot.com/");
            
            // Element
            browser.assertThat(browser.locator("#name")).isVisible().isEditable();
            browser.assertThat(browser.locator("#name")).hasValue("John Doe");
            //browser.assertThat(browser.locator("h1")).hasText("Welcome");
            browser.assertThat(browser.locator("button")).hasCount(14);
            browser.assertThat(browser.locator("a").first()).hasAttribute("href");
            browser.assertThat(browser.locator("li")).count(12); 
            browser.assertThat(browser.locator("h1")).containText("Automation Testing");
            browser.assertThat(browser.locator("#search")).containValue("query");
            */
           // Single
            /*browser.locator("#country").select("UK");
            browser.locator("#country").select(2);
            browser.select("name=country", "Germany");
            // Multi
            browser.locator("#colors").selectOptions("Red", "Blue");
            browser.locator("#colors").selectOptions(0, 2, 4);
            browser.locator("#colors").deselectOptions("Blue");
            System.out.println("Selected values: " + browser.getSelectedValues(browser.locator("#colors")));
            System.out.println("Selected labels: " + browser.getSelectedLabels(browser.locator("#colors")));
            */
           // Alert - accept OK
            /*browser.acceptNextAlert();
            browser.locator("#alertBtn").click();
            System.out.println("Alert message: " + browser.getLastAlertMessage());

            // Confirm - dismiss Cancel
            browser.dismissNextAlert();
            browser.locator("#confirmBtn").click();
            System.out.println("Confirm message: " + browser.getLastAlertMessage());

            // Prompt - accept with text
            browser.acceptNextAlert("my name");
            browser.locator("#promptBtn").click();
            System.out.println("Prompt message: " + browser.getLastAlertMessage());
            */
            //browser.gotoUrl("https://demo.automationtesting.in/Register.html");
            //browser.locator("//*[text()='SwitchTo']").hover();
            //browser.locator("//*[text()='Copy Text']").doubleClick();
            //browser.gotoUrl("https://demo.guru99.com/test/simple_context_menu.html");
            //browser.locator("//*[text()='right click me']").rightClick();
            //browser.dragAndDrop("#draggable", "#droppable");
            //browser.locator("#draggable").first().dragAndDrop(browser.locator("#droppable"));
            
            //browser.scrollToPageBottom();
            //browser.waitForTime(1000);
            //browser.scrollToPageTop();
            //browser.scrollDown();
            //browser.scrollUp();
            //browser.scrollBy(0, 120);   // 300px down
            //browser.waitForTime(1000);
            //browser.scrollBy(0, -100);   // 200px up
            //browser.scrollToSelector("#productTable");
            //browser.locator("#section-2").scrollToSelector();

            /*browser.gotoUrl("https://the-internet.herokuapp.com/nested_frames");
            browser.waitForTime(10000);
            browser.waitForSelector("[name='frame-top']", 10_000);
            CSTestingBrowser outer = browser.frame("[name='frame-top']");
            outer.waitForTime(10000);
            //browser.waitForSelector("[src='https://docs.google.com/forms/d/1yfUq-GO9BEssafd6TvHhf0D6QLDVG3q5InwNE2FFFFQ/viewform?embedded=true']", 60_000);
            CSTestingBrowser inner = outer.frame("[name='frame-left']");
            System.out.println("Left frame body: " + inner.locator("//body").getTextContent());
            */
            // No report.recordPass() needed – each call below is recorded automatically
            String url = "https://testautomationpractice.blogspot.com/";
            browser.gotoUrl(url);
            browser.waitForLoad(10_000);

            // --- Option A: WITH SWITCHING (handles + switchToWindow) ---
            //String first = browser.getCurrentWindowHandle();
            // "New Tab" is often in a widget – give it time to load.
            browser.waitForTime(3000);
            //String newTabSelector = "#PopUp";   // link; or "//*[contains(text(),'New Tab')]" for any element
            /*browser.locator(newTabSelector).scrollToSelector();    // scroll into view before click
            browser.locator(newTabSelector).first().click();
            browser.waitForTime(500);
            List<String> handles = browser.getWindowHandles();
            for (String h : handles) {
                if (!h.equals(first)) {
                    browser.switchToWindow(h);
                    System.out.println("New tab title: " + browser.title());
                    //break;
                }
            }            
            browser.switchToWindow(first);   // back to first tab
            System.out.println("Back on first tab: " + browser.title());

            // --- Option B: WITHOUT SWITCHING (keep references, use getPages) ---
            CSTestingBrowser parent = browser;
            browser.waitForSelector("#PopUp", 15_000);
            browser.locator("#PopUp").first().click();   // opens new tab
            browser.waitForTime(500);
            List<CSTestingBrowser> pages = browser.getPages();
            CSTestingBrowser newTabPage = pages.size() > 1 ? pages.get(1) : pages.get(0);
            System.out.println("Parent tab title: " + parent.title());
            System.out.println("New tab title:    " + newTabPage.title());
            parent.locator("h1").getTextContent();   // work on first tab – no switch
            newTabPage.locator("body").getTextContent();  // work on new tab – no switch
            */
        } finally {
            if (browser != null) {
                //browser.close();
                System.out.println("Browser closed.");
            }
            try {
                report.write(REPORT_PATH);
                System.out.println("HTML report: " + Paths.get(REPORT_PATH).toAbsolutePath());
            } catch (IOException e) {
                System.err.println("Could not write report: " + e.getMessage());
            }
        }
    }
}
