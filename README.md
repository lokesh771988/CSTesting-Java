# CSTesting Java Client

**Repo:** [github.com/lokesh771988/CSTesting-Java](https://github.com/lokesh771988/CSTesting-Java)

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

## Install to local Maven repo

```bash
mvn clean install
```

Then in another project:

```xml
<dependency>
  <groupId>io.github.lokesh771988</groupId>
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
