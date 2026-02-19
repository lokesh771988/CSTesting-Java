# CSTesting Java Client

**Repo:** [github.com/cstesttool/CSTesting-Java](https://github.com/cstesttool/CSTesting-Java)

CSTesting for Java is **our own tool** that works like [Playwright for Java](https://github.com/microsoft/playwright-java): **no Playwright dependency, no npx, no Node server.** When you use **useChromeDirect** (default), the client launches Chrome via the **Chrome DevTools Protocol (CDP)** — pure Java, using your system Chrome.

## Prerequisites

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
