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

import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * @author haiyangl
 *
 */
class ForwardingStream<T> implements Stream<T> {
    private final Supplier<Stream<T>> supplier;
    private Stream<T> s;

    ForwardingStream(final Supplier<Stream<T>> supplier, final Stream<T> s) {
        this.supplier = supplier;
        this.s = s;
    }

    public static <T> Stream<T> of(final Supplier<Stream<T>> supplier) {
        Objects.requireNonNull(supplier);

        return new ForwardingStream<>(supplier, null);
    }

    public static <T> Stream<T> of(final Stream<T> s) {
        Objects.requireNonNull(s);

        return new ForwardingStream<>(null, s);
    }

    private Stream<T> stream() {
        if (s == null) {
            s = supplier.get();
        }

        return s;
    }

    @Override
    public Iterator<T> iterator() {
        return stream().iterator();
    }

    @Override
    public Spliterator<T> spliterator() {
        return stream().spliterator();
    }

    @Override
    public boolean isParallel() {
        return stream().isParallel();
    }

    @Override
    public Stream<T> sequential() {
        return stream().sequential();
    }

    @Override
    public Stream<T> parallel() {
        return stream().parallel();
    }

    @Override
    public Stream<T> unordered() {
        return stream().unordered();
    }

    @Override
    public Stream<T> onClose(Runnable closeHandler) {
        return stream().onClose(closeHandler);
    }

    @Override
    public void close() {
        stream().close();
    }

    @Override
    public Stream<T> filter(Predicate<? super T> predicate) {
        return stream().filter(predicate);
    }

    @Override
    public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        return stream().map(mapper);
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        return stream().mapToInt(mapper);
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        return stream().mapToLong(mapper);
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        return stream().mapToDouble(mapper);
    }

    @Override
    public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return stream().flatMap(mapper);
    }

    @Override
    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        return stream().flatMapToInt(mapper);
    }

    @Override
    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        return stream().flatMapToLong(mapper);
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        return stream().flatMapToDouble(mapper);
    }

    @Override
    public Stream<T> distinct() {
        return stream().distinct();
    }

    @Override
    public Stream<T> sorted() {
        return stream().sorted();
    }

    @Override
    public Stream<T> sorted(Comparator<? super T> comparator) {
        return stream().sorted(comparator);
    }

    @Override
    public Stream<T> peek(Consumer<? super T> action) {
        return stream().peek(action);
    }

    @Override
    public Stream<T> limit(long maxSize) {
        return stream().limit(maxSize);
    }

    @Override
    public Stream<T> skip(long n) {
        return stream().skip(n);
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        stream().forEach(action);
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        stream().forEachOrdered(action);
    }

    @Override
    public Object[] toArray() {
        return stream().toArray();
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        return stream().toArray(generator);
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        return stream().reduce(identity, accumulator);
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        return stream().reduce(accumulator);
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        return stream().reduce(identity, accumulator, combiner);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        return stream().collect(supplier, accumulator, combiner);
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        return stream().collect(collector);
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        return stream().min(comparator);
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        return stream().max(comparator);
    }

    @Override
    public long count() {
        return stream().count();
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        return stream().anyMatch(predicate);
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        return stream().allMatch(predicate);
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        return stream().noneMatch(predicate);
    }

    @Override
    public Optional<T> findFirst() {
        return stream().findFirst();
    }

    @Override
    public Optional<T> findAny() {
        return stream().findAny();
    }
}
