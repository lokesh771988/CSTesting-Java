package com.cstesting.impl.driver;

import java.nio.file.Path;
import java.util.List;

/**
 * Provides access to the CSTesting server driver (like Playwright's Driver).
 * Can use a preinstalled/bundled driver (e.g. from cstesting.cli.dir) or the system npx.
 *
 * @see <a href="https://github.com/microsoft/playwright-java/blob/main/driver/src/main/java/com/microsoft/playwright/impl/driver/Driver.java">Playwright Driver</a>
 */
public abstract class Driver {

    private static final String CSTESTING_CLI_DIR = "cstesting.cli.dir";

    private static volatile Driver instance;

    /**
     * Returns the driver instance, using a preinstalled dir if set, otherwise the system driver.
     */
    public static synchronized Driver ensureDriver() {
        if (instance == null) {
            String dir = System.getProperty(CSTESTING_CLI_DIR);
            if (dir != null && !dir.isEmpty()) {
                instance = new PreinstalledDriver(Path.of(dir));
            } else {
                instance = new SystemDriver();
            }
        }
        return instance;
    }

    /**
     * Builds the process that runs the CSTesting server (node + script or npx).
     */
    public abstract ProcessBuilder createProcessBuilder(int port, boolean headless);

    /**
     * Root directory of the driver (node binary and package/). Null for system driver.
     */
    public abstract Path driverDir();

    /**
     * Driver that uses Node + CLI from a preinstalled/bundled directory (e.g. from a driver-bundle JAR).
     * Layout: driverDir/node (or node.exe), driverDir/package/cli.js (or server.js).
     */
    static final class PreinstalledDriver extends Driver {
        private final Path dir;
        private final String nodeExecutable;

        PreinstalledDriver(Path dir) {
            this.dir = dir.toAbsolutePath().normalize();
            this.nodeExecutable = System.getProperty("os.name", "").toLowerCase().contains("windows") ? "node.exe" : "node";
        }

        @Override
        public ProcessBuilder createProcessBuilder(int port, boolean headless) {
            Path nodePath = dir.resolve(nodeExecutable);
            Path cliPath = dir.resolve("package").resolve("cli.js");
            if (!cliPath.toFile().exists()) {
                cliPath = dir.resolve("package").resolve("server.js");
            }
            List<String> command = new java.util.ArrayList<>();
            command.add(nodePath.toString());
            command.add(cliPath.toString());
            command.add("server");
            command.add("--port=" + port);
            if (!headless) {
                command.add("--no-headless");
            }
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(dir.toFile());
            pb.environment().put("CSTESTING_DRIVER", "java");
            return pb;
        }

        @Override
        public Path driverDir() {
            return dir;
        }
    }

    /**
     * Driver that uses system npx (requires Node.js on PATH).
     */
    static final class SystemDriver extends Driver {
        @Override
        public ProcessBuilder createProcessBuilder(int port, boolean headless) {
            List<String> command = new java.util.ArrayList<>();
            command.add("npx");
            command.add("cstesting");
            command.add("server");
            command.add("--port=" + port);
            if (!headless) {
                command.add("--no-headless");
            }
            return new ProcessBuilder(command);
        }

        @Override
        public Path driverDir() {
            return null;
        }
    }
}
