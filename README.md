# CSTesting Java Client

**Repo:** [github.com/lokesh771988/CSTesting-Java](https://github.com/lokesh771988/CSTesting-Java)

Standalone Java project for the **CSTesting** browser automation client. If you **don’t install Node.js** on the machine running the Java tests, you must **connect to an existing server** (use `serverUrl`); the default “auto-start” mode requires Node.js on that machine. It connects to the CSTesting Node server (WebSocket) and exposes the same API: `goto`, `click`, `type`, `waitForURL`, etc.

## Prerequisites

- **Java 11+**
- **Node.js** — only needed in two cases:
  - **You use auto-start** (default): `CSTesting.createBrowser()` or `createBrowser(options)` without `serverUrl`. The Java client runs `npx cstesting server --port=9274` on the same machine, so **Node.js must be installed** there. Without Node.js, that will fail.
  - **You connect to an existing server**: `CSTestingOptions.builder().serverUrl("ws://host:9274").build()`. The machine that runs your Java tests **does not need Node.js**; only the machine (or CI) that runs the CSTesting server needs Node.js.

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
    └── protocol/
        ├── ServerConnection.java
        ├── WebSocketServerConnection.java
        └── ServerStarter.java
```

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
  <groupId>com.cstesting</groupId>
  <artifactId>cstesting-java</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Usage

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

Connect to an existing server:

```java
CSTestingBrowser browser = CSTesting.createBrowser(
    CSTestingOptions.builder().serverUrl("ws://localhost:9274").build()
);
```

## Publish to Maven Central

See the **EasyTesting** repo docs: `docs/publish-java-maven-gradle.md` for Sonatype OSSRH, GPG signing, and Gradle equivalent.

## Note

The CSTesting Node server (`npx cstesting server --port=9274`) must be implemented in the main EasyTesting repo. This Java project is ready to build and publish; the wire protocol is described in the EasyTesting repo under `docs/multi-language-support.md`.
