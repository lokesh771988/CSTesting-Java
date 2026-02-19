package com.cstesting.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a method to run once before any test method in the class. No browser is injected yet.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface BeforeClass {
}
