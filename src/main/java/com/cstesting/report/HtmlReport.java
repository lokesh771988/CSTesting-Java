package com.cstesting.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Records test steps and generates a self-contained HTML report at the end of execution.
 * Use {@link #recordPass(String, String)} / {@link #recordFail(String, String, String)} for each step,
 * then call {@link #write(String)} or {@link #write(Path)} to generate the report file.
 * <p>
 * Example:
 * <pre>
 * HtmlReport report = new HtmlReport();
 * report.setTestName("OpenBrowserDemo");
 * try {
 *     browser.gotoUrl("https://example.com");
 *     report.recordPass("gotoUrl", "https://example.com");
 *     browser.locator("h1").click();
 *     report.recordPass("click", "h1");
 * } catch (Exception e) {
 *     report.recordFail("click", "h1", e.getMessage());
 * }
 * report.write("target/cstesting-report.html");
 * </pre>
 */
public final class HtmlReport {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private String testName = "CSTesting Run";
    private final long startTime = System.currentTimeMillis();
    private final List<StepEntry> steps = new ArrayList<>();
    /** When set (e.g. from config run), report summary shows "Failed at step: N". 0-based. */
    private Integer failedStepIndex;

    public HtmlReport() {}

    /** Sets the report title (e.g. test or suite name). */
    public HtmlReport setTestName(String testName) {
        this.testName = testName != null ? testName : this.testName;
        return this;
    }

    /** Records a passed step. */
    public HtmlReport recordPass(String action, String description) {
        steps.add(new StepEntry(action, description, true, null));
        return this;
    }

    /** Records a failed step with an optional error message. */
    public HtmlReport recordFail(String action, String description, String errorMessage) {
        steps.add(new StepEntry(action, description, false, errorMessage));
        return this;
    }

    /** Set the failed step index (0-based) when report is from a config run. Summary will show "Failed at step: N". */
    public HtmlReport setFailedStepIndex(Integer failedStepIndex) {
        this.failedStepIndex = failedStepIndex;
        return this;
    }

    /** Writes the HTML report to the given path (e.g. "target/cstesting-report.html"). */
    public Path write(String filePath) throws IOException {
        return write(Paths.get(filePath));
    }

    /** Writes the HTML report to the given path. */
    public Path write(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        String html = buildHtml();
        Files.write(path, html.getBytes(StandardCharsets.UTF_8));
        return path;
    }

    private String buildHtml() {
        long durationMs = System.currentTimeMillis() - startTime;
        int passed = (int) steps.stream().filter(s -> s.pass).count();
        int failed = steps.size() - passed;
        String generated = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            StepEntry s = steps.get(i);
            String statusClass = s.pass ? "pass" : "fail";
            String statusText = s.pass ? "Pass" : "Fail";
            String time = LocalDateTime.now().format(TIME_FMT); // step time could be stored in StepEntry if needed
            rows.append("<tr class=\"").append(statusClass).append("\"><td>").append(i + 1).append("</td><td>")
                .append(escape(s.action)).append("</td><td>").append(escape(s.description))
                .append("</td><td>").append(statusText).append("</td><td>")
                .append(s.errorMessage != null ? escape(s.errorMessage) : "-").append("</td></tr>");
        }

        String failedStepLine = (failedStepIndex != null && failedStepIndex >= 0)
            ? "<span class=\"failed-step\">Failed at step: " + (failedStepIndex + 1) + "</span>\n"
            : "";
        return "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n"
            + "<title>" + escape(testName) + " - CSTesting Report</title>\n<style>\n"
            + "body{font-family:Segoe UI,Helvetica,Arial,sans-serif;margin:24px;background:#f5f5f5;}\n"
            + "h1{color:#333;margin:0 0 8px 0;}\n"
            + ".meta{color:#666;font-size:14px;margin-bottom:20px;}\n"
            + ".summary{display:flex;flex-wrap:wrap;gap:16px;margin-bottom:24px;align-items:center;}\n"
            + ".summary span{padding:8px 16px;border-radius:6px;font-weight:600;}\n"
            + ".summary .total{background:#e3f2fd;color:#1565c0;}\n"
            + ".summary .pass{background:#e8f5e9;color:#2e7d32;}\n"
            + ".summary .fail{background:#ffebee;color:#c62828;}\n"
            + ".summary .duration{background:#f3e5f5;color:#6a1b9a;}\n"
            + ".summary .failed-step{background:#ffcc80;color:#e65100;}\n"
            + "table{width:100%;border-collapse:collapse;background:white;box-shadow:0 1px 3px rgba(0,0,0,0.1);}\n"
            + "th,td{padding:10px 12px;text-align:left;border-bottom:1px solid #eee;}\n"
            + "th{background:#37474f;color:white;}\n"
            + "tr.pass{}\n"
            + "tr.fail{background:#ffebee;}\n"
            + "td:nth-child(4){font-weight:600;}\n"
            + "</style>\n</head>\n<body>\n"
            + "<h1>" + escape(testName) + "</h1>\n"
            + "<div class=\"meta\">Generated: " + escape(generated) + "</div>\n"
            + "<div class=\"summary\">\n"
            + "<span class=\"total\">Total: " + steps.size() + "</span>\n"
            + "<span class=\"pass\">Passed: " + passed + "</span>\n"
            + "<span class=\"fail\">Failed: " + failed + "</span>\n"
            + "<span class=\"duration\">Duration: " + (durationMs / 1000.0) + "s</span>\n"
            + failedStepLine
            + "</div>\n"
            + "<table>\n<thead><tr><th>#</th><th>Action</th><th>Description</th><th>Status</th><th>Error</th></tr></thead>\n<tbody>\n"
            + rows
            + "</tbody>\n</table>\n</body>\n</html>";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static final class StepEntry {
        final String action;
        final String description;
        final boolean pass;
        final String errorMessage;

        StepEntry(String action, String description, boolean pass, String errorMessage) {
            this.action = action;
            this.description = description;
            this.pass = pass;
            this.errorMessage = errorMessage;
        }
    }
}
