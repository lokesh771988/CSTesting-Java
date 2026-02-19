package com.cstesting.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a method to run once after all test methods in the class have run. Browser is already closed.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AfterClass {
}
