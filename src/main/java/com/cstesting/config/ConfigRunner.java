package com.cstesting.config;

import com.cstesting.CSTesting;
import com.cstesting.CSTestingBrowser;
import com.cstesting.CSTestingOptions;
import com.cstesting.ScreenshotOptions;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Runs a list of {@link ConfigStep} (from a .conf file) in a browser. Creates one browser, executes each step in order,
 * closes the browser at the end (or on close step). Returns {@link RunConfigResult} with success/failure and failed step index.
 */
public final class ConfigRunner {

    /**
     * Parse the config file and run it with default (headless) options.
     */
    public static RunConfigResult run(Path configPath) throws IOException {
        return run(configPath, CSTestingOptions.builder().headless(true).build());
    }

    /**
     * Parse the config file and run it with the given browser options.
     */
    public static RunConfigResult run(Path configPath, CSTestingOptions options) throws IOException {
        List<ConfigStep> steps = ConfigParser.parse(configPath);
        return run(steps, options);
    }

    /**
     * Run a pre-parsed list of steps with default (headless) options.
     */
    public static RunConfigResult run(List<ConfigStep> steps, CSTestingOptions options) {
        CSTestingBrowser browser = null;
        try {
            browser = CSTesting.createBrowser(options != null ? options : CSTestingOptions.builder().headless(true).build());
            return runSteps(browser, steps);
        } finally {
            if (browser != null) {
                try { browser.close(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Run steps with an already-created browser. Caller is responsible for closing the browser. Use this when you need to inject a browser (e.g. with report).
     */
    public static RunConfigResult runSteps(CSTestingBrowser browser, List<ConfigStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return new RunConfigResult(true, 0, -1, null);
        }
        int index = 0;
        CSTestingBrowser current = browser;
        for (ConfigStep step : steps) {
            try {
                current = executeStep(current, step);
                if (current == null) break; // close step: browser already closed
            } catch (Throwable t) {
                String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
                if (t.getCause() != null) msg = t.getCause().getMessage() != null ? t.getCause().getMessage() : msg;
                return new RunConfigResult(false, steps.size(), index, msg);
            }
            index++;
        }
        return new RunConfigResult(true, steps.size(), -1, null);
    }

    /** Returns the browser to use for the next step (same or frame); null after close. */
    private static CSTestingBrowser executeStep(CSTestingBrowser browser, ConfigStep step) {
        switch (step.getType()) {
            case GOTO:
                browser.gotoUrl(require(step, "url"));
                return browser;
            case TYPE:
                browser.type(require(step, "selector"), step.get("text") != null ? step.get("text") : "");
                return browser;
            case CLICK:
                browser.click(require(step, "selector"));
                return browser;
            case DOUBLE_CLICK:
                browser.doubleClick(require(step, "selector"));
                return browser;
            case RIGHT_CLICK:
                browser.rightClick(require(step, "selector"));
                return browser;
            case HOVER:
                browser.hover(require(step, "selector"));
                return browser;
            case WAIT:
                String t = step.get("timeout");
                browser.waitForTime(t != null && !t.isEmpty() ? Long.parseLong(t.trim()) : 1000L);
                return browser;
            case SCREENSHOT:
                String path = step.get("path");
                if (path != null && !path.isEmpty()) {
                    browser.getScreenshot(ScreenshotOptions.builder().path(path).build());
                } else {
                    browser.getScreenshot();
                }
                return browser;
            case FRAME:
                return browser.frame(require(step, "selector"));
            case DIALOG_ACCEPT:
                browser.acceptNextAlert();
                return browser;
            case DIALOG_ACCEPT_PROMPT:
                browser.acceptNextAlert(step.get("promptText") != null ? step.get("promptText") : "");
                return browser;
            case DIALOG_DISMISS:
                browser.dismissNextAlert();
                return browser;
            case CHECK:
                browser.check(require(step, "selector"));
                return browser;
            case UNCHECK:
                browser.uncheck(require(step, "selector"));
                return browser;
            case SELECT:
                String opt = step.get("option");
                if (opt != null && opt.matches("\\d+")) {
                    browser.select(require(step, "selector"), Integer.parseInt(opt));
                } else {
                    browser.select(require(step, "selector"), opt != null ? opt : "");
                }
                return browser;
            case VERIFY_TEXT:
                String sel = require(step, "selector");
                String expected = step.get("expected");
                browser.assertThat(browser.locator(sel)).containText(expected != null ? expected : "");
                return browser;
            case ASSERT_ATTRIBUTE:
                String selector = require(step, "selector");
                String attr = require(step, "attr");
                String value = step.get("value");
                if (value != null) {
                    browser.assertThat(browser.locator(selector)).hasAttribute(attr, value);
                } else {
                    browser.assertThat(browser.locator(selector)).hasAttribute(attr);
                }
                return browser;
            case ASSERT_TEXT_EQUALS_ATTRIBUTE: {
                String textSelector = require(step, "textSelector");
                String attrSelector = require(step, "attrSelector");
                String attrName = require(step, "attr");
                String text = browser.getTextContent(textSelector);
                String attrVal = browser.getAttribute(attrSelector, attrName);
                if (text == null) text = "";
                if (attrVal == null) attrVal = "";
                if (!text.trim().equals(attrVal.trim())) {
                    throw new AssertionError("Text of " + textSelector + " (\"" + text + "\") does not equal attribute " + attrName + " of " + attrSelector + " (\"" + attrVal + "\")");
                }
                return browser;
            }
            case CLOSE:
                browser.close();
                return null;
            default:
                throw new IllegalArgumentException("Unknown step type: " + step.getType());
        }
    }

    private static String require(ConfigStep step, String key) {
        String v = step.get(key);
        if (v == null || v.isEmpty()) throw new IllegalArgumentException("Config step " + step.getType() + " requires '" + key + "'");
        return v;
    }

    /**
     * Entry point to run a config file from command line.
     * Usage: java -cp ... com.cstesting.config.ConfigRunner path/to/file.conf
     * Optional second arg: --headed to run in headed mode.
     */
    public static void main(String[] args) throws IOException {
        if (args == null || args.length == 0) {
            System.err.println("Usage: ConfigRunner <config-file.conf> [--headed]");
            System.exit(1);
        }
        Path path = Paths.get(args[0]);
        if (!java.nio.file.Files.isRegularFile(path)) {
            System.err.println("Config file not found: " + path);
            System.exit(1);
        }
        boolean headed = args.length > 1 && "--headed".equalsIgnoreCase(args[1]);
        CSTestingOptions options = CSTestingOptions.builder().headless(!headed).build();
        RunConfigResult result = run(path, options);
        if (result.isSuccess()) {
            System.out.println("Config run passed: " + result.getTotalSteps() + " steps.");
        } else {
            System.err.println("Config run failed at step " + (result.getFailedStepIndex() + 1) + ": " + result.getErrorMessage());
            System.exit(1);
        }
    }
}
