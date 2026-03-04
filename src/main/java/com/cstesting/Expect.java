package com.cstesting;

import java.util.Objects;

/**
 * Fluent assertions for arbitrary values (non-locator). Use {@link #of(Object)} then chain
 * {@link #toBe(Object)}, {@link #toEqual(Object)}, {@link #toContain(CharSequence)}, or {@link #not()}.
 * <p>
 * Example:
 * <pre>
 * Expect.of(response.get("status")).toBe("ok");
 * Expect.of(list.size()).toBe(3);
 * Expect.of(text).toContain("hello");
 * Expect.of(value).not().toBe(null);
 * </pre>
 */
public final class Expect<T> {

    private final T value;
    private final boolean negated;

    private Expect(T value, boolean negated) {
        this.value = value;
        this.negated = negated;
    }

    public static <T> Expect<T> of(T value) {
        return new Expect<>(value, false);
    }

    /**
     * Negate the next assertion (e.g. expect().not().toBe(x)).
     */
    public Expect<T> not() {
        return new Expect<>(value, !negated);
    }

    /**
     * Assert value is equal to expected (uses Objects.equals). For primitives, box first: Expect.of((Integer) i).toBe(3).
     */
    public Expect<T> toBe(T expected) {
        boolean match = Objects.equals(value, expected);
        if (negated ? match : !match) {
            throw new AssertionError(negated
                ? "Expected value not to be " + expected + " but got " + value
                : "Expected " + value + " to be " + expected);
        }
        return this;
    }

    /**
     * Same as {@link #toBe(Object)} (alias for consistency with some test styles).
     */
    public Expect<T> toEqual(T expected) {
        return toBe(expected);
    }

    /**
     * Assert that the string representation of value contains the given sequence. Useful for strings or toString().
     */
    public Expect<T> toContain(CharSequence substring) {
        String str = value != null ? value.toString() : "";
        boolean match = substring != null && str.contains(substring);
        if (negated ? match : !match) {
            throw new AssertionError(negated
                ? "Expected value not to contain \"" + substring + "\" but it did: " + value
                : "Expected " + value + " to contain \"" + substring + "\"");
        }
        return this;
    }

    /**
     * Assert value is null (or if negated, that it is not null).
     */
    public Expect<T> toBeNull() {
        boolean match = value == null;
        if (negated ? match : !match) {
            throw new AssertionError(negated
                ? "Expected value not to be null but it was"
                : "Expected value to be null but got " + value);
        }
        return this;
    }
}
