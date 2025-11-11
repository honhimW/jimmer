package org.babyfish.jimmer.lang;

import org.jetbrains.annotations.Nullable;

/**
 * Similar to `java.util.Optional`, but different.
 * <p>
 * The value inside the `Optional` is allowed to be null,
 * but the `Optional` itself cannot be null in principle.
 * Because of this, `Optional` itself being Null will cause Intellij to give a warning.
 * <p>
 * Ref is slightly different,
 * not only its internal value is allowed to be null,
 * the Ref itself can also be null, to represent unknown data.
 */

public class Ref<T> {

    @Nullable
    private final T value;

    private Ref(@Nullable T value) {
        this.value = value;
    }

    @Nullable
    public T getValue() {
        return value;
    }

    private static final Ref<?> EMPTY = new Ref<>(null);

    public static <T> Ref<T> of(@Nullable T value) {
        if (value == null) {
            return empty();
        } else {
            return new Ref<>(value);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Ref<T> empty() {
        return (Ref<T>) EMPTY;
    }

}
