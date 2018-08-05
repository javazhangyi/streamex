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

import java.util.AbstractMap.SimpleImmutableEntry;

import static com.landawn.streamex.StreamExInternals.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.landawn.streamex.util.Comparators;
import com.landawn.streamex.util.Fn;
import com.landawn.streamex.util.Fn.Suppliers;
import com.landawn.streamex.util.Keyed;

/**
 * A {@link Stream} of {@link Entry} objects which provides additional specific
 * functionality.
 * 
 * <p>
 * While {@code EntryStream} implements {@code Iterable}, it is not a
 * general-purpose {@code Iterable} as it supports only a single
 * {@code Iterator}; invoking the {@link #iterator iterator} method to obtain a
 * second or subsequent iterator throws {@code IllegalStateException}.
 * 
 * @author Tagir Valeev
 *
 * @param <K> the type of {@code Entry} keys
 * @param <V> the type of {@code Entry} values
 */
public class EntryStream<K, V> extends AbstractStreamEx<Entry<K, V>, EntryStream<K, V>> {
    EntryStream(Stream<? extends Entry<K, V>> stream, StreamContext context) {
        super(stream, context);
    }

    EntryStream(Spliterator<? extends Entry<K, V>> spliterator, StreamContext context) {
        super(spliterator, context);
    }

    @Override
    EntryStream<K, V> supply(Stream<Map.Entry<K, V>> stream) {
        return new EntryStream<>(stream, context);
    }

    @Override
    EntryStream<K, V> supply(Spliterator<Entry<K, V>> spliterator) {
        return new EntryStream<>(spliterator, context);
    }

    static <K, V> Consumer<? super Entry<K, V>> toConsumer(BiConsumer<? super K, ? super V> action) {
        return entry -> action.accept(entry.getKey(), entry.getValue());
    }

    <M extends Map<K, V>> Consumer<? super Entry<K, V>> toMapConsumer(M map) {
        return entry -> {
            V oldVal = map.putIfAbsent(entry.getKey(), entry.getValue());
            if (oldVal != null) {
                throw new IllegalStateException("Duplicate entry for key '" + entry.getKey()
                    + "' (attempt to merge values '" + oldVal + "' and '" + entry.getValue() + "')");
            }
        };
    }

    BiPredicate<? super Entry<K, V>, ? super Entry<K, V>> equalKeys() {
        return (e1, e2) -> Objects.equals(e1.getKey(), e2.getKey());
    }

    static <K, V> Stream<Entry<K, V>> withValue(Stream<? extends K> s, V value) {
        return s == null ? null : s.map(key -> new SimpleImmutableEntry<>(key, value));
    }

    static <K, V> Stream<Entry<K, V>> withKey(K key, Stream<? extends V> s) {
        return s == null ? null : s.map(value -> new SimpleImmutableEntry<>(key, value));
    }

    static <K, V, R> Function<? super Entry<K, V>, ? extends R> toFunction(
            BiFunction<? super K, ? super V, ? extends R> mapper) {
        return entry -> mapper.apply(entry.getKey(), entry.getValue());
    }

    /**
     * Returns an {@code EntryStream} consisting of the entries whose keys are
     * results of replacing source keys with the contents of a mapped stream
     * produced by applying the provided mapping function to each source key and
     * values are left intact. Each mapped stream is
     * {@link java.util.stream.BaseStream#close() closed} after its contents
     * have been placed into this stream. (If a mapped stream is {@code null} an
     * empty stream is used, instead.)
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param <KK> The type of new keys
     * @param mapper a
     *        <a href="package-summary.html#NonInterference">non-interfering
     *        </a>, <a href="package-summary.html#Statelessness">stateless</a>
     *        function to apply to each key which produces a stream of new keys
     * @return the new stream
     */
    public <KK> EntryStream<KK, V> flatMapKeys(Function<? super K, ? extends Stream<? extends KK>> mapper) {
        return new EntryStream<>(stream().flatMap(e -> withValue(mapper.apply(e.getKey()), e.getValue())), context);
    }

    /**
     * Returns an {@code EntryStream} consisting of the entries whose keys are
     * results of replacing source keys with the contents of a mapped stream
     * produced by applying the provided mapping function and values are left
     * intact. Each mapped stream is {@link java.util.stream.BaseStream#close()
     * closed} after its contents have been placed into this stream. (If a
     * mapped stream is {@code null} an empty stream is used, instead.)
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param <KK> The type of new keys
     * @param mapper a
     *        <a href="package-summary.html#NonInterference">non-interfering
     *        </a>, <a href="package-summary.html#Statelessness">stateless</a>
     *        function to apply to each key and value which produces a stream of
     *        new keys
     * @return the new stream
     * @since 0.5.2
     */
    public <KK> EntryStream<KK, V> flatMapKeys(
            BiFunction<? super K, ? super V, ? extends Stream<? extends KK>> mapper) {
        return new EntryStream<>(stream().flatMap(e -> withValue(mapper.apply(e.getKey(), e.getValue()), e.getValue())),
                context);
    }

    public <KK> EntryStream<KK, V> flattMapKeys(Function<? super K, ? extends Collection<? extends KK>> mapper) {
        return new EntryStream<>(stream().flatMap(e -> withValue(StreamEx.of(mapper.apply(e.getKey())), e.getValue())),
                context);
    }

    public <KK> EntryStream<KK, V> flattMapKeys(
            BiFunction<? super K, ? super V, ? extends Collection<? extends KK>> mapper) {
        return new EntryStream<>(stream().flatMap(e -> withValue(StreamEx.of(mapper.apply(e.getKey(), e.getValue())), e
                .getValue())), context);
    }

    /**
     * Returns an {@code EntryStream} consisting of the entries whose values are
     * results of replacing source values with the contents of a mapped stream
     * produced by applying the provided mapping function to each source value
     * and keys are left intact. Each mapped stream is
     * {@link java.util.stream.BaseStream#close() closed} after its contents
     * have been placed into this stream. (If a mapped stream is {@code null} an
     * empty stream is used, instead.)
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param <VV> The type of new values
     * @param mapper a
     *        <a href="package-summary.html#NonInterference">non-interfering
     *        </a>, <a href="package-summary.html#Statelessness">stateless</a>
     *        function to apply to each value which produces a stream of new
     *        values
     * @return the new stream
     */
    public <VV> EntryStream<K, VV> flatMapValues(Function<? super V, ? extends Stream<? extends VV>> mapper) {
        return new EntryStream<>(stream().flatMap(e -> withKey(e.getKey(), mapper.apply(e.getValue()))), context);
    }

    /**
     * Returns an {@code EntryStream} consisting of the entries whose values are
     * results of replacing source values with the contents of a mapped stream
     * produced by applying the provided mapping function and keys are left
     * intact. Each mapped stream is {@link java.util.stream.BaseStream#close()
     * closed} after its contents have been placed into this stream. (If a
     * mapped stream is {@code null} an empty stream is used, instead.)
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param <VV> The type of new values
     * @param mapper a
     *        <a href="package-summary.html#NonInterference">non-interfering
     *        </a>, <a href="package-summary.html#Statelessness">stateless</a>
     *        function to apply to each key and value which produces a stream of
     *        new values
     * @return the new stream
     * @since 0.5.2
     */
    public <VV> EntryStream<K, VV> flatMapValues(
            BiFunction<? super K, ? super V, ? extends Stream<? extends VV>> mapper) {
        return new EntryStream<>(stream().flatMap(e -> withKey(e.getKey(), mapper.apply(e.getKey(), e.getValue()))),
                context);
    }

    public <VV> EntryStream<K, VV> flattMapValues(Function<? super V, ? extends Collection<? extends VV>> mapper) {
        return new EntryStream<>(stream().flatMap(e -> withKey(e.getKey(), StreamEx.of(mapper.apply(e.getValue())))),
                context);
    }

    public <VV> EntryStream<K, VV> flattMapValues(
            BiFunction<? super K, ? super V, ? extends Collection<? extends VV>> mapper) {
        return new EntryStream<>(stream().flatMap(e -> withKey(e.getKey(), StreamEx.of(mapper.apply(e.getKey(), e
                .getValue())))), context);
    }

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying the
     * provided mapping function to each key-value pair. Each mapped stream is
     * closed after its contents have been placed into this stream. (If a mapped
     * stream is {@code null} an empty stream is used, instead.)
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <R> The element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to each
     *        key-value pair which produces a stream of new values
     * @return the new stream
     * @since 0.3.0
     */
    public <R> StreamEx<R> flatMap(final BiFunction<? super K, ? super V, ? extends Stream<? extends R>> mapper) {
        return this.<R> flatMap(toFunction(mapper));
    }

    public <R> StreamEx<R> flattMap(final BiFunction<? super K, ? super V, ? extends Collection<? extends R>> mapper) {
        return this.<R> flattMap(toFunction(mapper));
    }

    /**
     * Returns a new {@code EntryStream} which is a concatenation of this stream
     * and the supplied key-value pair.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     * 
     * @param key the key of the new {@code Entry} to append to this stream
     * @param value the value of the new {@code Entry} to append to this stream
     * @return the new stream
     */
    public EntryStream<K, V> append(K key, V value) {
        return append(new SimpleImmutableEntry<>(key, value));
    }

    /**
     * Returns a new {@code EntryStream} which is a concatenation of this stream
     * and two supplied key-value pairs.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     * 
     * @param k1 the key of the first {@code Entry} to append to this stream
     * @param v1 the value of the first {@code Entry} to append to this stream
     * @param k2 the key of the second {@code Entry} to append to this stream
     * @param v2 the value of the second {@code Entry} to append to this stream
     * @return the new stream
     * @since 0.2.3
     */
    public EntryStream<K, V> append(K k1, V v1, K k2, V v2) {
        @SuppressWarnings("unchecked")
        final Map.Entry<K, V>[] entries = new Map.Entry[] { new SimpleImmutableEntry<>(k1, v1),
                new SimpleImmutableEntry<>(k2, v2) };
        return append(entries);
    }

    /**
     * Returns a new {@code EntryStream} which is a concatenation of this stream
     * and three supplied key-value pairs.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     * 
     * @param k1 the key of the first {@code Entry} to append to this stream
     * @param v1 the value of the first {@code Entry} to append to this stream
     * @param k2 the key of the second {@code Entry} to append to this stream
     * @param v2 the value of the second {@code Entry} to append to this stream
     * @param k3 the key of the third {@code Entry} to append to this stream
     * @param v3 the value of the third {@code Entry} to append to this stream
     * @return the new stream
     * @since 0.2.3
     */
    public EntryStream<K, V> append(K k1, V v1, K k2, V v2, K k3, V v3) {
        @SuppressWarnings("unchecked")
        final Map.Entry<K, V>[] entries = new Map.Entry[] { new SimpleImmutableEntry<>(k1, v1),
                new SimpleImmutableEntry<>(k2, v2), new SimpleImmutableEntry<>(k3, v3) };
        return append(entries);
    }

    @Override
    public EntryStream<K, V> append(Entry<K, V> entry) {
        return appendSpliterator(null, new ConstSpliterator.OfRef<>(entry, 1, true));
    }

    @Override
    public EntryStream<K, V> append(Entry<K, V>... entries) {
        if (entries == null || entries.length == 0) {
            return this;
        }

        return appendSpliterator(null, Spliterators.spliterator(entries, Spliterator.ORDERED));
    }

    /**
     * Returns a new {@code EntryStream} which is a concatenation of this stream
     * and the stream created from the supplied map entries.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a>.
     * 
     * <p>
     * May return this if the supplied map is empty and non-concurrent.
     * 
     * @param map the map to prepend to the stream
     * @return the new stream
     * @since 0.2.1
     */
    public EntryStream<K, V> append(Map<K, V> map) {
        return appendSpliterator(null, map.entrySet().spliterator());
    }

    /**
     * Returns a new {@code EntryStream} which is a concatenation of the
     * supplied key-value pair and this stream.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a> with <a href="package-summary.html#TSO">tail-stream
     * optimization</a>.
     * 
     * @param key the key of the new {@code Entry} to prepend to this stream
     * @param value the value of the new {@code Entry} to prepend to this stream
     * @return the new stream
     */
    public EntryStream<K, V> prepend(K key, V value) {
        return prepend(new SimpleImmutableEntry<>(key, value));
    }

    /**
     * Returns a new {@code EntryStream} which is a concatenation of two
     * supplied key-value pairs and this stream.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a> with <a href="package-summary.html#TSO">tail-stream
     * optimization</a>.
     * 
     * @param k1 the key of the first {@code Entry} to prepend to this stream
     * @param v1 the value of the first {@code Entry} to prepend to this stream
     * @param k2 the key of the second {@code Entry} to prepend to this stream
     * @param v2 the value of the second {@code Entry} to prepend to this stream
     * @return the new stream
     * @since 0.2.3
     */
    public EntryStream<K, V> prepend(K k1, V v1, K k2, V v2) {
        @SuppressWarnings("unchecked")
        final Map.Entry<K, V>[] entries = new Map.Entry[] { new SimpleImmutableEntry<>(k1, v1),
                new SimpleImmutableEntry<>(k2, v2) };
        return prepend(entries);
    }

    /**
     * Returns a new {@code EntryStream} which is a concatenation of three
     * supplied key-value pairs and this stream.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a> with <a href="package-summary.html#TSO">tail-stream
     * optimization</a>.
     * 
     * @param k1 the key of the first {@code Entry} to prepend to this stream
     * @param v1 the value of the first {@code Entry} to prepend to this stream
     * @param k2 the key of the second {@code Entry} to prepend to this stream
     * @param v2 the value of the second {@code Entry} to prepend to this stream
     * @param k3 the key of the third {@code Entry} to prepend to this stream
     * @param v3 the value of the third {@code Entry} to prepend to this stream
     * @return the new stream
     * @since 0.2.3
     */
    public EntryStream<K, V> prepend(K k1, V v1, K k2, V v2, K k3, V v3) {
        @SuppressWarnings("unchecked")
        final Map.Entry<K, V>[] entries = new Map.Entry[] { new SimpleImmutableEntry<>(k1, v1),
                new SimpleImmutableEntry<>(k2, v2), new SimpleImmutableEntry<>(k3, v3) };
        return prepend(entries);
    }

    @Override
    public EntryStream<K, V> prepend(Entry<K, V> entry) {
        return supply(new PrependSpliterator<>(spliterator(), entry));
    }

    @Override
    public EntryStream<K, V> prepend(Entry<K, V>... entries) {
        if (entries == null || entries.length == 0) {
            return this;
        }

        return prependSpliterator(null, Spliterators.spliterator(entries, Spliterator.ORDERED));
    }

    /**
     * Returns a new {@code EntryStream} which is a concatenation of the stream
     * created from the supplied map entries and this stream.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate
     * operation</a> with <a href="package-summary.html#TSO">tail-stream
     * optimization</a>.
     * 
     * <p>
     * May return this if the supplied map is empty and non-concurrent.
     * 
     * @param map the map to prepend to the stream
     * @return the new stream
     * @since 0.2.1
     */
    public EntryStream<K, V> prepend(Map<K, V> map) {
        return prependSpliterator(null, map.entrySet().spliterator());
    }

    public EntryStream<K, V> sortedByKey(final Comparator<? super K> cmp) {
        return sorted(Comparators.comparingByKey(cmp));
    }

    public EntryStream<K, V> sortedByValue(final Comparator<? super V> cmp) {
        return sorted(Comparators.comparingByValue(cmp));
    }

    /**
     * Returns a stream consisting of the elements of this stream which have
     * distinct keys (according to object equality).
     *
     * <p>
     * For ordered streams, the selection of distinct keys is stable (for
     * elements with duplicating keys, the element appearing first in the
     * encounter order is preserved.) For unordered streams, no stability
     * guarantees are made.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">stateful intermediate
     * operation</a>.
     *
     * @return the new stream
     * @since 0.3.8
     */
    public EntryStream<K, V> distinctByKey() {
        return distinctBy(Fn.key());
    }

    /**
     * Returns a stream consisting of the elements of this stream which have
     * distinct values (according to object equality).
     *
     * <p>
     * For ordered streams, the selection of distinct values is stable (for
     * elements with duplicating values, the element appearing first in the
     * encounter order is preserved.) For unordered streams, no stability
     * guarantees are made.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">stateful intermediate
     * operation</a>.
     *
     * @return the new stream
     * @since 0.3.8
     */
    public EntryStream<K, V> distinctByValue() {
        return distinctBy(Fn.value());
    }

    @Override
    public EntryStream<K, V> distinct(Predicate<? super Long> occurrencesFilter) {
        final Supplier<? extends Map<Map.Entry<K, V>, Long>> mapSupplier = Suppliers.ofLinkedHashMap();

        return groupBy(Fn.identity(), Fn.identity(), Collectors.counting(), mapSupplier).filter(
            e -> occurrencesFilter.test(e.getValue())).mapToEntry(Fn.key());
    }

    @Override
    public EntryStream<K, V> distinctBy(Function<? super Entry<K, V>, ?> keyExtractor,
            Predicate<? super Long> occurrencesFilter) {
        final Supplier<? extends Map<Keyed<?, Map.Entry<K, V>>, Long>> mapSupplier = isParallel()
                ? Suppliers.<Keyed<?, Map.Entry<K, V>>, Long> ofConcurrentHashMap()
                : Suppliers.<Keyed<?, Map.Entry<K, V>>, Long> ofLinkedHashMap();

        return groupBy(e -> Keyed.of(keyExtractor.apply(e), e), Fn.identity(), Collectors.counting(), mapSupplier)
                .filter(e -> occurrencesFilter.test(e.getValue())).mapToEntry(e -> e.getKey().val());
    }

    /**
     * Returns an {@code EntryStream} consisting of the entries whose keys are
     * modified by applying the given function and values are left unchanged.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <KK> The type of the keys of the new stream
     * @param keyMapper a non-interfering, stateless function to apply to each
     *        key
     * @return the new stream
     */
    public <KK> EntryStream<KK, V> mapKeys(Function<? super K, ? extends KK> keyMapper) {
        return new EntryStream<>(stream().map(e -> new SimpleImmutableEntry<>(keyMapper.apply(e.getKey()), e
                .getValue())), context);
    }

    /**
     * Returns an {@code EntryStream} consisting of the entries whose keys are
     * modified by applying the given function and values are left unchanged.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <KK> The type of the keys of the new stream
     * @param keyMapper a non-interfering, stateless function to apply to each
     *        key-value pair which returns the updated key
     * @return the new stream
     * @since 0.3.0
     */
    public <KK> EntryStream<KK, V> mapKeys(BiFunction<? super K, ? super V, ? extends KK> keyMapper) {
        return new EntryStream<>(stream().map(e -> new SimpleImmutableEntry<>(keyMapper.apply(e.getKey(), e.getValue()),
                e.getValue())), context);
    }

    /**
     * Returns an {@code EntryStream} consisting of the entries whose keys are
     * left unchanged and values are modified by applying the given function.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <VV> The type of the values of the new stream
     * @param valueMapper a non-interfering, stateless function to apply to each
     *        value
     * @return the new stream
     */
    public <VV> EntryStream<K, VV> mapValues(Function<? super V, ? extends VV> valueMapper) {
        return new EntryStream<>(stream().map(e -> new SimpleImmutableEntry<>(e.getKey(), valueMapper.apply(e
                .getValue()))), context);
    }

    /**
     * Returns an {@code EntryStream} consisting of the entries whose keys are
     * left unchanged and values are modified by applying the given function.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <VV> The type of the values of the new stream
     * @param valueMapper a non-interfering, stateless function to apply to each
     *        key-value pair which returns the updated value
     * @return the new stream
     * @since 0.3.0
     */
    public <VV> EntryStream<K, VV> mapValues(BiFunction<? super K, ? super V, ? extends VV> valueMapper) {
        return new EntryStream<>(stream().map(e -> new SimpleImmutableEntry<>(e.getKey(), valueMapper.apply(e.getKey(),
            e.getValue()))), context);
    }

    /**
     * Returns a {@link StreamEx} consisting of the results of applying the
     * given function to the keys and values of this stream.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <R> The element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to key and
     *        value of each {@link Entry} in this stream
     * @return the new stream
     */
    public <R> StreamEx<R> map(BiFunction<? super K, ? super V, ? extends R> mapper) {
        return this.<R> map(toFunction(mapper));
    }

    /**
     * Returns a stream consisting of the elements of this stream which keys
     * match the given predicate.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param keyPredicate a non-interfering, stateless predicate to apply to
     *        the key of each element to determine if it should be included
     * @return the new stream
     */
    public EntryStream<K, V> filterByKey(Predicate<? super K> keyPredicate) {
        return filter(e -> keyPredicate.test(e.getKey()));
    }

    /**
     * Returns a stream consisting of the elements of this stream which values
     * match the given predicate.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param valuePredicate a non-interfering, stateless predicate to apply to
     *        the value of each element to determine if it should be included
     * @return the new stream
     */
    public EntryStream<K, V> filterByValue(Predicate<? super V> valuePredicate) {
        return filter(e -> valuePredicate.test(e.getValue()));
    }

    /**
     * Returns a stream consisting of the elements of this stream which elements
     * match the given predicate.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param predicate a non-interfering, stateless predicate to apply to the
     *        key-value pairs of each element to determine if it should be
     *        included
     * @return the new stream
     * @since 0.3.0
     */
    public EntryStream<K, V> filter(BiPredicate<? super K, ? super V> predicate) {
        return filter(e -> predicate.test(e.getKey(), e.getValue()));
    }

    /**
     * Returns a stream consisting of the elements of this stream which keys
     * don't match the given predicate.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param keyPredicate a non-interfering, stateless predicate to apply to
     *        the key of each element to determine if it should be excluded
     * @return the new stream
     */
    public EntryStream<K, V> removeByKey(Predicate<? super K> keyPredicate) {
        return filterByKey(keyPredicate.negate());
    }

    /**
     * Returns a stream consisting of the elements of this stream which values
     * don't match the given predicate.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param valuePredicate a non-interfering, stateless predicate to apply to
     *        the value of each element to determine if it should be excluded
     * @return the new stream
     */
    public EntryStream<K, V> removeByValue(Predicate<? super V> valuePredicate) {
        return filterByValue(valuePredicate.negate());
    }

    /**
     * Returns a stream consisting of the elements of this stream which values
     * don't match the given predicate.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param predicate a non-interfering, stateless predicate to apply to the
     *        key-value pairs of each element to determine if it should be
     *        excluded
     * @return the new stream
     * @since 0.6.0
     */
    public EntryStream<K, V> removeIf(BiPredicate<? super K, ? super V> predicate) {
        return filter(predicate.negate());
    }

    @SuppressWarnings("rawtypes")
    private static final Predicate<Map.Entry> NON_KEY = e -> e.getKey() != null;
    @SuppressWarnings("rawtypes")
    private static final Predicate<Map.Entry> NON_VALUE = e -> e.getValue() != null;

    /**
     * Returns a stream consisting of the elements of this stream which key is
     * not null.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @return the new stream
     * @deprecated replaced by {@code #skipNullKeys()}
     */
    @Deprecated
    public EntryStream<K, V> nonNullKeys() {
        return filter(NON_KEY);
    }

    /**
     * Returns a stream consisting of the elements of this stream which key is
     * not null.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @return the new stream
     */
    public EntryStream<K, V> skipNullKeys() {
        return filter(NON_KEY);
    }

    /**
     * Returns a stream consisting of the elements of this stream which value is
     * not null.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @return the new stream
     * @deprecated replaced by {@code #skipNullValues()}
     */
    @Deprecated
    public EntryStream<K, V> nonNullValues() {
        return filter(NON_VALUE);
    }

    /**
     * Returns a stream consisting of the elements of this stream which value is
     * not null.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @return the new stream
     */
    public EntryStream<K, V> skipNullValues() {
        return filter(NON_VALUE);
    }

    /**
     * Returns a stream consisting of the elements of this stream which keys are
     * instances of given class.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <KK> a type of keys to select.
     * @param clazz a class to filter the keys.
     * @return the new stream
     */
    @SuppressWarnings({ "unchecked" })
    public <KK> EntryStream<KK, V> selectKeys(Class<KK> clazz) {
        return (EntryStream<KK, V>) filter(e -> clazz.isInstance(e.getKey()));
    }

    /**
     * Returns a stream consisting of the elements of this stream which values
     * are instances of given class.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <VV> a type of values to select.
     * @param clazz a class to filter the values.
     * @return the new stream
     */
    @SuppressWarnings({ "unchecked" })
    public <VV> EntryStream<K, VV> selectValues(Class<VV> clazz) {
        return (EntryStream<K, VV>) filter(e -> clazz.isInstance(e.getValue()));
    }

    /**
     * Returns a stream consisting of the entries of this stream, additionally
     * performing the provided action on each entry key as entries are consumed
     * from the resulting stream.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * <p>
     * For parallel stream pipelines, the action may be called at whatever time
     * and in whatever thread the element is made available by the upstream
     * operation. If the action modifies shared state, it is responsible for
     * providing the required synchronization.
     *
     * @param keyAction a non-interfering action to perform on the keys of the
     *        entries as they are consumed from the stream
     * @return the new stream
     * @since 0.2.3
     */
    public EntryStream<K, V> peekKeys(Consumer<? super K> keyAction) {
        return peek(e -> keyAction.accept(e.getKey()));
    }

    /**
     * Returns a stream consisting of the entries of this stream, additionally
     * performing the provided action on each entry value as entries are
     * consumed from the resulting stream.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * <p>
     * For parallel stream pipelines, the action may be called at whatever time
     * and in whatever thread the element is made available by the upstream
     * operation. If the action modifies shared state, it is responsible for
     * providing the required synchronization.
     *
     * @param valueAction a non-interfering action to perform on the values of
     *        the entries as they are consumed from the stream
     * @return the new stream
     * @since 0.2.3
     */
    public EntryStream<K, V> peekValues(Consumer<? super V> valueAction) {
        return peek(e -> valueAction.accept(e.getValue()));
    }

    /**
     * Returns a stream consisting of the entries of this stream, additionally
     * performing the provided action on each entry key-value pair as entries
     * are consumed from the resulting stream.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * <p>
     * For parallel stream pipelines, the action may be called at whatever time
     * and in whatever thread the element is made available by the upstream
     * operation. If the action modifies shared state, it is responsible for
     * providing the required synchronization.
     *
     * @param action a non-interfering action to perform on the keys and values
     *        of the entries as they are consumed from the stream
     * @return the new stream
     * @since 0.2.3
     */
    public EntryStream<K, V> peek(BiConsumer<? super K, ? super V> action) {
        return peek(toConsumer(action));
    }

    /**
     * Returns a stream consisting of the keys of this stream elements.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @return the new stream
     */
    public StreamEx<K> keys() {
        return map(Entry::getKey);
    }

    /**
     * Returns a stream consisting of the values of this stream elements.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @return the new stream
     */
    public StreamEx<V> values() {
        return map(Entry::getValue);
    }

    public StreamEx<Map.Entry<K, V>> entries() {
        return map(Fn.identity());
    }

    /**
     * Returns a stream consisting of the {@link Entry} objects which keys are
     * the values of this stream elements and vice versa.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @return the new stream
     */
    public EntryStream<V, K> inversed() {
        return new EntryStream<>(stream().map(e -> new SimpleImmutableEntry<>(e.getValue(), e.getKey())), context);
    }

    /**
     * Merge series of adjacent stream entries with equal keys grouping the
     * corresponding values into {@code List}.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate</a>
     * partial reduction operation.
     * 
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code List} objects of the resulting stream.
     *
     * <p>
     * The key of the resulting entry is the key of the first merged entry.
     * 
     * @return a new {@code EntryStream} which keys are the keys of the original
     *         stream and the values of adjacent entries with the same keys are
     *         grouped into {@code List}
     * @see StreamEx#groupRuns(BiPredicate)
     * @since 0.5.5
     */
    @SuppressWarnings("resource")
    public EntryStream<K, List<V>> collapseKeys() {
        return new StreamEx<>(new CollapseSpliterator<Entry<K, V>, PairBox<K, List<V>>>(equalKeys(), e -> new PairBox<>(
                e.getKey(), Collections.singletonList(e.getValue())), (pb, e) -> {
                    if (!(pb.b instanceof ArrayList)) {
                        V old = pb.b.get(0);
                        pb.b = new ArrayList<>();
                        pb.b.add(old);
                    }
                    pb.b.add(e.getValue());
                    return pb;
                }, (pb1, pb2) -> {
                    if (!(pb1.b instanceof ArrayList)) {
                        V old = pb1.b.get(0);
                        pb1.b = new ArrayList<>();
                        pb1.b.add(old);
                    }
                    pb1.b.addAll(pb2.b);
                    return pb1;
                }, spliterator()), context).mapToEntry(pb -> pb.a, pb -> pb.b);
    }

    /**
     * Merge series of adjacent stream entries with equal keys combining the
     * corresponding values using the provided function.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate</a>
     * partial reduction operation.
     * 
     * <p>
     * The key of the resulting entry is the key of the first merged entry.
     *
     * @param merger a non-interfering, stateless, associative function to merge
     *        values of two adjacent entries which keys are equal. Note that it
     *        can be applied to the results if previous merges.
     * @return a new {@code EntryStream} which keys are the keys of the original
     *         stream and the values are values of the adjacent entries with the
     *         same keys, combined using the provided merger function.
     * @see StreamEx#collapse(BiPredicate, BinaryOperator)
     * @since 0.5.5
     */
    public EntryStream<K, V> collapseKeys(BinaryOperator<V> merger) {
        BinaryOperator<Entry<K, V>> entryMerger = (e1, e2) -> new SimpleImmutableEntry<>(e1.getKey(), merger.apply(e1
                .getValue(), e2.getValue()));
        return new EntryStream<>(new CollapseSpliterator<>(equalKeys(), Fn.identity(), entryMerger, entryMerger,
                spliterator()), context);
    }

    /**
     * Merge series of adjacent stream entries with equal keys combining the
     * corresponding values using the provided {@code Collector}.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate</a>
     * partial reduction operation.
     * 
     * <p>
     * The key of the resulting entry is the key of the first merged entry.
     *
     * @param <R> the type of the values in the resulting stream
     * @param <A> the intermediate accumulation type of the {@code Collector}
     * @param collector a {@code Collector} which is used to combine the values
     *        of the adjacent entries with the equal keys.
     * @return a new {@code EntryStream} which keys are the keys of the original
     *         stream and the values are values of the adjacent entries with the
     *         same keys, combined using the provided collector.
     * @see StreamEx#collapse(BiPredicate, Collector)
     * @since 0.5.5
     */
    @SuppressWarnings("resource")
    public <A, R> EntryStream<K, R> collapseKeys(Collector<? super V, A, R> collector) {
        Supplier<A> supplier = collector.supplier();
        BiConsumer<A, ? super V> accumulator = collector.accumulator();
        BinaryOperator<A> combiner = collector.combiner();
        Function<A, R> finisher = collector.finisher();
        return new StreamEx<>(new CollapseSpliterator<Entry<K, V>, PairBox<K, A>>(equalKeys(), e -> {
            A a = supplier.get();
            accumulator.accept(a, e.getValue());
            return new PairBox<>(e.getKey(), a);
        }, (pb, e) -> {
            accumulator.accept(pb.b, e.getValue());
            return pb;
        }, (pb1, pb2) -> {
            pb1.b = combiner.apply(pb1.b, pb2.b);
            return pb1;
        }, spliterator()), context).mapToEntry(pb -> pb.a, pb -> finisher.apply(pb.b));
    }

    /**
     * Returns a new {@code EntryStream} which values are the same as this
     * stream values and keys are the results of applying the accumulation
     * function to this stream keys, going left to right.
     * 
     * <p>
     * This is a stateful
     * <a href="package-summary.html#StreamOps">quasi-intermediate</a>
     * operation.
     *
     * <p>
     * This method cannot take all the advantages of parallel streams as it must
     * process elements strictly left to right. Using an unordered source or
     * removing the ordering constraint with {@link #unordered()} may improve
     * the parallel processing speed.
     *
     * @param op an <a href="package-summary.html#Associativity">associative</a>
     *        , <a href="package-summary.html#NonInterference">non-interfering
     *        </a>, <a href="package-summary.html#Statelessness">stateless</a>
     *        function for computing the next key based on the previous one
     * @return the new stream.
     * @see #scan(BinaryOperator)
     * @see #scanValues(BinaryOperator)
     * @since 0.6.4
     */
    public EntryStream<K, V> scanKeys(BinaryOperator<K> op) {
        return scan((a, b) -> new SimpleImmutableEntry<>(op.apply(a.getKey(), b.getKey()), b.getValue()));
    }

    /**
     * Returns a new {@code EntryStream} which keys are the same as this stream
     * keys and values are the results of applying the accumulation function to
     * this stream values, going left to right.
     * 
     * <p>
     * This is a stateful
     * <a href="package-summary.html#StreamOps">quasi-intermediate</a>
     * operation.
     *
     * <p>
     * This method cannot take all the advantages of parallel streams as it must
     * process elements strictly left to right. Using an unordered source or
     * removing the ordering constraint with {@link #unordered()} may improve
     * the parallel processing speed.
     *
     * @param op an <a href="package-summary.html#Associativity">associative</a>
     *        , <a href="package-summary.html#NonInterference">non-interfering
     *        </a>, <a href="package-summary.html#Statelessness">stateless</a>
     *        function for computing the next value based on the previous one
     * @return the new stream.
     * @see #scan(BinaryOperator)
     * @see #scanKeys(BinaryOperator)
     * @since 0.6.4
     */
    public EntryStream<K, V> scanValues(BinaryOperator<V> op) {
        return scan((a, b) -> new SimpleImmutableEntry<>(b.getKey(), op.apply(a.getValue(), b.getValue())));
    }

    public <KK, VV> EntryStream<KK, VV> scan(final Map.Entry<KK, VV> seed,
            final BiFunction<? super Map.Entry<KK, VV>, ? super Map.Entry<K, V>, Map.Entry<KK, VV>> op) {
        return new EntryStream<>(new UnknownSizeSpliterator.USOfRef<>(new Iterator<Map.Entry<KK, VV>>() {
            private final Iterator<Map.Entry<K, V>> iter = iterator();
            private boolean isFirst = true;
            private Map.Entry<KK, VV> val = null;

            @Override
            public boolean hasNext() {
                return iter.hasNext() || isFirst;
            }

            @Override
            public Map.Entry<KK, VV> next() {
                if (!(iter.hasNext() || isFirst)) {
                    throw new NoSuchElementException();
                }

                if (isFirst) {
                    val = seed;
                    isFirst = false;
                } else {
                    val = op.apply(val, iter.next());
                }

                return val;
            }
        }), this.context);
    }

    /**
     * Returns a {@link Map} containing the elements of this stream. There are
     * no guarantees on the type or serializability of the {@code Map} returned;
     * if more control over the returned {@code Map} is required, use
     * {@link #toMap(Supplier)}.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     *
     * <p>
     * Returned {@code Map} is guaranteed to be modifiable.
     *
     * <p>
     * For parallel stream the concurrent {@code Map} is created.
     *
     * @return a {@code Map} containing the elements of this stream
     * @throws IllegalStateException if this stream contains duplicate keys
     *         (according to {@link Object#equals(Object)})
     * @see Collectors#toMap(Function, Function)
     * @see Collectors#toConcurrentMap(Function, Function)
     * @see #toImmutableMap()
     */
    public Map<K, V> toMap() {
        return toMap(Fn.throwingMerger(), Suppliers.ofMap());
    }

    /**
     * Returns a {@link Map} containing the elements of this stream. There are
     * no guarantees on the type or serializability of the {@code Map} returned;
     * if more control over the returned {@code Map} is required, use
     * {@link #toMap(BinaryOperator, Supplier)}.
     *
     * <p>
     * If the mapped keys contains duplicates (according to
     * {@link Object#equals(Object)}), the value mapping function is applied to
     * each equal element, and the results are merged using the provided merging
     * function.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     *
     * <p>
     * Returned {@code Map} is guaranteed to be modifiable.
     *
     * @param mergeFunction a merge function, used to resolve collisions between
     *        values associated with the same key, as supplied to
     *        {@link Map#merge(Object, Object, BiFunction)}
     * @return a {@code Map} containing the elements of this stream
     * @see Collectors#toMap(Function, Function)
     * @see Collectors#toConcurrentMap(Function, Function)
     * @since 0.1.0
     */
    public Map<K, V> toMap(BinaryOperator<V> mergeFunction) {
        return toMap(mergeFunction, Suppliers.ofMap());
    }

    /**
     * Returns a {@link Map} containing the elements of this stream. The
     * {@code Map} is created by a provided supplier function.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     * 
     * @param <M> the type of the resulting map
     * @param mapSupplier a function which returns a new, empty {@code Map} into
     *        which the results will be inserted
     * @return a {@code Map} containing the elements of this stream
     * @throws IllegalStateException if this stream contains duplicate keys
     *         (according to {@link Object#equals(Object)})
     * @see Collectors#toMap(Function, Function)
     * @see Collectors#toConcurrentMap(Function, Function)
     */
    public <M extends Map<K, V>> M toMap(Supplier<M> mapSupplier) {
        return toMap(Fn.throwingMerger(), mapSupplier);
    }

    /**
     * Returns a {@link Map} containing the elements of this stream. The
     * {@code Map} is created by a provided supplier function.
     *
     * <p>
     * If the mapped keys contains duplicates (according to
     * {@link Object#equals(Object)}), the value mapping function is applied to
     * each equal element, and the results are merged using the provided merging
     * function.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     * 
     * @param <M> the type of the resulting map
     * @param mergeFunction a merge function, used to resolve collisions between
     *        values associated with the same key.
     * @param mapSupplier a function which returns a new, empty {@code Map} into
     *        which the results will be inserted
     * @return a {@code Map} containing the elements of this stream
     * @see Collectors#toMap(Function, Function)
     * @see Collectors#toConcurrentMap(Function, Function)
     */
    public <M extends Map<K, V>> M toMap(BinaryOperator<V> mergeFunction, Supplier<M> mapSupplier) {
        // return rawCollect(MoreCollectors.toMap(Fn.key(), Fn.value(), mergeFunction, mapSupplier));

        if (isParallel() && mapSupplier.get() instanceof ConcurrentMap)
            return (M) rawCollect(MoreCollectors.toConcurrentMap(Fn.key(), Fn.value(), mergeFunction,
                (Supplier<ConcurrentMap<K, V>>) mapSupplier));

        return rawCollect(MoreCollectors.toMap(Fn.key(), Fn.value(), mergeFunction, mapSupplier));
    }

    public <KK, VV> Map<KK, VV> toMap(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valMapper) {
        return toMap(keyMapper, valMapper, Fn.throwingMerger());
    }

    public <KK, VV> Map<KK, VV> toMap(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valMapper, final BinaryOperator<VV> mergeFunction) {
        return toMap(keyMapper, valMapper, mergeFunction, Suppliers.ofMap());
    }

    public <KK, VV, M extends Map<KK, VV>> M toMap(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valMapper, final Supplier<M> mapSupplier) {
        return toMap(keyMapper, valMapper, Fn.throwingMerger(), mapSupplier);
    }

    public <KK, VV, M extends Map<KK, VV>> M toMap(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valMapper, final BinaryOperator<VV> mergeFunction,
            final Supplier<M> mapSupplier) {
        // return rawCollect(MoreCollectors.toMap(Fn.key(), Fn.value(), mergeFunction, mapSupplier));

        if (isParallel() && mapSupplier.get() instanceof ConcurrentMap)
            return (M) rawCollect(MoreCollectors.toConcurrentMap(keyMapper, valMapper, mergeFunction,
                (Supplier<ConcurrentMap<KK, VV>>) mapSupplier));

        return rawCollect(MoreCollectors.toMap(keyMapper, valMapper, mergeFunction, mapSupplier));
    }

    /**
     * Returns a {@link SortedMap} containing the elements of this stream. There
     * are no guarantees on the type or serializability of the {@code SortedMap}
     * returned; if more control over the returned {@code Map} is required, use
     * {@link #toMap(Supplier)}.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     *
     * <p>
     * Returned {@code SortedMap} is guaranteed to be modifiable.
     *
     * <p>
     * For parallel stream the concurrent {@code SortedMap} is created.
     *
     * @return a {@code SortedMap} containing the elements of this stream
     * @see #toNavigableMap()
     * @throws IllegalStateException if this stream contains duplicate keys
     *         (according to {@link Object#equals(Object)})
     * @since 0.1.0
     */
    public SortedMap<K, V> toSortedMap() {
        return toSortedMap(Fn.throwingMerger());
    }

    /**
     * Returns a {@link SortedMap} containing the elements of this stream. There
     * are no guarantees on the type or serializability of the {@code SortedMap}
     * returned; if more control over the returned {@code Map} is required, use
     * {@link #toMap(BinaryOperator, Supplier)}.
     *
     * <p>
     * If the mapped keys contains duplicates (according to
     * {@link Object#equals(Object)}), the value mapping function is applied to
     * each equal element, and the results are merged using the provided merging
     * function.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     *
     * <p>
     * Returned {@code SortedMap} is guaranteed to be modifiable.
     *
     * @param mergeFunction a merge function, used to resolve collisions between
     *        values associated with the same key, as supplied to
     *        {@link Map#merge(Object, Object, BiFunction)}
     * @return a {@code SortedMap} containing the elements of this stream
     * @see #toNavigableMap(BinaryOperator)
     * @since 0.1.0
     */
    public SortedMap<K, V> toSortedMap(BinaryOperator<V> mergeFunction) {
        return toMap(mergeFunction, Suppliers.ofTreeMap());
    }

    /**
     * Returns a {@link NavigableMap} containing the elements of this stream.
     * There are no guarantees on the type or serializability of the
     * {@code NavigableMap} returned; if more control over the returned
     * {@code Map} is required, use {@link #toMap(Supplier)}.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     *
     * <p>
     * Returned {@code NavigableMap} is guaranteed to be modifiable.
     *
     * <p>
     * For parallel stream the concurrent {@code NavigableMap} is created.
     *
     * @return a {@code NavigableMap} containing the elements of this stream
     * @see Collectors#toMap(Function, Function)
     * @see Collectors#toConcurrentMap(Function, Function)
     * @throws IllegalStateException if this stream contains duplicate keys
     *         (according to {@link Object#equals(Object)})
     * @since 0.6.5
     */
    public NavigableMap<K, V> toNavigableMap() {
        return toNavigableMap(Fn.throwingMerger());
    }

    /**
     * Returns a {@link NavigableMap} containing the elements of this stream.
     * There are no guarantees on the type or serializability of the
     * {@code NavigableMap} returned; if more control over the returned
     * {@code Map} is required, use {@link #toMap(BinaryOperator, Supplier)}.
     *
     * <p>
     * If the mapped keys contains duplicates (according to
     * {@link Object#equals(Object)}), the value mapping function is applied to
     * each equal element, and the results are merged using the provided merging
     * function.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     *
     * <p>
     * Returned {@code NavigableMap} is guaranteed to be modifiable.
     *
     * @param mergeFunction a merge function, used to resolve collisions between
     *        values associated with the same key, as supplied to
     *        {@link Map#merge(Object, Object, BiFunction)}
     * @return a {@code NavigableMap} containing the elements of this stream
     * @see Collectors#toMap(Function, Function)
     * @since 0.6.5
     */
    public NavigableMap<K, V> toNavigableMap(BinaryOperator<V> mergeFunction) {
        return toMap(mergeFunction, Suppliers.ofTreeMap());
    }

    /**
     * Returns an immutable {@link Map} containing the elements of this stream.
     * There's no guarantees on exact type of the returned {@code Map}. In
     * particular, no specific element order in the resulting {@code Map} is
     * guaranteed. The returned {@code Map} is guaranteed to be serializable if
     * all its elements are serializable.
     *
     * <p>
     * This is a terminal operation.
     *
     * @return a {@code Map} containing the elements of this stream
     * @throws IllegalStateException if this stream contains duplicate keys
     *         (according to {@link Object#equals(Object)})
     * @see #toMap()
     * @since 0.6.3
     */
    public Map<K, V> toImmutableMap() {
        Map<K, V> map = toMap();
        return map.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(map);
    }

    /**
     * Creates a {@link Map} containing the elements of this stream, then
     * performs finishing transformation and returns its result. There are no
     * guarantees on the type or serializability of the {@code Map} created.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     *
     * <p>
     * Created {@code Map} is guaranteed to be modifiable.
     *
     * <p>
     * For parallel stream the concurrent {@code Map} is created.
     *
     * @param <R> the type of the result
     * @param finisher a function to be applied to the intermediate map
     * @return result of applying the finisher transformation to the {@code Map}
     *         of the stream elements.
     * @throws IllegalStateException if this stream contains duplicate keys
     *         (according to {@link Object#equals(Object)})
     * @see #toMap()
     * @since 0.5.5
     */
    public <R> R toMapAndThen(Function<? super Map<K, V>, R> finisher) {
        if (context.fjp != null)
            return context.terminate(() -> finisher.apply(toMap()));
        return finisher.apply(toMap());
    }

    /**
     * Drains the stream content into the supplied {@code Map}.
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     * 
     * @param <M> type of the resulting map
     * @param map a mutable map to put the stream elements into
     * @return the supplied map, updated from this stream
     * @throws IllegalStateException if this stream contains duplicate keys, or
     *         the stream contains the key which was already present in the map
     *         (according to {@link Object#equals(Object)})
     * @since 0.6.3
     */
    public <M extends Map<K, V>> M into(M map) {
        Consumer<? super Entry<K, V>> cons = toMapConsumer(map);
        if (isParallel()) {
            if (map instanceof ConcurrentMap)
                forEach(cons);
            else
                toMap().entrySet().forEach(cons);
        } else {
            spliterator().forEachRemaining(cons);
        }
        return map;
    }

    /**
     * Returns a {@link EntryStream<K, List<V>>} where elements of this stream
     * with the same key are grouped together. The resulting {@code Map} keys
     * are the keys of this stream entries and the values are the lists of the
     * corresponding values.
     *
     * @return a {@code Map} containing the elements of this stream
     * @see #groupTo()
     * @see Collectors#groupingBy(Function)
     * @since 0.8
     */
    public EntryStream<K, List<V>> groupBy() {
        return groupBy(Collectors.toList());
    }

    /**
     * Returns a {@link EntryStream<K, List<V>>} where elements of this stream
     * with the same key are grouped together. The resulting {@code Map} keys
     * are the keys of this stream entries and the values are the lists of the
     * corresponding values. The {@code Map} is created using the provided
     * supplier function.
     *
     * @param <M> the type of the resulting {@code Map}
     * @param mapSupplier a function which returns a new, empty {@code Map} into
     *        which the results will be inserted
     * @return a {@code Map} containing the elements of this stream
     * @see #groupTo(Supplier)
     * @since 0.8
     */
    public EntryStream<K, List<V>> groupBy(Supplier<? extends Map<K, List<V>>> mapSupplier) {
        return groupBy(Collectors.toList(), mapSupplier);
    }

    /**
     * Returns a {@link EntryStream<K, C>} where elements of this stream with
     * the same key are grouped together. The resulting {@code Map} keys are the
     * keys of this stream entries and the values are the collections of the
     * corresponding values. The collections are created by the provided
     * factory.
     * 
     * @param collectionFactory a {@code Supplier} which returns a new, empty
     *        {@code Collection} of the appropriate type
     * @param mapSupplier a function which returns a new, empty {@code Map} into
     *        which the results will be inserted
     *
     * @param <C> the type of the resulting {@code Collection}
     * @param <M> the type of the resulting {@code Map}
     * @return a {@code Map} containing the elements of this stream
     * @see #groupTo(Supplier, Supplier)
     * @see Collectors#toCollection(Supplier)
     * @since 0.8
     * @deprecated replaced by {@link #groupBy(Collector, Supplier)}
     */
    @Deprecated
    public <C extends Collection<V>> EntryStream<K, C> groupBy(Supplier<? extends C> collectionFactory,
            Supplier<? extends Map<K, C>> mapSupplier) {
        return groupBy(Collectors.toCollection((Supplier<C>) collectionFactory), mapSupplier);
    }

    /**
     * Returns a {@link Map} where elements of this stream with the same key are
     * grouped together. The resulting {@code Map} keys are the keys of this
     * stream entries and the corresponding values are combined using the
     * provided downstream collector.
     *
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Map} containing the elements of this stream
     * @see #groupTo(Collector)
     * @see Collectors#groupingBy(Function, Collector)
     * @since 0.8
     */
    public <A, D> EntryStream<K, D> groupBy(Collector<? super V, A, D> downstream) {
        return groupBy(downstream, Suppliers.ofMap());
    }

    /**
     * Returns a {@link Map} where elements of this stream with the same key are
     * grouped together. The resulting {@code Map} keys are the keys of this
     * stream entries and the corresponding values are combined using the
     * provided downstream collector. The {@code Map} is created using the
     * provided supplier function.
     * 
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @param mapSupplier a function which returns a new, empty {@code Map} into
     *        which the results will be inserted
     *
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param <M> the type of the resulting {@code Map}
     * @return a {@code Map} containing the elements of this stream
     * @see #groupTo(Collector, Supplier)
     * @see Collectors#groupingBy(Function, Supplier, Collector)
     * @since 0.8
     */
    public <A, D> EntryStream<K, D> groupBy(Collector<? super V, A, D> downstream,
            Supplier<? extends Map<K, D>> mapSupplier) {
        return groupBy(Fn.key(), Fn.value(), downstream, mapSupplier);
    }

    public <KK, VV> EntryStream<KK, List<VV>> groupBy(final Function<? super Map.Entry<K, V>, ? extends KK> classifier,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper) {
        return groupBy(classifier, valueMapper, Collectors.toList());
    }

    public <KK, VV, M extends Map<KK, List<VV>>> EntryStream<KK, List<VV>> groupBy(
            final Function<? super Map.Entry<K, V>, ? extends KK> classifier,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper, Supplier<M> mapSupplier) {
        return groupBy(classifier, valueMapper, Collectors.toList(), mapSupplier);
    }

    public <A, D, KK, VV> EntryStream<KK, D> groupBy(final Function<? super Map.Entry<K, V>, ? extends KK> classifier,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper,
            final Collector<? super VV, A, D> downstream) {
        return groupBy(classifier, valueMapper, downstream, Suppliers.ofMap());
    }

    public <A, D, KK, VV, M extends Map<KK, D>> EntryStream<KK, D> groupBy(
            final Function<? super Map.Entry<K, V>, ? extends KK> classifier,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper,
            final Collector<? super VV, A, D> downstream, final Supplier<M> mapSupplier) {
        Supplier<Stream<Map.Entry<KK, D>>> supplier = null;

        if (context.parallel) {
            supplier = () -> groupTo(classifier, valueMapper, downstream, mapSupplier).entrySet().parallelStream();
        } else {
            supplier = () -> groupTo(classifier, valueMapper, downstream, mapSupplier).entrySet().stream();
        }

        return new EntryStream<>(ForwardingStream.of(supplier), context);
    }

    /**
     * Returns a {@link Map} where elements of this stream with the same key are
     * grouped together. The resulting {@code Map} keys are the keys of this
     * stream entries and the values are the lists of the corresponding values.
     * 
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code Map} or {@code List} objects returned. If
     * more control over the returned {@code Map} is required, use
     * {@link #groupTo(Supplier)}. If more control over the lists required, use
     * {@link #groupingTo(Supplier)}.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     *
     * @return a {@code Map} containing the elements of this stream
     * @see Collectors#groupingBy(Function)
     */
    public Map<K, List<V>> groupTo() {
        return groupTo(Collectors.toList());
    }

    /**
     * Returns a {@link Map} where elements of this stream with the same key are
     * grouped together. The resulting {@code Map} keys are the keys of this
     * stream entries and the values are the lists of the corresponding values.
     * The {@code Map} is created using the provided supplier function.
     * 
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code List} objects returned. If more control over
     * the lists required, use {@link #groupingTo(Supplier)}.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     *
     * @param <M> the type of the resulting {@code Map}
     * @param mapSupplier a function which returns a new, empty {@code Map} into
     *        which the results will be inserted
     * @return a {@code Map} containing the elements of this stream
     * @see #groupTo(Collector, Supplier)
     * @see #groupTo(Supplier, Supplier)
     */
    public <M extends Map<K, List<V>>> M groupTo(Supplier<M> mapSupplier) {
        return groupTo(Collectors.toList(), mapSupplier);
    }

    /**
     * Returns a {@link Map} where elements of this stream with the same key are
     * grouped together. The resulting {@code Map} keys are the keys of this
     * stream entries and the values are the collections of the corresponding
     * values. The collections are created by the provided factory.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     * 
     * @param collectionFactory a {@code Supplier} which returns a new, empty
     *        {@code Collection} of the appropriate type
     * @param mapSupplier a function which returns a new, empty {@code Map} into
     *        which the results will be inserted
     *
     * @param <C> the type of the resulting {@code Collection}
     * @param <M> the type of the resulting {@code Map}
     * @return a {@code Map} containing the elements of this stream
     * @see Collectors#toCollection(Supplier)
     * @deprecated replaced by {@link #groupTo(Collector, Supplier)}
     */
    @Deprecated
    public <C extends Collection<V>, M extends Map<K, C>> M groupTo(Supplier<? extends C> collectionFactory,
            Supplier<M> mapSupplier) {
        return groupTo(Collectors.toCollection((Supplier<C>) collectionFactory), mapSupplier);
    }

    /**
     * Returns a {@link Map} where elements of this stream with the same key are
     * grouped together. The resulting {@code Map} keys are the keys of this
     * stream entries and the corresponding values are combined using the
     * provided downstream collector.
     * 
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code Map} object returned. If more control over
     * the returned {@code Map} is required, use
     * {@link #groupTo(Collector, Supplier)}.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     *
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @return a {@code Map} containing the elements of this stream
     * @see Collectors#groupingBy(Function, Collector)
     */
    public <A, D> Map<K, D> groupTo(Collector<? super V, A, D> downstream) {
        Function<Entry<K, V>, K> keyMapper = Entry::getKey;
        Collector<Entry<K, V>, ?, D> mapping = MoreCollectors.mapping(Entry::getValue, downstream);
        if (isParallel() && downstream.characteristics().contains(Characteristics.UNORDERED)) {
            return collect(MoreCollectors.groupingByConcurrent(keyMapper, mapping));
        }
        return collect(MoreCollectors.groupingBy(keyMapper, mapping));
    }

    /**
     * Returns a {@link Map} where elements of this stream with the same key are
     * grouped together. The resulting {@code Map} keys are the keys of this
     * stream entries and the corresponding values are combined using the
     * provided downstream collector. The {@code Map} is created using the
     * provided supplier function.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     * 
     * @param downstream a {@code Collector} implementing the downstream
     *        reduction
     * @param mapSupplier a function which returns a new, empty {@code Map} into
     *        which the results will be inserted
     *
     * @param <A> the intermediate accumulation type of the downstream collector
     * @param <D> the result type of the downstream reduction
     * @param <M> the type of the resulting {@code Map}
     * @return a {@code Map} containing the elements of this stream
     * @see Collectors#groupingBy(Function, Supplier, Collector)
     */
    @SuppressWarnings("unchecked")
    public <A, D, M extends Map<K, D>> M groupTo(final Collector<? super V, A, D> downstream,
            final Supplier<M> mapSupplier) {
        Function<Entry<K, V>, K> keyMapper = Entry::getKey;
        Collector<Entry<K, V>, ?, D> mapping = MoreCollectors.mapping(Entry::getValue, downstream);
        if (isParallel() && downstream.characteristics().contains(Characteristics.UNORDERED) && mapSupplier
                .get() instanceof ConcurrentMap) {
            return (M) collect(MoreCollectors.groupingByConcurrent(keyMapper, mapping,
                (Supplier<? extends ConcurrentMap<K, D>>) mapSupplier));
        }
        return collect(MoreCollectors.groupingBy(keyMapper, mapping, mapSupplier));
    }

    public <KK, VV> Map<KK, List<VV>> groupTo(final Function<? super Map.Entry<K, V>, ? extends KK> classifier,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper) {
        return groupTo(classifier, valueMapper, Collectors.toList());
    }

    public <KK, VV, M extends Map<KK, List<VV>>> M groupTo(
            final Function<? super Map.Entry<K, V>, ? extends KK> classifier,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper, final Supplier<M> mapSupplier) {
        return groupTo(classifier, valueMapper, Collectors.toList(), mapSupplier);
    }

    public <A, D, KK, VV> Map<KK, D> groupTo(final Function<? super Map.Entry<K, V>, ? extends KK> classifier,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper,
            final Collector<? super VV, A, D> downstream) {
        return groupTo(classifier, valueMapper, downstream, Suppliers.ofMap());
    }

    public <A, D, KK, VV, M extends Map<KK, D>> M groupTo(
            final Function<? super Map.Entry<K, V>, ? extends KK> classifier,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper,
            final Collector<? super VV, A, D> downstream, final Supplier<M> mapSupplier) {
        final Collector<Map.Entry<K, V>, ?, D> mapping = MoreCollectors.mapping(valueMapper, downstream);

        if (isParallel() && downstream.characteristics().contains(Characteristics.UNORDERED) && mapSupplier
                .get() instanceof ConcurrentMap) {
            return (M) collect(MoreCollectors.groupingByConcurrent(classifier, mapping,
                (Supplier<? extends ConcurrentMap<KK, D>>) mapSupplier));
        }
        return collect(MoreCollectors.groupingBy(classifier, mapping, mapSupplier));
    }

    /**
     * Performs an action for each key-value pair of this stream.
     *
     * <p>
     * This is a <a href="package-summary.html#StreamOps">terminal</a>
     * operation.
     *
     * <p>
     * The behavior of this operation is explicitly nondeterministic. For
     * parallel stream pipelines, this operation does <em>not</em> guarantee to
     * respect the encounter order of the stream, as doing so would sacrifice
     * the benefit of parallelism. For any given element, the action may be
     * performed at whatever time and in whatever thread the library chooses. If
     * the action accesses shared state, it is responsible for providing the
     * required synchronization.
     *
     * @param action a non-interfering action to perform on the key and value
     * @see #forEach(java.util.function.Consumer)
     */
    public void forEach(BiConsumer<? super K, ? super V> action) {
        forEach(toConsumer(action));
    }

    /**
     * short-cut for:
     * <code>entries().xxx().mapToEntry(Function.Identity())</code>
     * 
     * @param transfer
     * @return
     */
    public <KK, VV> EntryStream<KK, VV> chain(
            Function<? super StreamEx<Map.Entry<K, V>>, StreamEx<Map.Entry<KK, VV>>> transfer) {
        return transfer.apply(entries()).mapToEntry(Fn.identity());
    }

    /**
     * Returns an empty sequential {@code EntryStream}.
     *
     * @param <K> the type of stream element keys
     * @param <V> the type of stream element values
     * @return an empty sequential stream
     * @since 0.0.8
     */
    public static <K, V> EntryStream<K, V> empty() {
        return of(Stream.empty());
    }

    /**
     * Returns an {@code EntryStream} object which wraps given {@link Stream} of
     * {@link Entry} elements
     * 
     * @param <K> the type of original stream keys
     * @param <V> the type of original stream values
     * @param stream original stream
     * @return the wrapped stream
     */
    public static <K, V> EntryStream<K, V> of(Stream<? extends Entry<K, V>> stream) {
        if (stream instanceof AbstractStreamEx) {
            @SuppressWarnings("unchecked")
            AbstractStreamEx<Entry<K, V>, ?> ase = (AbstractStreamEx<Entry<K, V>, ?>) stream;
            if (ase.spliterator != null)
                return new EntryStream<>(ase.spliterator(), ase.context);
            return new EntryStream<>(ase.stream(), ase.context);
        }
        return new EntryStream<>(stream, StreamContext.of(stream));
    }

    /**
     * Returns a sequential {@link EntryStream} created from given
     * {@link Spliterator}.
     *
     * @param <K> the type of stream keys
     * @param <V> the type of stream values
     * @param spliterator a spliterator to create the stream from.
     * @return the new stream
     * @since 0.3.4
     */
    public static <K, V> EntryStream<K, V> of(Spliterator<? extends Entry<K, V>> spliterator) {
        return of(StreamSupport.stream(spliterator, false));
    }

    /**
     * Returns a sequential, ordered {@link EntryStream} created from given
     * {@link Iterator}.
     *
     * <p>
     * This method is roughly equivalent to
     * {@code EntryStream.of(Spliterators.spliteratorUnknownSize(iterator, ORDERED))}
     * , but may show better performance for parallel processing.
     * 
     * <p>
     * Use this method only if you cannot provide better Stream source (like
     * {@code Collection} or {@code Spliterator}).
     *
     * @param <K> the type of stream keys
     * @param <V> the type of stream values
     * @param iterator an iterator to create the stream from.
     * @return the new stream
     * @since 0.5.1
     */
    public static <K, V> EntryStream<K, V> of(Iterator<? extends Entry<K, V>> iterator) {
        return of(new UnknownSizeSpliterator.USOfRef<>(iterator));
    }

    /**
     * Returns an {@code EntryStream} object which contains the entries of
     * supplied {@code Map}.
     * 
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @param map the map to create the stream from
     * @return a new {@code EntryStream}
     */
    public static <K, V> EntryStream<K, V> of(Map<K, V> map) {
        if (map == null || map.size() == 0) {
            return empty();
        }

        return of(map.entrySet().stream());
    }

    /**
     * Returns an {@code EntryStream} object whose keys are indices of given
     * list and the values are the corresponding list elements.
     * 
     * <p>
     * The list elements are accessed using {@link List#get(int)}, so the list
     * should provide fast random access. The list is assumed to be unmodifiable
     * during the stream operations.
     * 
     * @param <V> list element type
     * @param list list to create the stream from
     * @return a new {@code EntryStream}
     * @since 0.2.3
     */
    public static <V> EntryStream<Integer, V> of(List<V> list) {
        if (list == null || list.size() == 0) {
            return empty();
        }

        return EntryStream.of(new RangeBasedSpliterator.AsEntry<>(list));
    }

    /**
     * Returns an {@code EntryStream} object whose keys are indices of given
     * array and the values are the corresponding array elements.
     * 
     * @param <V> array element type
     * @param array array to create the stream from
     * @return a new {@code EntryStream}
     * @since 0.2.3
     */
    public static <V> EntryStream<Integer, V> of(V[] array) {
        if (array == null || array.length == 0) {
            return empty();
        }

        return of(Arrays.asList(array));
    }

    public static <K, T> EntryStream<K, T> of(final T[] a, final Function<? super T, K> keyExtractor) {
        final Function<T, T> valueMapper = Fn.identity();

        return StreamEx.of(a).mapToEntry(keyExtractor, valueMapper);
    }

    public static <K, T> EntryStream<K, T> of(final Collection<? extends T> c,
            final Function<? super T, K> keyExtractor) {
        final Function<T, T> valueMapper = Fn.identity();

        return StreamEx.of(c).mapToEntry(keyExtractor, valueMapper);
    }

    /**
     * Returns a sequential {@code EntryStream} containing a single key-value
     * pair
     *
     * @param <K> the type of key
     * @param <V> the type of value
     * @param key the key of the single element
     * @param value the value of the single element
     * @return a singleton sequential stream
     */
    public static <K, V> EntryStream<K, V> of(K key, V value) {
        return of(Stream.of(new SimpleImmutableEntry<>(key, value)));
    }

    /**
     * Returns a sequential {@code EntryStream} containing two key-value pairs
     *
     * @param <K> the type of key
     * @param <V> the type of value
     * @param k1 the key of the first element
     * @param v1 the value of the first element
     * @param k2 the key of the second element
     * @param v2 the value of the second element
     * @return a sequential stream
     * @since 0.2.3
     */
    public static <K, V> EntryStream<K, V> of(K k1, V v1, K k2, V v2) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2)));
    }

    /**
     * Returns a sequential {@code EntryStream} containing three key-value pairs
     *
     * @param <K> the type of key
     * @param <V> the type of value
     * @param k1 the key of the first element
     * @param v1 the value of the first element
     * @param k2 the key of the second element
     * @param v2 the value of the second element
     * @param k3 the key of the third element
     * @param v3 the value of the third element
     * @return a sequential stream
     * @since 0.2.3
     */
    public static <K, V> EntryStream<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2),
            new SimpleImmutableEntry<>(k3, v3)));
    }

    /**
     * Returns a sequential {@code EntryStream} containing four key-value pairs
     *
     * @param <K> the type of key
     * @param <V> the type of value
     * @param k1 the key of the first element
     * @param v1 the value of the first element
     * @param k2 the key of the second element
     * @param v2 the value of the second element
     * @param k3 the key of the third element
     * @param v3 the value of the third element
     * @param k4 the key of the fourth element
     * @param v4 the value of the fourth element
     * @return a sequential stream
     * @since 0.5.2
     */
    public static <K, V> EntryStream<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2),
            new SimpleImmutableEntry<>(k3, v3), new SimpleImmutableEntry<>(k4, v4)));
    }

    /**
     * Returns a sequential {@code EntryStream} containing five key-value pairs
     *
     * @param <K> the type of key
     * @param <V> the type of value
     * @param k1 the key of the first element
     * @param v1 the value of the first element
     * @param k2 the key of the second element
     * @param v2 the value of the second element
     * @param k3 the key of the third element
     * @param v3 the value of the third element
     * @param k4 the key of the fourth element
     * @param v4 the value of the fourth element
     * @param k5 the key of the fifth element
     * @param v5 the value of the fifth element
     * @return a sequential stream
     * @since 0.5.2
     */
    public static <K, V> EntryStream<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2),
            new SimpleImmutableEntry<>(k3, v3), new SimpleImmutableEntry<>(k4, v4), new SimpleImmutableEntry<>(k5,
                    v5)));
    }

    /**
     * Returns a sequential {@code EntryStream} containing six key-value pairs
     *
     * @param <K> the type of key
     * @param <V> the type of value
     * @param k1 the key of the first element
     * @param v1 the value of the first element
     * @param k2 the key of the second element
     * @param v2 the value of the second element
     * @param k3 the key of the third element
     * @param v3 the value of the third element
     * @param k4 the key of the fourth element
     * @param v4 the value of the fourth element
     * @param k5 the key of the fifth element
     * @param v5 the value of the fifth element
     * @param k6 the key of the sixth element
     * @param v6 the value of the sixth element
     * @return a sequential stream
     * @since 0.5.2
     */
    public static <K, V> EntryStream<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2),
            new SimpleImmutableEntry<>(k3, v3), new SimpleImmutableEntry<>(k4, v4), new SimpleImmutableEntry<>(k5, v5),
            new SimpleImmutableEntry<>(k6, v6)));
    }

    /**
     * Returns a sequential {@code EntryStream} containing seven key-value pairs
     *
     * @param <K> the type of key
     * @param <V> the type of value
     * @param k1 the key of the first element
     * @param v1 the value of the first element
     * @param k2 the key of the second element
     * @param v2 the value of the second element
     * @param k3 the key of the third element
     * @param v3 the value of the third element
     * @param k4 the key of the fourth element
     * @param v4 the value of the fourth element
     * @param k5 the key of the fifth element
     * @param v5 the value of the fifth element
     * @param k6 the key of the sixth element
     * @param v6 the value of the sixth element
     * @param k7 the key of the seventh element
     * @param v7 the value of the seventh element
     * @return a sequential stream
     * @since 0.5.2
     */
    public static <K, V> EntryStream<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6,
            K k7, V v7) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2),
            new SimpleImmutableEntry<>(k3, v3), new SimpleImmutableEntry<>(k4, v4), new SimpleImmutableEntry<>(k5, v5),
            new SimpleImmutableEntry<>(k6, v6), new SimpleImmutableEntry<>(k7, v7)));
    }

    /**
     * Returns a sequential {@code EntryStream} containing eight key-value pairs
     *
     * @param <K> the type of key
     * @param <V> the type of value
     * @param k1 the key of the first element
     * @param v1 the value of the first element
     * @param k2 the key of the second element
     * @param v2 the value of the second element
     * @param k3 the key of the third element
     * @param v3 the value of the third element
     * @param k4 the key of the fourth element
     * @param v4 the value of the fourth element
     * @param k5 the key of the fifth element
     * @param v5 the value of the fifth element
     * @param k6 the key of the sixth element
     * @param v6 the value of the sixth element
     * @param k7 the key of the seventh element
     * @param v7 the value of the seventh element
     * @param k8 the key of the eighth element
     * @param v8 the value of the eighth element
     * @return a sequential stream
     * @since 0.5.2
     */
    public static <K, V> EntryStream<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6,
            K k7, V v7, K k8, V v8) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2),
            new SimpleImmutableEntry<>(k3, v3), new SimpleImmutableEntry<>(k4, v4), new SimpleImmutableEntry<>(k5, v5),
            new SimpleImmutableEntry<>(k6, v6), new SimpleImmutableEntry<>(k7, v7), new SimpleImmutableEntry<>(k8,
                    v8)));
    }

    /**
     * Returns a sequential {@code EntryStream} containing nine key-value pairs
     *
     * @param <K> the type of key
     * @param <V> the type of value
     * @param k1 the key of the first element
     * @param v1 the value of the first element
     * @param k2 the key of the second element
     * @param v2 the value of the second element
     * @param k3 the key of the third element
     * @param v3 the value of the third element
     * @param k4 the key of the fourth element
     * @param v4 the value of the fourth element
     * @param k5 the key of the fifth element
     * @param v5 the value of the fifth element
     * @param k6 the key of the sixth element
     * @param v6 the value of the sixth element
     * @param k7 the key of the seventh element
     * @param v7 the value of the seventh element
     * @param k8 the key of the eighth element
     * @param v8 the value of the eighth element
     * @param k9 the key of the ninth element
     * @param v9 the value of the ninth element
     * @return a sequential stream
     * @since 0.5.2
     */
    public static <K, V> EntryStream<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6,
            K k7, V v7, K k8, V v8, K k9, V v9) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2),
            new SimpleImmutableEntry<>(k3, v3), new SimpleImmutableEntry<>(k4, v4), new SimpleImmutableEntry<>(k5, v5),
            new SimpleImmutableEntry<>(k6, v6), new SimpleImmutableEntry<>(k7, v7), new SimpleImmutableEntry<>(k8, v8),
            new SimpleImmutableEntry<>(k9, v9)));
    }

    /**
     * Returns a sequential {@code EntryStream} containing ten key-value pairs
     *
     * @param <K> the type of key
     * @param <V> the type of value
     * @param k1 the key of the first element
     * @param v1 the value of the first element
     * @param k2 the key of the second element
     * @param v2 the value of the second element
     * @param k3 the key of the third element
     * @param v3 the value of the third element
     * @param k4 the key of the fourth element
     * @param v4 the value of the fourth element
     * @param k5 the key of the fifth element
     * @param v5 the value of the fifth element
     * @param k6 the key of the sixth element
     * @param v6 the value of the sixth element
     * @param k7 the key of the seventh element
     * @param v7 the value of the seventh element
     * @param k8 the key of the eighth element
     * @param v8 the value of the eighth element
     * @param k9 the key of the ninth element
     * @param v9 the value of the ninth element
     * @param k10 the key of the tenth element
     * @param v10 the value of the tenth element
     * @return a sequential stream
     * @since 0.5.2
     */
    public static <K, V> EntryStream<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6,
            K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2),
            new SimpleImmutableEntry<>(k3, v3), new SimpleImmutableEntry<>(k4, v4), new SimpleImmutableEntry<>(k5, v5),
            new SimpleImmutableEntry<>(k6, v6), new SimpleImmutableEntry<>(k7, v7), new SimpleImmutableEntry<>(k8, v8),
            new SimpleImmutableEntry<>(k9, v9), new SimpleImmutableEntry<>(k10, v10)));
    }

    /**
     * Returns a sequential {@code EntryStream} containing {@code Entry} objects
     * composed from corresponding key and value in given two lists. The size of
     * the returned {@code EntryStream} is
     * <code>Math.min(first.size(), second.size())</code>.
     *
     * <p>
     * The list values are accessed using {@link List#get(int)}, so the lists
     * should provide fast random access. The lists are assumed to be
     * unmodifiable during the stream operations.
     * 
     * @param <K> the type of stream element keys
     * @param <V> the type of stream element values
     * @param keys the list of keys, assumed to be unmodified during use
     * @param values the list of values, assumed to be unmodified during use
     * @return a new {@code EntryStream}
     * @see StreamEx#zip(List, List, BiFunction)
     * @since 0.2.1
     */
    public static <K, V> EntryStream<K, V> zip(List<K> keys, List<V> values) {
        if ((keys == null || keys.size() == 0) && (values == null || values.size() == 0)) {
            return EntryStream.empty();
        }

        return of(new RangeBasedSpliterator.ZipRef<>(0, Math.min(keys.size(), values.size()), SimpleImmutableEntry::new,
                keys, values));
    }

    /**
     * Returns a sequential {@code EntryStream} containing {@code Entry} objects
     * composed from corresponding key and value in given two arrays. The size
     * of the returned {@code EntryStream} is
     * <code>Math.min(first.length, second.length)</code>.
     * 
     * @param <K> the type of stream element keys
     * @param <V> the type of stream element values
     * @param keys the array of keys
     * @param values the array of values
     * @return a new {@code EntryStream}
     * @see StreamEx#zip(Object[], Object[], BiFunction)
     * @since 0.2.1
     */
    public static <K, V> EntryStream<K, V> zip(K[] keys, V[] values) {
        if ((keys == null || keys.length == 0) && (values == null || values.length == 0)) {
            return EntryStream.empty();
        }

        return zip(Arrays.asList(keys), Arrays.asList(values));
    }

    /**
     * Returns a sequential ordered {@code EntryStream} containing the possible
     * pairs of elements taken from the provided list.
     * 
     * <p>
     * Both keys and values are taken from the input list. The index of the key
     * is always strictly less than the index of the value. The pairs in the
     * stream are lexicographically ordered. For example, for the list of three
     * elements the stream of three elements is created:
     * {@code Map.Entry(list.get(0), list.get(1))},
     * {@code Map.Entry(list.get(0), list.get(2))} and
     * {@code Map.Entry(list.get(1), list.get(2))}. The number of elements in
     * the resulting stream is {@code list.size()*(list.size()+1L)/2}.
     * 
     * <p>
     * The list values are accessed using {@link List#get(int)}, so the list
     * should provide fast random access. The list is assumed to be unmodifiable
     * during the stream operations.
     *
     * @param <T> type of the list elements
     * @param list a list to take the elements from
     * @return a new {@code EntryStream}
     * @see StreamEx#ofPairs(List, BiFunction)
     * @since 0.3.6
     */
    public static <T> EntryStream<T, T> ofPairs(List<T> list) {
        if (list == null || list.size() == 0) {
            return empty();
        }

        return of(new PairPermutationSpliterator<>(list, SimpleImmutableEntry<T, T>::new));
    }

    /**
     * Returns a sequential ordered {@code EntryStream} containing the possible
     * pairs of elements taken from the provided array.
     * 
     * <p>
     * Both keys and values are taken from the input array. The index of the key
     * is always strictly less than the index of the value. The pairs in the
     * stream are lexicographically ordered. For example, for the array of three
     * elements the stream of three elements is created:
     * {@code Map.Entry(array[0], array[1])},
     * {@code Map.Entry(array[0], array[2])} and
     * {@code Map.Entry(array[1], array[2])}. The number of elements in the
     * resulting stream is {@code array.length*(array.length+1L)/2}..
     * 
     * @param <T> type of the array elements
     * @param array a array to take the elements from
     * @return a new {@code EntryStream}
     * @see StreamEx#ofPairs(Object[], BiFunction)
     * @since 0.3.6
     */
    public static <T> EntryStream<T, T> ofPairs(T[] array) {
        if (array == null || array.length == 0) {
            return empty();
        }

        return ofPairs(Arrays.asList(array));
    }

    /**
     * Return a new {@link EntryStream} containing all the nodes of tree-like
     * data structure in entry values along with the corresponding tree depths
     * in entry keys, in depth-first order.
     * 
     * <p>
     * The keys of the returned stream are non-negative integer numbers. 0 is
     * used for the root node only, 1 is for root immediate children, 2 is for
     * their children and so on.
     * 
     * <p>
     * The streams created by mapper may be automatically
     * {@link java.util.stream.BaseStream#close() closed} after its contents
     * already consumed and unnecessary anymore. It's not guaranteed that all
     * created streams will be closed during the stream terminal operation. If
     * it's necessary to close all the created streams, call the {@code close()}
     * method of the resulting stream returned by {@code ofTree()}.
     * 
     * @param <T> the type of tree nodes
     * @param root root node of the tree
     * @param mapper a non-interfering, stateless function to apply to each tree
     *        node and its depth which returns null for leaf nodes or stream of
     *        direct children for non-leaf nodes.
     * @return the new sequential ordered {@code EntryStream}
     * @since 0.5.2
     * @see StreamEx#ofTree(Object, Function)
     * @see #ofTree(Object, Class, BiFunction)
     */
    public static <T> EntryStream<Integer, T> ofTree(T root, BiFunction<Integer, T, Stream<T>> mapper) {
        TreeSpliterator<T, Entry<Integer, T>> spliterator = new TreeSpliterator.Depth<>(root, mapper);
        return new EntryStream<>(spliterator, StreamContext.SEQUENTIAL.onClose(spliterator::close));
    }

    /**
     * Return a new {@link EntryStream} containing all the nodes of tree-like
     * data structure in entry values along with the corresponding tree depths
     * in entry keys, in depth-first order.
     * 
     * <p>
     * The keys of the returned stream are non-negative integer numbers. 0 is
     * used for the root node only, 1 is for root immediate children, 2 is for
     * their children and so on.
     * 
     * <p>
     * The streams created by mapper may be automatically
     * {@link java.util.stream.BaseStream#close() closed} after its contents
     * already consumed and unnecessary anymore. It's not guaranteed that all
     * created streams will be closed during the stream terminal operation. If
     * it's necessary to close all the created streams, call the {@code close()}
     * method of the resulting stream returned by {@code ofTree()}.
     * 
     * @param <T> the base type of tree nodes
     * @param <TT> the sub-type of composite tree nodes which may have children
     * @param root root node of the tree
     * @param collectionClass a class representing the composite tree node
     * @param mapper a non-interfering, stateless function to apply to each
     *        composite tree node and its depth which returns stream of direct
     *        children. May return null if the given node has no children.
     * @return the new sequential ordered stream
     * @since 0.5.2
     * @see StreamEx#ofTree(Object, Class, Function)
     * @see #ofTree(Object, BiFunction)
     */
    @SuppressWarnings("unchecked")
    public static <T, TT extends T> EntryStream<Integer, T> ofTree(T root, Class<TT> collectionClass,
            BiFunction<Integer, TT, Stream<T>> mapper) {
        return ofTree(root, (d, t) -> collectionClass.isInstance(t) ? mapper.apply(d, (TT) t) : null);
    }

    public static <K, V> EntryStream<K, V> concat(Map<K, V> a, Map<K, V> b) {
        final EntryStream<K, V> s = of(a);

        return s.append(b);
    }

    public static <K, V> EntryStream<K, V> concat(Map<K, V> a, Map<K, V> b, Map<K, V> c) {
        final EntryStream<K, V> s = of(a);

        return s.append(b).append(c);
    }

    /**
     * Returns an infinite sequential unordered {@code EntryStream} where each
     * entry key is generated by the provided {@code keySupplier} and each entry
     * value is generated by the provided {@code valueSupplier}. This is
     * suitable for generating constant streams, streams of random elements,
     * etc.
     *
     * @param keySupplier the supplier to generate keys for the new stream
     * @param valueSupplier the supplier to generate values for the new stream
     * @param <K> the type of stream element keys
     * @param <V> the type of stream element values
     * @return a new infinite sequential unordered {@code EntryStream}
     * @since 0.6.6
     * @see StreamEx#generate(Supplier)
     */
    public static <K, V> EntryStream<K, V> generate(Supplier<? extends K> keySupplier,
            Supplier<? extends V> valueSupplier) {
        return new EntryStream<>(Stream.generate(() -> new SimpleImmutableEntry<>(keySupplier.get(), valueSupplier
                .get())), StreamContext.SEQUENTIAL);
    }
}
