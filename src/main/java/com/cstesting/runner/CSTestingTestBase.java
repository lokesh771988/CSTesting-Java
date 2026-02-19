package com.cstesting.runner;

import com.cstesting.CSTestingBrowser;
import com.cstesting.CSTestingOptions;

/**
 * Base class for tests run by {@link CSTestingRunner}. Provides a {@link #browser} field
 * that the runner injects before each test method. Override {@link #getBrowserOptions()} to
 * customize browser options (e.g. headless).
 */
public abstract class CSTestingTestBase {

    /** Injected by the runner before each test method. Available in @BeforeMethod, @CSTest, @AfterMethod. */
    protected CSTestingBrowser browser;

    /** Override to customize browser options (default: headless). */
    protected CSTestingOptions getBrowserOptions() {
        return CSTestingOptions.builder().headless(true).build();
    }

    /** Called by the runner to inject the browser. */
    public void setBrowser(CSTestingBrowser browser) {
        this.browser = browser;
    }
}
