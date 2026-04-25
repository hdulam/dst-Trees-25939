package com.sebdeveloper6952;

/**
 * Minimal ordered map contract for the 2-3 tree exercise.
 * <p>
 * Keys must not be {@code null}. Values may be {@code null}; therefore
 * {@link #containsKey(Object)} — not {@link #get(Object)} — is the
 * authoritative membership test.
 */
public interface Map<K, V> {

    /**
     * Associates {@code value} with {@code key}.
     *
     * @return the previous value bound to {@code key}, or {@code null} if none.
     */
    V put(K key, V value);

    /**
     * @return the value bound to {@code key}, or {@code null} if absent
     *         (or if the key is bound to {@code null}).
     */
    V get(K key);

    /**
     * Removes the binding for {@code key}, if any.
     *
     * @return the value that was bound to {@code key}, or {@code null} if absent.
     */
    V remove(K key);

    /** @return {@code true} iff {@code key} has a binding in this map. */
    boolean containsKey(K key);

    /** @return the number of bindings in this map. */
    int size();

    /** @return {@code true} iff this map has no bindings. */
    boolean isEmpty();
}
