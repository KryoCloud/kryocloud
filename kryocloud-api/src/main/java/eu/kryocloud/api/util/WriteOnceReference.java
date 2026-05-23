package eu.kryocloud.api.util;

import java.util.concurrent.atomic.AtomicReference;

public final class WriteOnceReference<T> {

    private final AtomicReference<T> reference = new AtomicReference<>();

    public boolean set(T value) {
        validateValue(value);
        return reference.compareAndSet(null, value);
    }

    public void setOrThrow(T value) {
        validateValue(value);

        if (!reference.compareAndSet(null, value)) {
            throw new IllegalStateException("Reference already initialized");
        }
    }

    public T get() {
        T value = reference.get();

        if (value == null) {
            throw new IllegalStateException("Reference not initialized");
        }

        return value;
    }

    public T getOrNull() {
        return reference.get();
    }

    public boolean isSet() {
        return reference.get() != null;
    }

    private void validateValue(T value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
    }
}
