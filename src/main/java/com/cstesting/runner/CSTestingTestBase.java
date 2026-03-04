package com.cstesting.runner;

import com.cstesting.CSTestingBrowser;
import com.cstesting.CSTestingOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for tests run by {@link CSTestingRunner}. Provides a {@link #browser} field
 * that the runner injects before each test method. Override {@link #getBrowserOptions()} to
 * customize browser options (e.g. headless).
 */
public abstract class CSTestingTestBase {

    /** Injected by the runner before each test method. Available in @BeforeMethod, @CSTest, @AfterMethod. */
    protected CSTestingBrowser browser;

    /** Steps recorded by {@link #step(String)} during the current test. Runner clears before each test. */
    private List<String> stepNames = new ArrayList<>();

    /** Override to customize browser options (default: headless). */
    protected CSTestingOptions getBrowserOptions() {
        return CSTestingOptions.builder().headless(true).build();
    }

    /** Called by the runner to inject the browser. */
    public void setBrowser(CSTestingBrowser browser) {
        this.browser = browser;
    }

    /** Record a step name for the current test (e.g. for reports). No-op if not run by CSTestingRunner. */
    protected void step(String name) {
        if (name != null) stepNames.add(name);
    }

    /** Steps recorded so far in the current test. Runner clears before each test. */
    public List<String> getStepNames() {
        return Collections.unmodifiableList(new ArrayList<>(stepNames));
    }

    /** Called by the runner before each test to clear steps. */
    public void clearSteps() {
        stepNames.clear();
    }
}
