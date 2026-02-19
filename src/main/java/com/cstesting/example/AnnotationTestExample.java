package com.cstesting.example;

import com.cstesting.CSTesting;
import com.cstesting.annotations.AfterMethod;
import com.cstesting.annotations.BeforeMethod;
import com.cstesting.annotations.BeforeSuite;
import com.cstesting.annotations.AfterSuite;
import com.cstesting.annotations.BeforeClass;
import com.cstesting.annotations.AfterClass;
import com.cstesting.annotations.CSTest;
import com.cstesting.runner.CSTestingRunner;
import com.cstesting.runner.CSTestingTestBase;

/**
 * Example test class using CSTesting annotations (TestNG-style).
 * Browser is created in @BeforeMethod (or injected by CSTestingRunner when run via runner).
 * Run: mvn exec:java -Pannotation-tests
 * Or: mvn exec:java -Dexec.mainClass="com.cstesting.runner.CSTestingRunner" -Dexec.args="com.cstesting.example.AnnotationTestExample"
 */
public class AnnotationTestExample extends CSTestingTestBase {

    private boolean browserCreatedHere;

    @BeforeSuite
    public void beforeSuite() {
        System.out.println("Before suite");
    }
    @AfterSuite
    public void afterSuite() {
        System.out.println("After suite");
    }
    @BeforeClass
    public void beforeClass() {
        System.out.println("Before class");
    }
    @AfterClass
    public void afterClass() {
        System.out.println("After class");
    }

    @BeforeMethod
    public void beforeEach() {
        // Create browser if not already set (runner injects it when using mvn exec:java -Pannotation-tests)
        if (browser == null) {
            browser = CSTesting.createBrowser(getBrowserOptions());
            browserCreatedHere = true;
        } else {
            browserCreatedHere = false;
        }
        System.out.println("Before each test");
    }

    @AfterMethod
    public void afterEach() {
        System.out.println("After each test");
        // Close only if we created the browser in this class (standalone). Runner closes when it created it.
        if (browserCreatedHere && browser != null) {
            try {
                browser.close();
            } catch (Exception ignored) {}
            browser = null;
        }
    }

    @CSTest(description = "Navigate to example.com and check title")
    public void testExampleTitle() {
        browser.gotoUrl("https://example.com");
        browser.assertThat().hasTitle("Example");
    }

    @CSTest(description = "Check heading is visible")
    public void testHeadingVisible() {
        browser.gotoUrl("https://example.com");
        browser.assertThat(browser.locator("h1")).isVisible();
    }

    /** Entry point when this class is run as main (e.g. exec:java with this as mainClass, or from IDE). */
    public static void main(String[] args) {
        CSTestingRunner.run(AnnotationTestExample.class);
    }
}
