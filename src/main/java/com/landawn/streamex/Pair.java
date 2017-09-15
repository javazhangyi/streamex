/*
 * Copyright 2015, 2016 Tagir Valeev
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import com.landawn.streamex.function.TriPredicate;

/**
 * @author haiyangl
 *
 */
public final class Pair<L, R> implements Map.Entry<L, R> {
    private volatile L left;
    private volatile R right;

    public Pair() {
    }

    Pair(final L l, final R r) {
        this.left = l;
        this.right = r;
    }

    public static <L, R> Pair<L, R> of(final L l, final R r) {
        return new Pair<>(l, r);
    }

    public static <T> Pair<T, T> from(T[] a) {
        if (a == null || a.length == 0) {
            return new Pair<>(null, null);
        } else if (a.length == 1) {
            return new Pair<>(a[0], null);
        } else {
            return new Pair<>(a[0], a[1]);
        }
    }

    public static <T> Pair<T, T> from(Collection<? extends T> c) {
        if (c == null || c.size() == 0) {
            return new Pair<>(null, null);
        }

        @SuppressWarnings("unchecked")
        List<T> list = c instanceof List ? (List<T>) c : null;

        if (c.size() == 1) {
            if (list != null) {
                return new Pair<>(list.get(0), null);
            } else {
                return new Pair<>(c.iterator().next(), null);
            }
        } else {
            if (list != null) {
                return new Pair<>(list.get(0), list.get(1));
            } else {
                final Iterator<? extends T> iter = c.iterator();
                return new Pair<>(iter.next(), iter.next());
            }
        }
    }

    public L left() {
        return left;
    }

    public R right() {
        return right;
    }

    public L getLeft() {
        return left;
    }

    public Pair<L, R> setLeft(final L left) {
        this.left = left;

        return this;
    }

    public R getRight() {
        return right;
    }

    public Pair<L, R> setRight(final R right) {
        this.right = right;

        return this;
    }

    public Pair<L, R> set(final L left, final R right) {
        this.left = left;
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
    public boolean setLeftIf(final L newLeft, BiPredicate<? super Pair<L, R>, ? super L> predicate) {
        if (predicate.test(this, newLeft)) {
            this.left = newLeft;
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
    public boolean setRightIf(final R newRight, BiPredicate<? super Pair<L, R>, ? super R> predicate) {
        if (predicate.test(this, newRight)) {
            this.right = newRight;
            return true;
        }

        return false;
    }

    /**
     * Set to the specified <code>newLeft</code> and <code>newRight</code> and
     * returns <code>true</code> if <code>predicate</code> returns true.
     * Otherwise returns <code>false</code> without setting the left/right to
     * new values.
     * 
     * @param newLeft
     * @param newRight
     * @param predicate - the first parameter is current pair, the second
     *        parameter is the <code>newLeft</code>, the third parameter is the
     *        <code>newRight</code>.
     * @return
     */
    public boolean setIf(final L newLeft, final R newRight,
            TriPredicate<? super Pair<L, R>, ? super L, ? super R> predicate) {
        if (predicate.test(this, newLeft, newRight)) {
            this.left = newLeft;
            this.right = newRight;
            return true;
        }

        return false;
    }

    @Override
    public L getKey() {
        return left;
    }

    @Override
    public R getValue() {
        return right;
    }

    @Override
    public R setValue(R value) {
        R oldValue = this.right;
        this.right = value;

        return oldValue;
    }

    //    public R getAndSetValue(R newRight) {
    //        return getAndSetRight(newRight);
    //    }
    //
    //    public R setAndGetValue(R newRight) {
    //        return setAndGetRight(newRight);
    //    }
    //
    //    /**
    //     * 
    //     * @param newRight
    //     * @param predicate
    //     * @return
    //     * @see #setRightIf(Object, BiPredicate)
    //     */
    //    public boolean setValueIf(final R newRight, BiPredicate<? super Pair<L, R>, ? super R> predicate) {
    //        return setRightIf(newRight, predicate);
    //    }

    /**
     * 
     * @return a new instance of Pair&lt;R, L&gt;.
     */
    public Pair<R, L> reversed() {
        return new Pair<>(this.right, this.left);
    }

    public Pair<L, R> copy() {
        return new Pair<>(this.left, this.right);
    }

    public Object[] toArray() {
        return new Object[] { left, right };
    }

    @SuppressWarnings("unchecked")
    public <A> A[] toArray(A[] a) {
        if (a.length < 2) {
            a = Arrays.copyOf(a, 2);
        }

        a[0] = (A) left;
        a[1] = (A) right;

        return a;
    }

    @SuppressWarnings("unchecked")
    public void forEach(Consumer<?> comsumer) {
        final Consumer<Object> objComsumer = (Consumer<Object>) comsumer;

        objComsumer.accept(left);
        objComsumer.accept(right);
    }

    public StreamEx<Pair<L, R>> stream() {
        return StreamEx.of(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hashCode(left);
        result = prime * result + Objects.hashCode(right);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Pair) {
            final Pair<L, R> other = (Pair<L, R>) obj;

            return Objects.equals(left, other.left) && Objects.equals(right, other.right);
        }

        return false;
    }

    @Override
    public String toString() {
        return "[" + Objects.toString(left) + ", " + Objects.toString(right) + "]";
    }

    public static final class IntPair {
        public final int _1;
        public final int _2;

        private IntPair(int _1, int _2) {
            this._1 = _1;
            this._2 = _2;
        }

        public static IntPair of(int _1, int _2) {
            return new IntPair(_1, _2);
        }

        public int min() {
            return Math.min(_1, _2);
        }

        public int max() {
            return Math.max(_1, _2);
        }

        public int sum() {
            return _1 + _2;
        }

        public double average() {
            return sum() / 2;
        }

        public IntPair reversed() {
            return new IntPair(_2, _1);
        }

        public int[] toArray() {
            return new int[] { _1, _2 };
        }

        public void forEach(IntConsumer comsumer) {
            comsumer.accept(this._1);
            comsumer.accept(this._2);
        }

        public StreamEx<IntPair> stream() {
            return StreamEx.of(this);
        }

        @Override
        public int hashCode() {
            return 31 * _1 + this._2;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (!(obj instanceof IntPair)) {
                return false;
            } else {
                IntPair other = (IntPair) obj;
                return this._1 == other._1 && this._2 == other._2;
            }
        }

        @Override
        public String toString() {
            return "[" + this._1 + ", " + this._2 + "]";
        }
    }

    public static final class LongPair {
        public final long _1;
        public final long _2;

        private LongPair(long _1, long _2) {
            this._1 = _1;
            this._2 = _2;
        }

        public static LongPair of(long _1, long _2) {
            return new LongPair(_1, _2);
        }

        public long min() {
            return Math.min(_1, _2);
        }

        public long max() {
            return Math.max(_1, _2);
        }

        public long sum() {
            return _1 + _2;
        }

        public double average() {
            return sum() / 2;
        }

        public LongPair reversed() {
            return new LongPair(_2, _1);
        }

        public long[] toArray() {
            return new long[] { _1, _2 };
        }

        public void forEach(LongConsumer comsumer) {
            comsumer.accept(this._1);
            comsumer.accept(this._2);
        }

        public StreamEx<LongPair> stream() {
            return StreamEx.of(this);
        }

        @Override
        public int hashCode() {
            return (int) (31 * _1 + this._2);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (!(obj instanceof LongPair)) {
                return false;
            } else {
                LongPair other = (LongPair) obj;
                return this._1 == other._1 && this._2 == other._2;
            }
        }

        @Override
        public String toString() {
            return "[" + this._1 + ", " + this._2 + "]";
        }
    }

    public static final class DoublePair {
        public final double _1;
        public final double _2;

        private DoublePair(double _1, double _2) {
            this._1 = _1;
            this._2 = _2;
        }

        public static DoublePair of(double _1, double _2) {
            return new DoublePair(_1, _2);
        }

        public double min() {
            return Math.min(_1, _2);
        }

        public double max() {
            return Math.max(_1, _2);
        }

        public double sum() {
            return _1 + _2;
        }

        public double average() {
            return sum() / 2;
        }

        public DoublePair reversed() {
            return new DoublePair(_2, _1);
        }

        public double[] toArray() {
            return new double[] { _1, _2 };
        }

        public void forEach(DoubleConsumer comsumer) {
            comsumer.accept(this._1);
            comsumer.accept(this._2);
        }

        public StreamEx<DoublePair> stream() {
            return StreamEx.of(this);
        }

        @Override
        public int hashCode() {
            return (int) (31 * _1 + this._2);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (!(obj instanceof DoublePair)) {
                return false;
            } else {
                DoublePair other = (DoublePair) obj;
                return this._1 == other._1 && this._2 == other._2;
            }
        }

        @Override
        public String toString() {
            return "[" + this._1 + ", " + this._2 + "]";
        }
    }
}
