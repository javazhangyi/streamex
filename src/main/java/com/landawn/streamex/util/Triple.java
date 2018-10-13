/*
 * Copyright (c) 2015, Haiyang Li.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.streamex.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.*;

import com.landawn.streamex.StreamEx;
import com.landawn.streamex.util.Tuple.Tuple3;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 *
 * @param <L>
 * @param <M>
 * @param <R>
 */
public final class Triple<L, M, R> {
    public L left;
    public M middle;
    public R right;

    public Triple() {
    }

    Triple(final L l, final M m, final R r) {
        this.left = l;
        this.middle = m;
        this.right = r;
    }

    public static <L, M, R> Triple<L, M, R> of(final L l, final M m, final R r) {
        return new Triple<>(l, m, r);
    }

    public L left() {
        return left;
    } 

    public M middle() {
        return middle;
    }

    public R right() {
        return right;
    }

    public L getLeft() {
        return left;
    }

    public Triple<L, M, R> setLeft(final L left) {
        this.left = left;

        return this;
    }

    public M getMiddle() {
        return middle;
    }

    public Triple<L, M, R> setMiddle(final M middle) {
        this.middle = middle;

        return this;
    }

    public R getRight() {
        return right;
    }

    public Triple<L, M, R> setRight(final R right) {
        this.right = right;

        return this;
    }

    public Triple<L, M, R> set(final L left, final M middle, final R right) {
        this.left = left;
        this.middle = middle;
        this.right = right;

        return this;
    }

    public L getAndSetLeft(L newLeft) {
        final L res = left;
        left = newLeft;
        return res;
    }

    public L setAndGetLeft(L newLeft) {
        left = newLeft;
        return left;
    }

    public M getAndSetMiddle(M newMiddle) {
        final M res = middle;
        middle = newMiddle;
        return res;
    }

    public M setAndGetMiddle(M newMiddle) {
        middle = newMiddle;
        return middle;
    }

    public R getAndSetRight(R newRight) {
        final R res = newRight;
        right = newRight;
        return res;
    }

    public R setAndGetRight(R newRight) {
        right = newRight;
        return right;
    }

    /**
     * Set to the specified <code>newLeft</code> and returns <code>true</code>
     * if <code>predicate</code> returns true. Otherwise returns
     * <code>false</code> without setting the value to new value.
     * 
     * @param newLeft
     * @param predicate - the first parameter is current pair, the second
     *        parameter is the <code>newLeft</code>
     * @return
     */
    public boolean setLeftIf(final L newLeft, BiPredicate<? super Triple<L, M, R>, ? super L> predicate) {
        if (predicate.test(this, newLeft)) {
            this.left = newLeft;
            return true;
        }

        return false;
    }

    /**
     * Set to the specified <code>newMiddle</code> and returns <code>true</code>
     * if <code>predicate</code> returns true. Otherwise returns
     * <code>false</code> without setting the value to new value.
     * 
     * @param newMiddle
     * @param predicate - the first parameter is current pair, the second
     *        parameter is the <code>newMiddle</code>
     * @return
     */
    public boolean setMiddleIf(final M newMiddle, BiPredicate<? super Triple<L, M, R>, ? super M> predicate) {
        if (predicate.test(this, newMiddle)) {
            this.middle = newMiddle;
            return true;
        }

        return false;
    }

    /**
     * Set to the specified <code>newRight</code> and returns <code>true</code>
     * if <code>predicate</code> returns true. Otherwise returns
     * <code>false</code> without setting the value to new value.
     * 
     * @param newRight
     * @param predicate - the first parameter is current pair, the second
     *        parameter is the <code>newRight</code>
     * @return
     */
    public boolean setRightIf(final R newRight, BiPredicate<? super Triple<L, M, R>, ? super R> predicate) {
        if (predicate.test(this, newRight)) {
            this.right = newRight;
            return true;
        }

        return false;
    }

    //    /**
    //     * Swap the left and right value. they must be same type.
    //     */
    //    public void reverse() {
    //        Object tmp = left;
    //        this.left = (L) right;
    //        this.right = (R) tmp;
    //    }

    /**
     * 
     * @return a new instance of Triple&lt;R, M, L&gt;.
     */
    public Triple<R, M, L> reversed() {
        return new Triple<>(this.right, this.middle, this.left);
    }

    public Triple<L, M, R> copy() {
        return new Triple<>(this.left, this.middle, this.right);
    }

    public Object[] toArray() {
        return new Object[] { left, middle, right };
    }

    public <A> A[] toArray(A[] a) {
        if (a.length < 3) {
            a = Arrays.copyOf(a, 3);
        }

        a[0] = (A) left;
        a[1] = (A) middle;
        a[2] = (A) right;

        return a;
    }

    public void forEach(Consumer<?> comsumer) {
        final Consumer<Object> objComsumer = (Consumer<Object>) comsumer;

        objComsumer.accept(left);
        objComsumer.accept(middle);
        objComsumer.accept(right);
    }

    public void accept(final Consumer<? super Triple<L, M, R>> action) {
        action.accept(this);
    }

    public <U> U map(final Function<? super Triple<L, M, R>, U> mapper) {
        return mapper.apply(this);
    }

    public Optional<Triple<L, M, R>> filter(final Predicate<? super Triple<L, M, R>> predicate) {
        return predicate.test(this) ? Optional.of(this) : Optional.<Triple<L, M, R>> empty();
    }

    public StreamEx<Triple<L, M, R>> stream() {
        return StreamEx.of(this);
    }

    public Tuple3<L, M, R> toTuple() {
        return Tuple.of(left, middle, right);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hashCode(left);
        result = prime * result + Objects.hashCode(middle);
        result = prime * result + Objects.hashCode(right);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Triple) {
            final Triple<L, M, R> other = (Triple<L, M, R>) obj;

            return Objects.equals(left, other.left) && Objects.equals(middle, other.middle) && Objects.equals(right,
                other.right);
        }

        return false;
    }

    @Override
    public String toString() {
        return "[" + Objects.toString(left) + ", " + Objects.toString(middle) + ", " + Objects.toString(right) + "]";
    }
}
