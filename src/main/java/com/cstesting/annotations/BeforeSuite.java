package com.cstesting.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a method to run once before the entire run (before any @BeforeClass or test). No browser is injected yet.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface BeforeSuite {
}
