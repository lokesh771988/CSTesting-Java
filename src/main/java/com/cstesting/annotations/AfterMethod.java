package com.cstesting.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a method to run after each test method. Runs before the browser is closed.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AfterMethod {
}
