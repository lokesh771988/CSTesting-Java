package com.cstesting.runner;

import com.cstesting.CSTesting;
import com.cstesting.CSTestingBrowser;
import com.cstesting.CSTestingOptions;
import com.cstesting.annotations.AfterClass;
import com.cstesting.annotations.AfterMethod;
import com.cstesting.annotations.AfterSuite;
import com.cstesting.annotations.BeforeClass;
import com.cstesting.annotations.BeforeMethod;
import com.cstesting.annotations.BeforeSuite;
import com.cstesting.annotations.CSTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Runs test classes that use CSTesting annotations ({@link CSTest}, {@link BeforeSuite}, {@link AfterSuite},
 * {@link BeforeClass}, {@link AfterClass}, {@link BeforeMethod}, {@link AfterMethod}). Similar to TestNG lifecycle: create a browser per test method,
 * inject it into the test instance, run before/after and test methods.
 * <p>
 * Usage from code:
 * <pre>
 * CSTestingRunner.run(MyTestClass.class);
 * CSTestingRunner.run(MyTestClass.class, CSTestingOptions.builder().headless(false).build());
 * </pre>
 * Usage from command line (no main() needed in test class):
 * <pre>
 * mvn exec:java -Dexec.mainClass="com.cstesting.runner.CSTestingRunner" -Dexec.args="com.example.MyTest"
 * </pre>
 */
public final class CSTestingRunner {

    private CSTestingRunner() {}

    /**
     * Entry point to run a test class by name. Pass the fully qualified class name as the first argument.
     * Example: {@code java -cp ... com.cstesting.runner.CSTestingRunner com.cstesting.example.AnnotationTestExample}
     */
    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            System.err.println("Usage: CSTestingRunner <fully.qualified.TestClass>");
            System.err.println("Example: mvn exec:java -Dexec.mainClass=\"com.cstesting.runner.CSTestingRunner\" -Dexec.args=\"com.cstesting.example.AnnotationTestExample\"");
            System.exit(1);
        }
        String className = args[0].trim();
        try {
            Class<?> testClass = Class.forName(className);
            run(testClass);
        } catch (ClassNotFoundException e) {
            System.err.println("Test class not found: " + className);
            e.printStackTrace();
            System.exit(1);
        }
    }

    /** Run all @CSTest methods in the given test class with default (headless) options. */
    public static void run(Class<?> testClass) {
        run(testClass, RunOptions.builder().build());
    }

    /** Run all @CSTest methods in the given test class with the provided browser options. */
    public static void run(Class<?> testClass, CSTestingOptions options) {
        run(testClass, RunOptions.builder().browserOptions(options).build());
    }

    /** Run @CSTest methods in the given test class with the provided run options (tags filter, browser options). */
    public static void run(Class<?> testClass, RunOptions runOptions) {
        Object instance;
        try {
            instance = testClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not instantiate test class " + testClass.getName(), e);
        }

        CSTestingOptions effectiveOptions = runOptions != null && runOptions.getBrowserOptions() != null
            ? runOptions.getBrowserOptions()
            : null;
        if (effectiveOptions == null && instance instanceof CSTestingTestBase) {
            effectiveOptions = ((CSTestingTestBase) instance).getBrowserOptions();
        }
        if (effectiveOptions == null) {
            effectiveOptions = CSTestingOptions.builder().headless(true).build();
        }

        List<Method> beforeSuite = findMethods(testClass, BeforeSuite.class);
        List<Method> afterSuite = findMethods(testClass, AfterSuite.class);
        List<Method> beforeClass = findMethods(testClass, BeforeClass.class);
        List<Method> afterClass = findMethods(testClass, AfterClass.class);
        List<Method> beforeMethod = findMethods(testClass, BeforeMethod.class);
        List<Method> afterMethod = findMethods(testClass, AfterMethod.class);
        List<Method> tests = findMethods(testClass, CSTest.class);
        if (runOptions != null && runOptions.getTags() != null && !runOptions.getTags().isEmpty()) {
            Set<String> filterTags = runOptions.getTags();
            tests = filterTestsByTags(tests, filterTags);
        }

        try {
            invokeAll(instance, beforeSuite);
        } catch (Exception e) {
            throw new RuntimeException("@BeforeSuite failed", e);
        }
        try {
            invokeAll(instance, beforeClass);
        } catch (Exception e) {
            throw new RuntimeException("@BeforeClass failed", e);
        }

        int passed = 0;
        int failed = 0;
        for (Method testMethod : tests) {
            CSTestingBrowser browser = null;
            try {
                if (instance instanceof CSTestingTestBase) ((CSTestingTestBase) instance).clearSteps();
                browser = CSTesting.createBrowser(effectiveOptions);
                injectBrowser(instance, browser);
                invokeAll(instance, beforeMethod);
                testMethod.invoke(instance);
                passed++;
                System.out.println("[PASS] " + testClass.getSimpleName() + "." + testMethod.getName());
            } catch (Throwable t) {
                failed++;
                Throwable cause = t.getCause() != null ? t.getCause() : t;
                System.err.println("[FAIL] " + testClass.getSimpleName() + "." + testMethod.getName() + " - " + cause.getMessage());
                cause.printStackTrace();
            } finally {
                try {
                    invokeAll(instance, afterMethod);
                } catch (Throwable t) {
                    if (t.getCause() != null) t.getCause().printStackTrace();
                    else t.printStackTrace();
                }
                if (browser != null) {
                    try {
                        browser.close();
                    } catch (Exception ignored) {}
                }
            }
        }

        try {
            invokeAll(instance, afterClass);
        } catch (Exception e) {
            throw new RuntimeException("@AfterClass failed", e);
        }
        try {
            invokeAll(instance, afterSuite);
        } catch (Exception e) {
            throw new RuntimeException("@AfterSuite failed", e);
        }

        System.out.println("Tests run: " + tests.size() + ", Passed: " + passed + ", Failed: " + failed);
        if (failed > 0) {
            throw new AssertionError(failed + " test(s) failed");
        }
    }

    private static List<Method> filterTestsByTags(List<Method> tests, Set<String> filterTags) {
        List<Method> out = new ArrayList<>();
        for (Method m : tests) {
            CSTest ann = m.getAnnotation(CSTest.class);
            if (ann == null) continue;
            String[] tags = ann.tags();
            if (tags == null || tags.length == 0) continue;
            for (String t : tags) {
                if (t != null && filterTags.contains(t)) {
                    out.add(m);
                    break;
                }
            }
        }
        return out;
    }

    private static List<Method> findMethods(Class<?> clazz, Class<? extends java.lang.annotation.Annotation> annotation) {
        List<Method> list = new ArrayList<>();
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getParameterCount() == 0 && m.isAnnotationPresent(annotation)) {
                m.setAccessible(true);
                list.add(m);
            }
        }
        return list;
    }

    private static void invokeAll(Object instance, List<Method> methods) throws Exception {
        for (Method m : methods) {
            m.invoke(instance);
        }
    }

    private static void injectBrowser(Object instance, CSTestingBrowser browser) {
        if (instance instanceof CSTestingTestBase) {
            ((CSTestingTestBase) instance).setBrowser(browser);
            return;
        }
        try {
            Method setter = instance.getClass().getMethod("setBrowser", CSTestingBrowser.class);
            setter.invoke(instance, browser);
        } catch (NoSuchMethodException e) {
            try {
                java.lang.reflect.Field f = instance.getClass().getDeclaredField("browser");
                f.setAccessible(true);
                f.set(instance, browser);
            } catch (Exception e2) {
                throw new RuntimeException("Test class must extend CSTestingTestBase or have setBrowser(CSTestingBrowser) or a field 'browser'", e2);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not inject browser", e);
        }
    }
}
