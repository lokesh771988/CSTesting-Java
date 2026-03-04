package com.cstesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Options for creating a CSTesting browser session.
 * Either start the Node server automatically or connect to an existing server.
 */
public final class CSTestingOptions {

    private final boolean headless;
    private final int port;
    private final String serverUrl;
    private final boolean useChromeDirect;
    private final List<String> args;
    private final String userDataDir;
    private final String chromePath;

    private CSTestingOptions(Builder b) {
        this.headless = b.headless;
        this.port = b.port;
        this.serverUrl = b.serverUrl;
        this.useChromeDirect = b.useChromeDirect;
        this.args = b.args != null ? Collections.unmodifiableList(new ArrayList<>(b.args)) : Collections.emptyList();
        this.userDataDir = b.userDataDir;
        this.chromePath = b.chromePath;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isHeadless() {
        return headless;
    }

    public int getPort() {
        return port;
    }

    /** If set, connect to this URL instead of starting the server (e.g. "ws://localhost:9274"). */
    public String getServerUrl() {
        return serverUrl;
    }

    /** If true, launch Chrome via CDP (no Node.js or CSTesting server required). */
    public boolean isUseChromeDirect() {
        return useChromeDirect;
    }

    /** Extra Chrome command-line arguments (e.g. "--disable-gpu"). Empty if not set. */
    public List<String> getArgs() {
        return args;
    }

    /** Custom Chrome user data directory; null = use temp dir. */
    public String getUserDataDir() {
        return userDataDir;
    }

    /** Path to Chrome/Chromium executable; null = auto-detect. */
    public String getChromePath() {
        return chromePath;
    }

    public static final class Builder {
        private boolean headless = true;
        private int port = 9274;
        private String serverUrl;
        private boolean useChromeDirect = true;
        private List<String> args;
        private String userDataDir;
        private String chromePath;

        public Builder headless(boolean headless) {
            this.headless = headless;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder serverUrl(String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        /** Use Chrome directly via CDP (no Node.js or CSTesting server). Requires Chrome browser installed. */
        public Builder useChromeDirect(boolean useChromeDirect) {
            this.useChromeDirect = useChromeDirect;
            return this;
        }

        /** Add a Chrome command-line argument (e.g. "--disable-gpu"). */
        public Builder addArg(String arg) {
            if (args == null) args = new ArrayList<>();
            args.add(arg);
            return this;
        }

        /** Set Chrome command-line arguments. Replaces any previously set. */
        public Builder args(List<String> args) {
            this.args = args != null ? new ArrayList<>(args) : null;
            return this;
        }

        /** Set Chrome user data directory (default: temp dir). */
        public Builder userDataDir(String userDataDir) {
            this.userDataDir = userDataDir;
            return this;
        }

        /** Set path to Chrome/Chromium executable (default: auto-detect). */
        public Builder chromePath(String chromePath) {
            this.chromePath = chromePath;
            return this;
        }

        public CSTestingOptions build() {
            return new CSTestingOptions(this);
        }
    }
}
