package com.cstesting;

/**
 * Browser automation API (mirrors CSTesting Node API).
 * Commands are sent to the CSTesting Node server over the wire protocol.
 */
public interface CSTestingBrowser {

    void gotoUrl(String url);

    void click(String selector);

    void type(String selector, String text);

    void select(String selector, Object option);

    void check(String selector);

    void uncheck(String selector);

    void waitForSelector(String selector, Integer timeoutMs);

    void waitForURL(String urlOrPattern, Integer timeoutMs);

    void waitForLoad();

    String url();

    String content();

    Object evaluate(String expression);

    boolean isVisible(String selector);

    boolean isDisabled(String selector);

    boolean isSelected(String selector);

    String getTextContent(String selector);

    CSTestingBrowser frame(String iframeSelector);

    void close();
}
