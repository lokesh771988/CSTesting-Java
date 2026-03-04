package com.cstesting.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parses a .conf file into a list of {@link ConfigStep}. Format: one step per line;
 * line = command followed by space-separated args. Use double quotes for values with spaces.
 * # starts a comment. Blank lines are ignored.
 * <p>
 * Commands: goto, type, click, doubleClick, rightClick, hover, wait, screenshot, frame,
 * dialogAccept, dialogAcceptPrompt, dialogDismiss, check, uncheck, select,
 * verifyText, assertAttribute, close.
 */
public final class ConfigParser {

    private static final Pattern QUOTED = Pattern.compile("\"([^\"]*)\"|'([^']*)'");

    /**
     * Parse a config file into steps.
     */
    public static List<ConfigStep> parse(Path path) throws IOException {
        return parse(Files.newBufferedReader(path, StandardCharsets.UTF_8));
    }

    /**
     * Parse from a reader.
     */
    public static List<ConfigStep> parse(Reader reader) throws IOException {
        List<ConfigStep> steps = new ArrayList<>();
        try (BufferedReader br = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                ConfigStep step = parseLine(line);
                if (step != null) steps.add(step);
            }
        }
        return steps;
    }

    static ConfigStep parseLine(String line) {
        List<String> tokens = tokenize(line);
        if (tokens.isEmpty()) return null;
        String cmd = tokens.get(0).toLowerCase();
        switch (cmd) {
            case "goto":
                return ConfigStep.builder(ConfigStep.Type.GOTO).arg("url", get(tokens, 1)).build();
            case "type":
                return ConfigStep.builder(ConfigStep.Type.TYPE).arg("selector", get(tokens, 1)).arg("text", get(tokens, 2)).build();
            case "click":
                return ConfigStep.builder(ConfigStep.Type.CLICK).arg("selector", get(tokens, 1)).build();
            case "doubleclick":
                return ConfigStep.builder(ConfigStep.Type.DOUBLE_CLICK).arg("selector", get(tokens, 1)).build();
            case "rightclick":
                return ConfigStep.builder(ConfigStep.Type.RIGHT_CLICK).arg("selector", get(tokens, 1)).build();
            case "hover":
                return ConfigStep.builder(ConfigStep.Type.HOVER).arg("selector", get(tokens, 1)).build();
            case "wait":
                return ConfigStep.builder(ConfigStep.Type.WAIT).arg("timeout", get(tokens, 1)).build();
            case "screenshot":
                return ConfigStep.builder(ConfigStep.Type.SCREENSHOT).arg("path", get(tokens, 1)).build();
            case "frame":
                return ConfigStep.builder(ConfigStep.Type.FRAME).arg("selector", get(tokens, 1)).build();
            case "dialogaccept":
                return ConfigStep.builder(ConfigStep.Type.DIALOG_ACCEPT).arg("promptText", get(tokens, 1)).build();
            case "dialogacceptprompt":
                return ConfigStep.builder(ConfigStep.Type.DIALOG_ACCEPT_PROMPT).arg("promptText", get(tokens, 1)).build();
            case "dialogdismiss":
                return ConfigStep.builder(ConfigStep.Type.DIALOG_DISMISS).build();
            case "check":
                return ConfigStep.builder(ConfigStep.Type.CHECK).arg("selector", get(tokens, 1)).build();
            case "uncheck":
                return ConfigStep.builder(ConfigStep.Type.UNCHECK).arg("selector", get(tokens, 1)).build();
            case "select":
                return ConfigStep.builder(ConfigStep.Type.SELECT).arg("selector", get(tokens, 1)).arg("option", get(tokens, 2)).build();
            case "verifytext":
                return ConfigStep.builder(ConfigStep.Type.VERIFY_TEXT).arg("selector", get(tokens, 1)).arg("expected", get(tokens, 2)).build();
            case "assertattribute":
                return ConfigStep.builder(ConfigStep.Type.ASSERT_ATTRIBUTE).arg("selector", get(tokens, 1)).arg("attr", get(tokens, 2)).arg("value", get(tokens, 3)).build();
            case "asserttextequalsattribute": {
                String a1 = get(tokens, 1), a2 = get(tokens, 2), a3 = get(tokens, 3);
                String textSel = a1;
                String attrSel = a3 != null ? a2 : a1;
                String attr = a3 != null ? a3 : a2;
                return ConfigStep.builder(ConfigStep.Type.ASSERT_TEXT_EQUALS_ATTRIBUTE)
                    .arg("textSelector", textSel).arg("attrSelector", attrSel).arg("attr", attr).build();
            }
            case "close":
                return ConfigStep.builder(ConfigStep.Type.CLOSE).build();
            default:
                throw new IllegalArgumentException("Unknown config command: " + cmd + " in line: " + line);
        }
    }

    private static String get(List<String> tokens, int index) {
        return index < tokens.size() ? tokens.get(index) : null;
    }

    private static List<String> tokenize(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        char quoteChar = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuote) {
                if (c == quoteChar) {
                    inQuote = false;
                    out.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"' || c == '\'') {
                    inQuote = true;
                    quoteChar = c;
                } else if (Character.isWhitespace(c)) {
                    if (current.length() > 0) {
                        out.add(current.toString());
                        current.setLength(0);
                    }
                } else {
                    current.append(c);
                }
            }
        }
        if (current.length() > 0) out.add(current.toString());
        return out;
    }
}
