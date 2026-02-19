package com.cstesting.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a method to run before each test method. Runs after the browser is created and injected.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface BeforeMethod {
}
