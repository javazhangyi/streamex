/*
 * Copyright 2017 Haiyang Li
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.streamex;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * @author Haiyang Li
 *
 */
public class Holder<T> {
    private volatile T value;

    public Holder() {
    }

    Holder(T value) {
        this.value = value;
    }

    public static <T> Holder<T> of(T value) {
        return new Holder<>(value);
    }

    public T value() {
        return value;
    }

    public T getValue() {
        return value;
    }

    public Holder<T> setValue(final T value) {
        this.value = value;

        return this;
    }

    public T getAndSet(final T value) {
        final T result = this.value;
        this.value = value;
        return result;
    }

    public T setAndGet(final T value) {
        this.value = value;
        return this.value;
    }

    public final T getAndUpdate(UnaryOperator<T> updateFunction) {
        final T res = value;
        this.value = updateFunction.apply(value);
        return res;
    }

    public final T updateAndGet(UnaryOperator<T> updateFunction) {
        this.value = updateFunction.apply(value);
        return value;
    }

    /**
     * Set to the specified <code>newValue</code> and returns <code>true</code>
     * if <code>predicate</code> returns true. Otherwise returns
     * <code>false</code> without setting the value to new value.
     * 
     * @param newValue
     * @param predicate - test the current value.
     * @return
     */
    public boolean setIf(final T newValue, Predicate<? super T> predicate) {
        if (predicate.test(value)) {
            this.value = newValue;
            return true;
        }

        return false;
    }

    /**
     * Set to the specified <code>newValue</code> and returns <code>true</code>
     * if <code>predicate</code> returns true. Otherwise returns
     * <code>false</code> without setting the value to new value.
     * 
     * @param newValue
     * @param predicate - the first parameter is current value, the second
     *        parameter is the <code>newValue</code>
     * @return
     */
    public boolean setIf(final T newValue, BiPredicate<? super T, ? super T> predicate) {
        if (predicate.test(value, newValue)) {
            this.value = newValue;
            return true;
        }

        return false;
    }

    public boolean isNotNull() {
        return value != null;
    }

    public void accept(final Consumer<? super T> action) {
        action.accept(value);
    }

    public void acceptIfNotNull(final Consumer<? super T> action) {
        if (isNotNull()) {
            action.accept(value);
        }
    }

    public <U> U apply(final Function<? super T, U> action) {
        return action.apply(value);
    }

    /**
     * Execute the specified action if value is not null, otherwise return an
     * empty <code>Optional</code> directly.
     * 
     * @param action
     * @return
     */
    public <U> Optional<U> applyIfNotNull(final Function<? super T, U> action) {
        return isNotNull() ? Optional.of(action.apply(value)) : Optional.<U> empty();
    }

    public StreamEx<T> stream() {
        return StreamEx.of(value);
    }

    /**
     * 
     * @return an empty Stream if the value is null.
     */
    public StreamEx<T> streamIfNotNull() {
        return isNotNull() ? StreamEx.of(value) : StreamEx.<T> empty();
    }

    /**
     * Return the value is not null, otherwise return {@code other}.
     *
     * @param other the value to be returned if not present or null, may be null
     * @return the value, if not present or null, otherwise {@code other}
     */
    public T orIfNull(T other) {
        return isNotNull() ? value : other;
    }

    /**
     * Return the value is not null, otherwise invoke {@code other} and return
     * the result of that invocatioObjects.
     *
     * @param other a {@code Supplier} whose result is returned if not present
     *        or null
     * @return the value if not present or null otherwise the result of
     *         {@code other.get()}
     * @throws NullPointerException if value is not present and {@code other} is
     *         null
     */
    public T orGetIfNull(Supplier<? extends T> other) {
        return isNotNull() ? value : other.get();
    }

    /**
     * Return the value is not null, otherwise throw an exception to be created
     * by the provided supplier.
     *
     * @apiNote A method reference to the exception constructor with an empty
     *          argument list can be used as the supplier. For example,
     *          {@code IllegalStateException::new}
     *
     * @param <X> Type of the exception to be thrown
     * @param exceptionSupplier The supplier which will return the exception to
     *        be thrown
     * @return the present value
     * @throws X if not present or null
     * @throws NullPointerException if not present or null and
     *         {@code exceptionSupplier} is null
     */
    public <X extends Throwable> T orThrowIfNull(Supplier<? extends X> exceptionSupplier) throws X {
        if (isNotNull()) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    @Override
    public int hashCode() {
        return (value == null) ? 0 : value.hashCode();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof Holder && Objects.equals(((Holder<T>) obj).value, value));
    }

    @Override
    public String toString() {
        return Objects.toString(value);
    }

    public static final class V<T> extends Holder<T> {

        private V() {
            // singleton.
        }
    }
}
