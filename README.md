# CSTesting Java Client

**Repo:** [github.com/cstesttool/CSTesting-Java](https://github.com/cstesttool/CSTesting-Java)

---

## Installation

### Introduction

CSTesting for Java is a browser automation library designed for end-to-end testing and scripting. It drives **Chrome** via the **Chrome DevTools Protocol (CDP)** — no Playwright, no npx, no Node server. Run tests on Windows, Linux, and macOS, locally or on CI, headless or headed.

CSTesting is distributed as a Maven artifact. The easiest way to use it is to add one dependency to your project's `pom.xml` as described below. If you're not familiar with Maven, please refer to its [documentation](https://maven.apache.org/guides/).

### Usage

Get started by adding the dependency and running the example below.

**pom.xml** (add the dependency):

```xml
<dependency>
  <groupId>io.github.cstesttool</groupId>
  <artifactId>cstesting-java</artifactId>
  <version>0.1.4</version>
</dependency>
```

**App.java** (`src/main/java/org/example/App.java`):

```java
package org.example;

import com.cstesting.CSTesting;
import com.cstesting.CSTestingBrowser;
import com.cstesting.CSTestingOptions;

public class App {
    public static void main(String[] args) {
        CSTestingBrowser browser = CSTesting.createBrowser(
            CSTestingOptions.builder().headless(true).build()
        );
        try {
            browser.gotoUrl("https://example.com");
            System.out.println(browser.title());
        } finally {
            browser.close();
        }
    }
}
```

Compile and run:

```bash
mvn compile exec:java -Dexec.mainClass="org.example.App"
```

No browser binaries are downloaded — CSTesting uses your system Chrome. To use a custom Chrome path, set `-Dcstesting.chrome.path=/path/to/chrome`.

### First script

In this script we navigate to a page and take a screenshot (requires a small helper to save the page as PNG; here we use the title and URL as the “result”):

```java
package org.example;

import com.cstesting.CSTesting;
import com.cstesting.CSTestingBrowser;
import com.cstesting.CSTestingOptions;

public class App {
    public static void main(String[] args) {
        CSTestingBrowser browser = CSTesting.createBrowser(
            CSTestingOptions.builder().headless(true).build()
        );
        try {
            browser.gotoUrl("https://example.com");
            browser.waitForLoad(5_000);
            System.out.println("Title: " + browser.title());
            System.out.println("URL: " + browser.url());
            browser.locator("h1").getTextContent();  // example interaction
        } finally {
            browser.close();
        }
    }
}
```

By default, the browser runs in headless mode. To see the browser window, set `headless(false)`:

```java
CSTestingBrowser browser = CSTesting.createBrowser(
    CSTestingOptions.builder().headless(false).build()
);
```

### Running the example

```bash
mvn compile exec:java -Dexec.mainClass="org.example.App"
```

Browsers launched with CSTesting run headless by default (no visible window). Pass `CSTestingOptions.builder().headless(false).build()` to show the browser UI.

### System requirements

- **Java 11 or higher**
- **Chrome** installed (used via CDP). On Windows: typical Chrome install path. On macOS: `/Applications/Google Chrome.app`. On Linux: `google-chrome` or `chromium`. Override with `-Dcstesting.chrome.path=/path/to/chrome` if needed.
- **OS:** Windows 10+, macOS 10.14+, or Linux (e.g. Debian, Ubuntu) on x86-64 or arm64.

---

## Prerequisites (summary)

- **Java 11+**
- **Chrome** installed (used via CDP when `useChromeDirect` is true). Override path with `-Dcstesting.chrome.path=/path/to/chrome` if needed.
- Optional: use **serverUrl** to connect to an existing CSTesting server instead of launching Chrome via CDP.

## Project layout

```
CSTesting-Java/
├── pom.xml
├── README.md
└── src/main/java/com/cstesting/
    ├── CSTesting.java           # Entry: createBrowser()
    ├── CSTestingOptions.java    # Options (headless, port, serverUrl)
    ├── CSTestingBrowser.java    # Browser API interface
    ├── CSTestingBrowserImpl.java
    ├── CSTestingBrowserFrame.java
    ├── impl/cdp/
    │   ├── CDPConnection.java      # Chrome DevTools Protocol over WebSocket
    │   ├── ChromeLauncher.java     # Launch Chrome (no Playwright, no npx)
    │   └── CSTestingBrowserCDP.java
    ├── impl/driver/
    │   └── Driver.java             # Optional: for Node server path (serverUrl / bundled driver)
    └── protocol/
        ├── ServerConnection.java
        ├── WebSocketServerConnection.java
        └── ServerStarter.java
```

**Chrome via CDP (default):** When **useChromeDirect** is true, we launch Chrome with `--remote-debugging-port`, connect over WebSocket to the Chrome DevTools Protocol, and drive the browser from Java. No Playwright, no npx, no Node — just Java + Chrome.

## Build

```bash
mvn clean package
```

If you get *"The process cannot access the file ... cstesting-java-0.1.0.jar"*, something (e.g. a running demo or another Maven run) has the JAR open. Close it, or run `.\unlock-and-build.ps1` (stops Java/Chrome and deletes `target`, then runs `mvn package`).

### HTML report after execution

**Option 1 – Automatic (no manual recording):** Use `createBrowserWithReport` so every action (gotoUrl, click, type, waitFor*, etc.) is recorded automatically. The report will have one row per step without calling the report in your code.

```java
import com.cstesting.CSTesting;
import com.cstesting.report.HtmlReport;

HtmlReport report = new HtmlReport().setTestName("My Test");
CSTestingBrowser browser = CSTesting.createBrowserWithReport(
    CSTestingOptions.builder().headless(true).build(),
    report
);
try {
    browser.gotoUrl("https://example.com");   // recorded automatically
    browser.locator("button").click();        // recorded automatically
} finally {
    report.write("target/cstesting-report.html");
}
```

**Option 2 – Manual:** Create the browser normally and call `recordPass` / `recordFail` yourself:

```java
HtmlReport report = new HtmlReport().setTestName("My Test");
CSTestingBrowser browser = CSTesting.createBrowser(...);
try {
    browser.gotoUrl("https://example.com");
    report.recordPass("gotoUrl", "https://example.com");
    browser.locator("button").click();
    report.recordPass("click", "button");
} catch (Exception e) {
    report.recordFail("click", "button", e.getMessage());
} finally {
    report.write("target/cstesting-report.html");
}
```

Open `target/cstesting-report.html` in a browser to see a summary (Total / Passed / Failed / Duration) and a step-by-step table. The demo `OpenBrowserDemo` uses Option 1 and generates the report at `target/cstesting-report.html`.

## Install to local Maven repo

```bash
mvn clean install
```

Then in another project:

```xml
<dependency>
  <groupId>io.github.cstesttool</groupId>
  <artifactId>cstesting-java</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Usage

**Default: launch Chrome via CDP (useChromeDirect = true).** Our own implementation: starts Chrome, connects to CDP over WebSocket, same API as Playwright-style. No Playwright install, no npx.

```java
import com.cstesting.CSTesting;
import com.cstesting.CSTestingBrowser;
import com.cstesting.CSTestingOptions;

CSTestingBrowser browser = CSTesting.createBrowser(
    CSTestingOptions.builder().headless(true).build()
);
browser.gotoUrl("https://example.com");
browser.click("button");
browser.waitForURL("**/login", 10_000);
String url = browser.url();
browser.close();
```

**Connect to an existing server** (no driver on this machine):

```java
CSTestingBrowser browser = CSTesting.createBrowser(
    CSTestingOptions.builder().serverUrl("ws://localhost:9274").build()
);
```

---

## Writing tests

### Introduction

CSTesting assertions are designed for the dynamic web. Use **`assertThat()`** for page-level checks (title, URL) and **`assertThat(locator)`** for element-level checks (visible, attribute, text, value). Assertions throw **`AssertionError`** on failure. Use **`waitForSelector`**, **`waitForLoad`**, or **`waitForURL`** when you need to wait for conditions before asserting or acting.

The example below shows how to write a test using assertions, locators, and selectors.

```java
package org.example;

import com.cstesting.CSTesting;
import com.cstesting.CSTestingBrowser;
import com.cstesting.CSTestingOptions;

public class App {
    public static void main(String[] args) {
        CSTestingBrowser browser = CSTesting.createBrowser(
            CSTestingOptions.builder().headless(true).build()
        );
        try {
            browser.gotoUrl("https://example.com");

            // Expect the page title to contain a substring.
            browser.assertThat().hasTitle("Example");

            // Create a locator and expect an attribute to match.
            var link = browser.locator("a").first();
            browser.assertThat(link).hasAttribute("href");

            // Click the link (e.g. "More information...").
            link.click();

            // Wait for navigation and expect a heading to be visible.
            browser.waitForLoad(5_000);
            browser.assertThat(browser.locator("h1")).isVisible();
        } finally {
            browser.close();
        }
    }
}
```

### Assertions

Use **`browser.assertThat()`** for the page and **`browser.assertThat(locator)`** for elements. Assertions are fluent and throw `AssertionError` if the condition is not met.

**Page assertions:**

```java
// Title contains or equals the expected string.
browser.assertThat().hasTitle("Example Domain");

// URL contains the string or matches a pattern (use * as wildcard).
browser.assertThat().hasURL("https://example.com");
browser.assertThat().hasURL("**/example*");
```

**Element assertions:**

```java
// Visibility and state.
browser.assertThat(browser.locator("button")).isVisible();
browser.assertThat(browser.locator("#name")).isEditable();
browser.assertThat(browser.locator("input")).isEnabled();
browser.assertThat(browser.locator("[disabled]")).isDisabled();

// Text and value.
browser.assertThat(browser.locator("h1")).hasText("Example");
browser.assertThat(browser.locator("h1")).containText("Example");
browser.assertThat(browser.locator("#search")).hasValue("query");
browser.assertThat(browser.locator("#search")).containValue("query");

// Attributes.
browser.assertThat(browser.locator("a").first()).hasAttribute("href");
browser.assertThat(browser.locator("a").first()).hasAttribute("href", "https://example.com");

// Count of matching elements.
browser.assertThat(browser.locator("button")).count(3);
browser.assertThat(browser.locator("li")).hasCount(5);
```

### Locators

Locators represent a way to find element(s) on the page. Create them with **`browser.locator(selector)`** and use them for actions (`.click()`, `.type()`, etc.) and assertions. If the selector matches **multiple** elements, use **`.first()`**, **`.last()`**, or **`.nth(index)`** before performing an action.

**Selector formats:** CSS (default), XPath (`//button` or `xpath=//button`), `id=value`, `name=value`, or any `attr=value`.

```java
// By text (XPath).
var getStarted = browser.locator("//a[contains(.,'Get Started')]").first();
browser.assertThat(getStarted).hasAttribute("href", "/docs/intro");
getStarted.click();

// By CSS or id/name.
browser.assertThat(browser.locator("h1")).isVisible();
browser.assertThat(browser.locator("id=submit")).isEnabled();
browser.locator("name=email").type("user@example.com");
```

See [Locators](#locators) for the full list of selector formats and locator methods.

### Test isolation

Create a **new browser** for each test so that tests do not share state (cookies, storage, or tabs). Open the browser at the start of the test and close it in a `finally` block (or use try-with-resources if you wrap the API).

```java
public void testLogin() {
    CSTestingBrowser browser = CSTesting.createBrowser(
        CSTestingOptions.builder().headless(true).build()
    );
    try {
        browser.gotoUrl("https://example.com/login");
        browser.locator("name=user").type("alice");
        browser.locator("name=pass").type("secret");
        browser.locator("button[type=submit]").click();
        browser.assertThat().hasURL("**/welcome");
    } finally {
        browser.close();
    }
}
```

Each test that creates its own browser gets a clean profile and does not interfere with others.

### Annotations (TestNG-style)

CSTesting provides annotations and a runner so you can write tests in a TestNG-like style: **`@CSTest`**, **`@BeforeSuite`**, **`@AfterSuite`**, **`@BeforeClass`**, **`@AfterClass`**, **`@BeforeMethod`**, **`@AfterMethod`**. Extend **`CSTestingTestBase`** to get a **`browser`** field injected before each test, then run the class with **`CSTestingRunner.run(YourTestClass.class)`**.

**Annotations:**

| Annotation      | When it runs |
|-----------------|--------------|
| `@BeforeSuite`  | Once before the entire run (before any class or test; no browser yet). |
| `@AfterSuite`   | Once after the entire run (after all tests and @AfterClass; browser already closed). |
| `@BeforeClass`  | Once before any test in the class (no browser yet). |
| `@AfterClass`   | Once after all tests in the class (browser already closed). |
| `@BeforeMethod` | Before each `@CSTest` method (browser is set). |
| `@AfterMethod`  | After each `@CSTest` method (browser still open). |
| `@CSTest`       | The test method. |

**Execution order:** `@BeforeSuite` → `@BeforeClass` → (for each test: `@BeforeMethod` → `@CSTest` → `@AfterMethod`) → `@AfterClass` → `@AfterSuite`.

**Example:**

```java
import com.cstesting.annotations.*;
import com.cstesting.runner.CSTestingRunner;
import com.cstesting.runner.CSTestingTestBase;

public class MyTest extends CSTestingTestBase {

    @BeforeMethod
    public void setUp() {
        // browser is already created and set
    }

    @AfterMethod
    public void tearDown() {
        // runs before browser is closed
    }

    @CSTest(description = "Check example.com title")
    public void testTitle() {
        browser.gotoUrl("https://example.com");
        browser.assertThat().hasTitle("Example");
    }

    @CSTest
    public void testHeading() {
        browser.gotoUrl("https://example.com");
        browser.assertThat(browser.locator("h1")).isVisible();
    }
}
// Run: mvn exec:java -Dexec.mainClass="com.cstesting.runner.CSTestingRunner" -Dexec.args="com.example.MyTest"
```

**Run the tests** (no `main()` needed in the test class):

```bash
mvn exec:java -Dexec.mainClass="com.cstesting.runner.CSTestingRunner" -Dexec.args="com.example.MyTest"
```

Or from code: `CSTestingRunner.run(MyTest.class);`. To use a visible browser, override **`getBrowserOptions()`** in your test class:

```java
@Override
protected CSTestingOptions getBrowserOptions() {
    return CSTestingOptions.builder().headless(false).build();
}
```

---

## Actions

### Introduction

CSTesting can interact with HTML elements such as text inputs, checkboxes, radio buttons, select options, mouse clicks (click, double-click, right-click), typing text, hover, and drag-and-drop. Use **locators** (e.g. `browser.locator("selector")`) to target elements; all actions accept either a selector string or a `Locator`. When multiple elements match, use `.first()`, `.last()`, or `.nth(index)` before performing an action.

### Text input

**`type(selector, text)`** or **`locator.type(text)`** is the main way to fill form fields. It focuses the element, clears any existing value, and types the given text. It works for `<input>`, `<textarea>`, and similar elements.

```java
// By selector
browser.type("id=username", "Peter");
browser.type("name=email", "user@example.com");

// By locator (chain with first() if multiple match)
browser.locator("id=username").type("Peter");
browser.locator("[name='Birth date']").type("2020-02-02");
browser.locator("textarea").first().type("Hello");
```

### Checkboxes and radio buttons

Use **`check(selector)`** / **`check(Locator)`** to check a checkbox or select a radio button, and **`uncheck(selector)`** / **`uncheck(Locator)`** to uncheck. Works with `input[type=checkbox]` and `input[type=radio]`. No-op if the element is already in the desired state.

```java
// Check the checkbox
browser.locator("id=agree").check();
browser.check("name=subscribe");

// Assert the checked state
browser.assertThat(browser.locator("name=subscribe")).isVisible();  // or use isSelected via getValue/evaluate if needed

// Select a radio button
browser.locator("id=size-xl").check();
browser.check("name=size");  // if only one with name=size; otherwise use locator("name=size").nth(1).check()
```

### Select options

Use **`select(selector, option)`** for a single-select dropdown and **`selectOptions(selector, options...)`** for one or more options (single or multi-select). Option can be a **value**, **visible label**, or **0-based index** (Integer). Use **`deselectOptions`** to clear options in a multi-select.

```java
// Single selection by value or label
browser.locator("#color").select("blue");
browser.locator("#color").select("Blue");   // label

// Single selection by index
browser.locator("#color").select(2);

// Multi-select
browser.locator("#colors").selectOptions("red", "green", "blue");
browser.locator("#colors").selectOptions(0, 2, 4);

// Deselect
browser.locator("#colors").deselectOptions("blue");

// Read selected
List<String> values = browser.getSelectedValues(browser.locator("#colors"));
List<String> labels = browser.getSelectedLabels(browser.locator("#colors"));
```

### Mouse click

**`click(selector)`** / **`click(Locator)`** performs a left click. **`doubleClick`** and **`rightClick`** are also available. **`hover(selector)`** / **`hover(Locator)`** moves the mouse to the element. Actions use the **center** of the element.

```java
// Generic click
browser.locator("button").click();
browser.click("id=submit");

// Double click
browser.locator("text=Item").doubleClick();
browser.doubleClick("#item");

// Right click (context menu)
browser.locator("text=Item").rightClick();
browser.rightClick("#row");

// Hover
browser.locator("#menu").hover();
```

Under the hood, actions (when using the CDP implementation) scroll the element into view and wait for it to be visible before performing the click or other action.

### Type characters

Use **`type(selector, text)`** or **`locator.type(text)`** to enter text. This replaces the current value (like a “fill”). For character-by-character input or special key events, use **`evaluate()`** to run JavaScript (e.g. dispatch `KeyboardEvent`) if your app relies on key-level handling.

```java
browser.locator("#area").type("Hello World!");
```

### Drag and drop

Use **`dragAndDrop(fromSelector, toSelector)`** or **`fromLocator.dragAndDrop(toLocator)`** to drag one element to another. The action moves the mouse to the source, presses the button, moves to the target, and releases.

```java
browser.dragAndDrop("#source", "#target");
browser.locator(".card").first().dragAndDrop(browser.locator(".drop-zone"));
```

### Scrolling

CSTesting scrolls the target element into view before actions like click when needed. For explicit scrolling, use:

- **`scrollToPageTop()`** / **`scrollToPageBottom()`** – scroll to top or bottom of the page.
- **`scrollUp()`** / **`scrollDown()`** – scroll by one viewport height.
- **`scrollBy(dx, dy)`** – scroll by a pixel offset (e.g. `scrollBy(0, 300)` to scroll down 300px).
- **`scrollToSelector(selector)`** / **`locator.scrollToSelector()`** – scroll until the element is in view (centered).

```java
// Scroll so the button is visible, then click
browser.scrollToSelector("#submit");
browser.locator("#submit").click();

// Scroll the page
browser.scrollToPageBottom();
browser.scrollBy(0, 300);
browser.locator("#footer").scrollToSelector();   // scroll element into view
```

For more scrolling options, see [Scrolling](#scrolling).

---

## Browser commands

All methods are on `CSTestingBrowser`. Selectors are CSS (e.g. `"button"`, `"#id"`, `".class"`).

| Method | Description |
|--------|-------------|
| `gotoUrl(String url)` | Navigate to a URL. |
| `click(String selector)` | Click the element matching the selector. |
| `hover(String selector)` | Move mouse to the element (center). |
| `doubleClick(String selector)` | Double-click the element. |
| `rightClick(String selector)` | Right-click (context menu) the element. |
| `dragAndDrop(String from, String to)` | Drag from source element to target element. |
| `scrollToPageTop()` / `scrollToPageBottom()` / `scrollUp()` / `scrollDown()` / `scrollBy(dx,dy)` / `scrollToSelector(selector)` | See [Scrolling](#scrolling). |
| `type(String selector, String text)` | Type text into the element (clears existing value). |
| `select(String selector, Object option)` | Select **one** option in a single-select dropdown (option = value, visible label, or 0-based index). |
| `selectOptions(String selector, Object... options)` | Select one or more options (single or multi-select). |
| `deselectOptions(String selector, Object... options)` | Deselect options in a multi-select. |
| `getSelectedValues(String selector)` | List of selected option values. |
| `getSelectedLabels(String selector)` | List of selected option visible text. |
| `check(String selector)` | Check a checkbox or radio (no-op if already checked). |
| `uncheck(String selector)` | Uncheck a checkbox (no-op if already unchecked). |
| `url()` | Return the current page URL. |
| `content()` | Return the full HTML of the page. |
| `evaluate(String expression)` | Run JavaScript in the page and return the result. |
| `isVisible(String selector)` | Return whether the element is visible. |
| `isDisabled(String selector)` | Return whether the element is disabled. |
| `isSelected(String selector)` | Return whether the checkbox/radio is selected. |
| `getTextContent(String selector)` | Return the text content of the element. |
| `frame(String iframeSelector)` | Switch to an iframe; returns a browser scoped to that frame. |
| `locator(String selector)` | Create a locator (supports CSS, XPath, `id=`, `name=`, any attribute). Use with `.first()`, `.last()`, `.nth(i)` when multiple match. |
| `close()` | Close the browser and release resources. |

All selector-based methods also accept a `Locator` instead of a String (e.g. `click(Locator)`, `type(Locator, String)`).

---

## Locators

Use `browser.locator(selector)` to create a **Locator**. If the selector matches **more than one** element and you perform an action (click, type, etc.) without choosing which one, an error is thrown. Use **`.first()`**, **`.last()`**, or **`.nth(index)`** to target one element.

**Selector formats:**

| Format | Example | Resolved to |
|--------|---------|-------------|
| CSS | `"button"`, `"#id"`, `".class"` | As-is |
| XPath | `"xpath=//button"` or `"//button"` | XPath |
| id | `"id=myId"` | `#myId` (CSS) |
| name | `"name=submit"` | `[name="submit"]` (CSS) |
| Any attribute | `"data-testid=foo"` | `[data-testid="foo"]` (CSS) |

**Methods on Locator:**

| Method | Description |
|--------|-------------|
| `first()` | Select the first matching element (index 0). |
| `last()` | Select the last matching element. |
| `nth(int index)` | Select the element at the given 0-based index. |

**Example:**

```java
// Single match – use directly (fluent)
browser.locator("button").click();

// Multiple matches – pick one, then act (otherwise error)
browser.locator("input[name='q']").first().click();
browser.locator("li").nth(2).click();
browser.locator("a").last().click();

// Attribute shortcuts – chain .first() / .last() / .nth(i) then .type() or .click()
browser.locator("id=username").type("admin");
browser.locator("name=password").type("secret");
browser.locator("data-testid=submit").click();
```

---

## Mouse actions

All mouse actions use the **center** of the element and support both selector and `Locator`. Implemented via CDP `Input.dispatchMouseEvent`.

| Method | Description |
|--------|-------------|
| `hover(selector)` / `hover(Locator)` | Move mouse to the element. |
| `doubleClick(selector)` / `doubleClick(Locator)` | Double-click the element. |
| `rightClick(selector)` / `rightClick(Locator)` | Right-click (context menu). |
| `dragAndDrop(fromSelector, toSelector)` / `dragAndDrop(Locator from, Locator to)` | Drag from source to target element. |

**Example:**

```java
browser.locator("#menu").hover();
browser.locator("#item").doubleClick();
browser.locator("#row").rightClick();
browser.dragAndDrop("#source", "#target");
browser.locator(".card").first().dragAndDrop(browser.locator(".drop-zone"));  // fluent: from.dragAndDrop(to)
```

---

## Dropdowns (single and multi-select)

| Method | Description |
|--------|-------------|
| `select(selector, option)` / `select(Locator, option)` | Select **one** option. `option` = String (value or label) or Integer (0-based index). |
| `selectOptions(selector, options...)` / `selectOptions(Locator, options...)` | Select one or more options. For single-select, first option in list is chosen; for multi-select, all given options are selected. |
| `deselectOptions(selector, options...)` / `deselectOptions(Locator, options...)` | Deselect the given options in a multi-select. |
| `getSelectedValues(selector)` / `getSelectedValues(Locator)` | Return list of selected option **values**. |
| `getSelectedLabels(selector)` / `getSelectedLabels(Locator)` | Return list of selected option **visible text**. |

**Example – single select:**

```java
browser.locator("#country").select("UK");           // by value or label
browser.locator("#country").select(2);              // by 0-based index
browser.select("name=country", "Germany");
```

**Example – multi-select:**

```java
browser.locator("#colors").selectOptions("Red", "Blue");
browser.locator("#colors").selectOptions(0, 2, 4); // by index
browser.locator("#colors").deselectOptions("Blue");
List<String> selected = browser.getSelectedValues(browser.locator("#colors"));
List<String> labels = browser.getSelectedLabels(browser.locator("#colors"));
```

---

## Assertions

Use **`browser.assertThat(locator)`** for element assertions and **`browser.assertThat()`** for page assertions. All methods throw **`AssertionError`** on failure and return the same `Assertion` instance for chaining.

**Element assertions** (use after `assertThat(Locator)`):

| Method | Description |
|--------|-------------|
| `isVisible()` | Assert the element is visible. |
| `isEditable()` | Assert the element is editable (input/textarea not disabled, not readonly). |
| `isEmpty()` | Assert value or text is empty. |
| `isEnabled()` | Assert the element is not disabled. |
| `isDisabled()` | Assert the element is disabled. |
| `contains(String)` | Assert text or value contains the given string. |
| `hasText(String)` | Assert text content contains or equals the string. |
| `containText(String)` | Assert text content contains the string. |
| `hasValue(String)` | Assert input/select value equals the string. |
| `containValue(String)` | Assert input/select value contains the string. |
| `hasAttribute(String name)` | Assert the element has the attribute (any value). |
| `hasAttribute(String name, String value)` | Assert the attribute equals the value. |
| `count(int expected)` | Assert the number of matching elements equals the value. |
| `hasCount(int)` | Same as `count(int)`. |

**Page assertions** (use after `assertThat()`):

| Method | Description |
|--------|-------------|
| `hasTitle(String)` | Assert page title equals or contains the string. |
| `hasURL(String)` | Assert URL equals, contains, or matches pattern (use `*` as wildcard). |

Example:

```java
browser.gotoUrl("https://example.com");
browser.assertThat().hasTitle("Example");
browser.assertThat().hasURL("*example*");

browser.locator("#name").type("John");
browser.assertThat(browser.locator("#name")).isVisible().isEditable().hasValue("John");
browser.assertThat(browser.locator("h1")).hasText("Welcome");
browser.assertThat(browser.locator("button")).count(2);
browser.assertThat(browser.locator("a[href]").first()).hasAttribute("href");
```

---

## Browser navigation commands

| Method | Description |
|--------|-------------|
| `back()` | Go back in history. |
| `forward()` | Go forward in history. |
| `refresh()` | Reload the current page. |

---

## Frames (iframes)

Switch to an iframe to run commands inside it. Returns a `CSTestingBrowser` scoped to that frame (same API: click, type, locator, etc. act inside the frame).

| Method | Description |
|--------|-------------|
| `frame(String iframeSelector)` | Switch to the iframe matching the selector (e.g. `"iframe"`, `"iframe#my"`, `"iframe[src*='embed']"`). |

**Example:**

```java
browser.gotoUrl("https://example.com/page-with-iframe");
browser.waitForSelector("iframe", 10_000);   // ensure iframe is in DOM
CSTestingBrowser frame = browser.frame("iframe");
frame.locator("button").click();             // click inside the iframe
frame.locator("#input").type("hello");
// back to main page: use the original browser reference for main-page actions
browser.locator("body").click();
```

**Nested frames:** You can call `frame(selector)` from inside an iframe to switch to an iframe within that frame.

```java
CSTestingBrowser outer = browser.frame("iframe#outer");
CSTestingBrowser inner = outer.frame("iframe#inner");
inner.locator("button").click();
```

**Note (CDP):** The iframe must have loaded before calling `frame()` so its execution context exists. Use `waitForSelector("iframe", timeout)` first.

---

## Windows and tabs

Use separate browser references per tab so you never need to switch back (Playwright-style). Handles are opaque IDs (CDP: WebSocket URLs for each page target). **Supported with CDP (useChromeDirect);** Impl/Frame delegate to the server.

| Method | Description |
|--------|-------------|
| `getWindowHandles()` | List of all tab/window handles. |
| `getCurrentWindowHandle()` | Handle of the current tab. |
| `switchToWindow(String handle)` | Switch to the tab with the given handle. |
| `newTab()` | Open a new tab (about:blank) and **return** a browser for it; this browser stays on the current tab. |
| `getPages()` | All open tabs as separate `CSTestingBrowser` instances (use any reference without switching). |

**Example – no switch needed (recommended):**

```java
browser.gotoUrl("https://example.com");
CSTestingBrowser parent = browser;                    // keep reference to first tab
CSTestingBrowser child = browser.newTab();             // new tab; parent still points to first tab
child.gotoUrl("https://example.com/other");
parent.locator("h1").click();                          // work on first tab
child.locator("a").click();                            // work on second tab – no switchToWindow
```

**Example – using handles and switch:**

```java
browser.gotoUrl("https://example.com");
String first = browser.getCurrentWindowHandle();
browser.newTab();
browser.gotoUrl("https://example.com/other");
browser.switchToWindow(first);                         // back to first tab
```

**Example – get all tabs as list:**

```java
List<CSTestingBrowser> pages = browser.getPages();
pages.get(0).gotoUrl("https://example.com");
pages.get(1).locator("button").click();
```

**Note (CDP):** `switchToWindow`, `newTab`, and `getPages` must be called from the main page (not from inside a frame). With `newTab()` and `getPages()`, the current browser’s connection is unchanged so you can keep using the parent reference without switching back.

---

## Waits

Condition-based waits **exit as soon as the condition is met** (or when the timeout is reached). All timeouts are optional; default is 30 seconds where applicable.

| Method | Description |
|--------|-------------|
| `waitForSelector(String selector, Integer timeoutMs)` | Wait until an element matching the selector is present in the DOM. |
| `waitForURL(String urlOrPattern, Integer timeoutMs)` | Wait until the current URL matches the string or pattern (e.g. `"**/login"`). |
| `waitForLoad()` | Wait until `document.readyState` is complete (default 30s). |
| `waitForLoad(Integer timeoutMs)` | Same as above with an explicit timeout. |
| `waitForPage(Integer timeoutMs)` | Wait until the page is ready (same as `waitForLoad` with timeout). |
| `waitForNetworkLoad(Integer timeoutMs)` | Wait until the page is loaded and network is idle. |
| `waitForTime(long millis)` | Fixed delay in milliseconds (e.g. `waitForTime(5000)` for 5 seconds). |

Example:

```java
browser.gotoUrl("https://example.com");
browser.waitForSelector("h1", 10_000);
browser.waitForLoad(15_000);
browser.waitForTime(1000);
browser.waitForURL("**/success", 20_000);
```

---

## Scrolling

| Method | Description |
|--------|-------------|
| `scrollToPageTop()` | Scroll to the top of the page. |
| `scrollToPageBottom()` | Scroll to the bottom of the page. |
| `scrollUp()` | Scroll up by one viewport height. |
| `scrollDown()` | Scroll down by one viewport height. |
| `scrollBy(int deltaX, int deltaY)` | Scroll by pixel offset (positive = down/right, negative = up/left). |
| `scrollToSelector(String selector)` / `scrollToSelector(Locator)` | Scroll until the element is in view (centered). |

**Example:**

```java
browser.scrollToPageTop();
browser.scrollToPageBottom();
browser.scrollDown();
browser.scrollUp();
browser.scrollBy(0, 300);   // scroll down 300px
browser.scrollToSelector("#footer");
browser.locator("#section-2").scrollToSelector();  // fluent
```

---

## Alerts (JavaScript dialogs)

Handle `alert()`, `confirm()`, and `prompt()` dialogs. Call **before** the action that opens the dialog.

| Method | Description |
|--------|-------------|
| `acceptNextAlert()` | Next dialog will be accepted (OK). Use for alert or confirm. |
| `acceptNextAlert(String promptText)` | Next prompt will be accepted with the given text. |
| `dismissNextAlert()` | Next confirm or prompt will be dismissed (Cancel). |
| `getLastAlertMessage()` | Message of the last dialog that opened (for assertions). |

**Example:**

```java
browser.acceptNextAlert();
browser.locator("#btnAlert").click();   // triggers alert("Hello")
String msg = browser.getLastAlertMessage();  // "Hello"
// assert "Hello".equals(msg) or use your test framework

browser.dismissNextAlert();
browser.locator("#btnConfirm").click(); // triggers confirm("Sure?") -> Cancel

browser.acceptNextAlert("my input");
browser.locator("#btnPrompt").click();  // prompt("Name?") accepted with "my input"
```

---

**Chrome path (useChromeDirect):**

- **Bundled driver:** set `-Dcstesting.cli.dir=/path/to/driver` (directory with `node` and `package/cli.js` or `server.js`) so users don’t need Node installed.

## Troubleshooting

### "Chrome did not expose CDP" / "Chrome not found"

When **useChromeDirect** is true, the client launches Chrome and connects via CDP. Install [Chrome](https://www.google.com/chrome/) or set `-Dcstesting.chrome.path=/path/to/chrome`. If another app is using port 9222, close it or use **serverUrl** to connect to an existing server instead.

## Publish to Maven Central

See the **EasyTesting** repo docs: `docs/publish-java-maven-gradle.md` for Sonatype OSSRH, GPG signing, and Gradle equivalent.

## Note

- **Chrome direct = our own CDP implementation:** No Playwright, no npx. When `useChromeDirect(true)` (default), we launch Chrome and talk to it via the Chrome DevTools Protocol from Java.
- Optional: connect to a CSTesting Node server with **serverUrl**; the wire protocol is in the EasyTesting repo under `docs/multi-language-support.md`.
