package com.cstesting.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a method as a CSTesting test. The method will be run by {@link com.cstesting.runner.CSTestingRunner}.
 * The test class should extend {@link com.cstesting.runner.CSTestingTestBase} (or have a field
 * {@code CSTestingBrowser browser} that the runner will inject).
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CSTest {
    /** Optional description for the test (e.g. for reports). */
    String description() default "";

    /** Optional tags for filtering (e.g. run only tests with tag "smoke"). */
    String[] tags() default {};
}
