package com.cstesting.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a method to run once after the entire run (after all tests and @AfterClass). Browser is already closed.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AfterSuite {
}
