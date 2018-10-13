/*
 * Copyright (c) 2017, Haiyang Li.
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;
import java.util.regex.Pattern;

/**
 * Factory utility class for functional interfaces.
 * 
 * <br>
 * Note: Don't save and reuse any Function/Predicat/Consumer/... created by calling the methods in this class.
 * The method should be called every time.
 * </br>
 * 
 * <pre>
 * <code>
 * 
 * Map<String, Integer> map = N.asMap("a", 1, "b", 2, "c", 3);
 * // Instead of
 * Stream.of(map).filter(e -> e.getKey().equals("a") || e.getKey().equals("b")).toMap(e -> e.getKey(), e -> e.getValue());
 * // Using Fn
 * Stream.of(map).filter(Fn.testByKey(k -> k.equals("a") || k.equals("b"))).collect(Collectors.toMap());
 * 
 * </code>
 * </pre>
 * 
 * 
 * 
 * @author haiyang li
 *
 */
public final class Fn extends Comparators {

    private static final Object NONE = new Object();

    @SuppressWarnings("rawtypes")
    public static final IntFunction<Map<String, Object>> FACTORY_OF_MAP = (IntFunction) Factory.MAP_FACTORY;
    @SuppressWarnings("rawtypes")
    public static final IntFunction<LinkedHashMap<String, Object>> FACTORY_OF_LINKED_HASH_MAP = (IntFunction) Factory.LINKED_HASH_MAP_FACTORY;
    @SuppressWarnings("rawtypes")
    public static final Supplier<Map<String, Object>> SUPPLIER_OF_MAP = (Supplier) Suppliers.MAP;
    @SuppressWarnings("rawtypes")
    public static final Supplier<LinkedHashMap<String, Object>> SUPPLIER_OF_LINKED_HASH_MAP = (Supplier) Suppliers.LINKED_HASH_MAP;

    private static final Runnable EMPTY_ACTION = new Runnable() {
        @Override
        public void run() {
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Consumer DO_NOTHING = new Consumer() {
        @Override
        public void accept(Object value) {
            // do nothing.
        }
    };

    private static final Consumer<AutoCloseable> CLOSE = new Consumer<AutoCloseable>() {
        @Override
        public void accept(AutoCloseable value) {
            MoreObjects.close(value);
        }
    };

    private static final Consumer<AutoCloseable> CLOSE_QUIETLY = new Consumer<AutoCloseable>() {
        @Override
        public void accept(AutoCloseable value) {
            MoreObjects.closeQuietly(value);
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Consumer PRINTLN = new Consumer() {
        @Override
        public void accept(Object value) {
            MoreObjects.println(value);
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Function TO_STRING = new Function() {
        @Override
        public Object apply(Object t) {
            return Objects.toString(t);
        }
    };

    @SuppressWarnings("rawtypes")
    private static final BiFunction COMPARE = new BiFunction<Comparable, Comparable, Integer>() {
        @Override
        public Integer apply(Comparable a, Comparable b) {
            return MoreObjects.compare(a, b);
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Function IDENTITY = Function.identity();

    private static final Function<String, String> TRIM = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return t == null ? null : t.trim();
        }
    };

    private static final Function<String, String> TRIM_TO_EMPTY = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return t == null ? "" : t.trim();
        }
    };

    private static final Function<String, String> TRIM_TO_NULL = new Function<String, String>() {
        @Override
        public String apply(String t) {
            if (t == null || (t = t.trim()).length() == 0) {
                return null;
            }

            return t;
        }
    };

    private static final Function<String, String> NULL_TO_EMPTY = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return t == null ? "" : t;
        }
    };

    private static final Function<String, Integer> LENGTH = new Function<String, Integer>() {
        @Override
        public Integer apply(String t) {
            return t == null ? 0 : t.length();
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Function<Collection, Integer> SIZE = new Function<Collection, Integer>() {
        @Override
        public Integer apply(Collection t) {
            return t == null ? 0 : t.size();
        }
    };

    private static final Function<Map.Entry<Object, Object>, Object> KEY = new Function<Map.Entry<Object, Object>, Object>() {
        @Override
        public Object apply(Map.Entry<Object, Object> t) {
            return t.getKey();
        }
    };

    private static final Function<Map.Entry<Object, Object>, Object> VALUE = new Function<Map.Entry<Object, Object>, Object>() {
        @Override
        public Object apply(Map.Entry<Object, Object> t) {
            return t.getValue();
        }
    };

    private static final Function<Map.Entry<Object, Object>, Map.Entry<Object, Object>> INVERSE = new Function<Map.Entry<Object, Object>, Map.Entry<Object, Object>>() {
        @Override
        public Map.Entry<Object, Object> apply(Map.Entry<Object, Object> t) {
            return new SimpleImmutableEntry<>(t.getValue(), t.getKey());
        }
    };

    private static final BiFunction<Object, Object, Map.Entry<Object, Object>> ENTRY = new BiFunction<Object, Object, Map.Entry<Object, Object>>() {
        @Override
        public Map.Entry<Object, Object> apply(Object key, Object value) {
            return new SimpleImmutableEntry<>(key, value);
        }
    };

    private static final BiFunction<Object, Object, Pair<Object, Object>> PAIR = new BiFunction<Object, Object, Pair<Object, Object>>() {
        @Override
        public Pair<Object, Object> apply(Object key, Object value) {
            return Pair.of(key, value);
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Predicate ALWAYS_TRUE = new Predicate() {
        @Override
        public boolean test(Object value) {
            return true;
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Predicate ALWAYS_FALSE = new Predicate() {
        @Override
        public boolean test(Object value) {
            return false;
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Predicate IS_NULL = new Predicate() {
        @Override
        public boolean test(Object value) {
            return value == null;
        }
    };

    private static final Predicate<CharSequence> IS_NULL_OR_EMPTY = new Predicate<CharSequence>() {
        @Override
        public boolean test(CharSequence value) {
            return value == null || value.length() == 0;
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Predicate NOT_NULL = new Predicate() {
        @Override
        public boolean test(Object value) {
            return value != null;
        }
    };

    private static final Predicate<CharSequence> NOT_NULL_OR_EMPTY = new Predicate<CharSequence>() {
        @Override
        public boolean test(CharSequence value) {
            return value != null && value.length() > 0;
        }
    };

    private static final ToIntFunction<Number> NUM_TO_INT_FUNC = new ToIntFunction<Number>() {
        @Override
        public int applyAsInt(Number value) {
            return value == null ? 0 : value.intValue();
        }
    };

    private static final ToLongFunction<Number> NUM_TO_LONG_FUNC = new ToLongFunction<Number>() {
        @Override
        public long applyAsLong(Number value) {
            return value == null ? 0 : value.longValue();
        }
    };

    private static final ToDoubleFunction<Number> NUM_TO_DOUBLE_FUNC = new ToDoubleFunction<Number>() {
        @Override
        public double applyAsDouble(Number value) {
            return value == null ? 0d : value.doubleValue();
        }
    };
 
    private Fn() {
        super();
        // Singleton.
    }

    public static <T> T get(final Supplier<T> supplier) {
        return supplier.get();
    }

    /**
     * Returns a {@code Supplier} which returns a single instance created by
     * calling the specified {@code supplier.get()}.
     * 
     * @param supplier
     * @return
     */
    public static <T> Supplier<T> single(final Supplier<T> supplier) {
        return new Supplier<T>() {
            private T instance = (T) NONE;

            @Override
            public T get() {
                synchronized (this) {
                    if (instance == NONE) {
                        instance = supplier.get();
                    }

                    return instance;
                }
            }
        };
    }

    public static Runnable close(final AutoCloseable closeable) {
        return new Runnable() {
            private volatile boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                MoreObjects.close(closeable);
            }
        };
    }

    @SafeVarargs
    public static Runnable closeAll(final AutoCloseable... a) {
        return new Runnable() {
            private volatile boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                MoreObjects.closeAll(a);
            }
        };
    }

    public static Runnable closeQuietly(final AutoCloseable closeable) {
        return new Runnable() {
            private volatile boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                MoreObjects.closeQuietly(closeable);
            }
        };
    }

    @SafeVarargs
    public static Runnable closeAllQuietly(final AutoCloseable... a) {
        return new Runnable() {
            private volatile boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                MoreObjects.closeAllQuietly(a);
            }
        };
    }

    public static Runnable emptyAction() {
        return EMPTY_ACTION;
    }

    public static <T> Consumer<T> doNothing() {
        return DO_NOTHING;
    }

    public static <T extends AutoCloseable> Consumer<T> close() {
        return (Consumer<T>) CLOSE;
    }

    public static <T extends AutoCloseable> Consumer<T> closeQuietly() {
        return (Consumer<T>) CLOSE_QUIETLY;
    }

    public static <T> Consumer<T> println() {
        return PRINTLN;
    }

    public static <T> Function<T, String> toStr() {
        return TO_STRING;
    }

    public static <T> Function<T, T> identity() {
        return IDENTITY;
    }

    public static <K, T> Function<T, Keyed<K, T>> keyed(final Function<? super T, K> keyExtractor) {
        Objects.requireNonNull(keyExtractor);

        return new Function<T, Keyed<K, T>>() {
            @Override
            public Keyed<K, T> apply(T t) {
                return Keyed.of(keyExtractor.apply(t), t);
            }
        };
    }

    private static final Function<Keyed<?, Object>, Object> VAL = new Function<Keyed<?, Object>, Object>() {
        @Override
        public Object apply(Keyed<?, Object> t) {
            return t.val();
        }
    };

    @SuppressWarnings("rawtypes")
    public static <K, T> Function<Keyed<K, T>, T> val() {
        return (Function) VAL;
    }

    private static final Function<Map.Entry<Keyed<Object, Object>, Object>, Object> KK = new Function<Map.Entry<Keyed<Object, Object>, Object>, Object>() {
        @Override
        public Object apply(Map.Entry<Keyed<Object, Object>, Object> t) {
            return t.getKey().val();
        }
    };

    @SuppressWarnings("rawtypes")
    public static <T, K, V> Function<Map.Entry<Keyed<K, T>, Long>, T> kk() {
        return (Function) KK;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Entry<K, V>, K> key() {
        return (Function) KEY;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Entry<K, V>, V> value() {
        return (Function) VALUE;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Entry<K, V>, Entry<V, K>> inverse() {
        return (Function) INVERSE;
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> BiFunction<K, V, Map.Entry<K, V>> entry() {
        return (BiFunction) ENTRY;
    }

    public static <K, T> Function<T, Map.Entry<K, T>> entry(final K key) {
        return new Function<T, Map.Entry<K, T>>() {
            @Override
            public Entry<K, T> apply(T t) {
                return new SimpleImmutableEntry<>(key, t);
            }
        };
    }

    public static <K, T> Function<T, Map.Entry<K, T>> entry(final Function<? super T, K> keyExtractor) {
        Objects.requireNonNull(keyExtractor);

        return new Function<T, Map.Entry<K, T>>() {
            @Override
            public Entry<K, T> apply(T t) {
                return new SimpleImmutableEntry<>(keyExtractor.apply(t), t);
            }
        };
    }

    @SuppressWarnings("rawtypes")
    public static <L, R> BiFunction<L, R, Pair<L, R>> pair() {
        return (BiFunction) PAIR;
    }

    public static Function<String, String> trim() {
        return TRIM;
    }

    public static Function<String, String> trimToEmpty() {
        return TRIM_TO_EMPTY;
    }

    public static Function<String, String> trimToNull() {
        return TRIM_TO_NULL;
    }

    public static Function<String, String> nullToEmpty() {
        return NULL_TO_EMPTY;
    }

    public static Function<String, Integer> length() {
        return LENGTH;
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Collection> Function<T, Integer> size() {
        return (Function<T, Integer>) SIZE;
    }

    public static <T, U> Function<T, U> cast(final Class<U> clazz) {
        Objects.requireNonNull(clazz);

        return new Function<T, U>() {
            @Override
            public U apply(T t) {
                return (U) t;
            }
        };
    }

    public static <T> Predicate<T> alwaysTrue() {
        return ALWAYS_TRUE;
    }

    public static <T> Predicate<T> alwaysFalse() {
        return ALWAYS_FALSE;
    }

    public static <T> Predicate<T> isNull() {
        return IS_NULL;
    }

    public static <T extends CharSequence> Predicate<T> isNullOrEmpty() {
        return (Predicate<T>) IS_NULL_OR_EMPTY;
    }

    public static <T> Predicate<T> notNull() {
        return NOT_NULL;
    }

    public static <T extends CharSequence> Predicate<T> notNullOrEmpty() {
        return (Predicate<T>) NOT_NULL_OR_EMPTY;
    }

    public static <T> Predicate<T> equal(final Object target) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return Objects.equals(value, target);
            }
        };
    }

    public static <T> Predicate<T> notEqual(final Object target) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return !Objects.equals(value, target);
            }
        };
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Predicate<T> greaterThan(final T target) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return MoreObjects.compare(value, target) > 0;
            }
        };
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Predicate<T> greaterEqual(final T target) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return MoreObjects.compare(value, target) >= 0;
            }
        };
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Predicate<T> lessThan(final T target) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return MoreObjects.compare(value, target) < 0;
            }
        };
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Predicate<T> lessEqual(final T target) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return MoreObjects.compare(value, target) <= 0;
            }
        };
    }

    /**
     * Checks if the value/element: {@code minValue < e < maxValue}.
     * 
     * @param minValue
     * @param maxValue
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Predicate<T> between(final T minValue, final T maxValue) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return MoreObjects.compare(value, minValue) > 0 && MoreObjects.compare(value, maxValue) < 0;
            }
        };
    }

    public static <T> Predicate<T> in(final Collection<?> c) {
        Objects.requireNonNull(c);

        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return c.contains(value);
            }
        };
    }

    public static <T> Predicate<T> notIn(final Collection<?> c) {
        Objects.requireNonNull(c);

        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return !c.contains(value);
            }
        };
    }

    public static <T> Predicate<T> instanceOf(final Class<?> clazz) {
        Objects.requireNonNull(clazz);

        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return clazz.isInstance(value);
            }
        };
    }

    @SuppressWarnings("rawtypes")
    public static Predicate<Class> subtypeOf(final Class<?> clazz) {
        Objects.requireNonNull(clazz);

        return new Predicate<Class>() {
            @Override
            public boolean test(Class value) {
                return clazz.isAssignableFrom(value);
            }
        };
    }

    public static Predicate<String> startsWith(final String prefix) {
        Objects.requireNonNull(prefix);

        return new Predicate<String>() {
            @Override
            public boolean test(String value) {
                return value.startsWith(prefix);
            }
        };
    }

    public static Predicate<String> endsWith(final String suffix) {
        Objects.requireNonNull(suffix);

        return new Predicate<String>() {
            @Override
            public boolean test(String value) {
                return value.endsWith(suffix);
            }
        };
    }

    public static Predicate<String> contains(final String str) {
        Objects.requireNonNull(str);

        return new Predicate<String>() {
            @Override
            public boolean test(String value) {
                return value.contains(str);
            }
        };
    }

    public static Predicate<CharSequence> matches(final Pattern pattern) {
        Objects.requireNonNull(pattern);

        return new Predicate<CharSequence>() {
            @Override
            public boolean test(CharSequence value) {
                return pattern.matcher(value).find();
            }
        };
    }

    public static <T, U> BiPredicate<T, U> equal() {
        return BiPredicates.EQUAL;
    }

    public static <T, U> BiPredicate<T, U> notEqual() {
        return BiPredicates.NOT_EQUAL;
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> BiPredicate<T, T> greaterThan() {
        return (BiPredicate<T, T>) BiPredicates.GREATER_THAN;
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> BiPredicate<T, T> greaterEqual() {
        return (BiPredicate<T, T>) BiPredicates.GREATER_EQUAL;
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> BiPredicate<T, T> lessThan() {
        return (BiPredicate<T, T>) BiPredicates.LESS_THAN;
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> BiPredicate<T, T> lessEqual() {
        return (BiPredicate<T, T>) BiPredicates.LESS_EQUAL;
    }

    public static <T> Predicate<T> not(final Predicate<T> predicate) {
        Objects.requireNonNull(predicate);

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return !predicate.test(t);
            }
        };
    }

    public static <T, U> BiPredicate<T, U> not(final BiPredicate<T, U> biPredicate) {
        Objects.requireNonNull(biPredicate);

        return new BiPredicate<T, U>() {
            @Override
            public boolean test(T t, U u) {
                return !biPredicate.test(t, u);
            }
        };
    }

    public static BooleanSupplier and(final BooleanSupplier first, final BooleanSupplier second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);

        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return first.getAsBoolean() && second.getAsBoolean();
            }
        };
    }

    public static BooleanSupplier and(final BooleanSupplier first, final BooleanSupplier second,
            final BooleanSupplier third) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(third);

        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return first.getAsBoolean() && second.getAsBoolean() && third.getAsBoolean();
            }
        };
    }

    public static <T> Predicate<T> and(final Predicate<? super T> first, final Predicate<? super T> second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return first.test(t) && second.test(t);
            }
        };
    }

    public static <T> Predicate<T> and(final Predicate<? super T> first, final Predicate<? super T> second,
            final Predicate<? super T> third) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(third);

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return first.test(t) && second.test(t) && third.test(t);
            }
        };
    }

    public static <T> Predicate<T> and(final Collection<Predicate<? super T>> c) {
        if (c == null || c.size() == 0) {
            throw new IllegalArgumentException();
        }

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                for (Predicate<? super T> p : c) {
                    if (p.test(t) == false) {
                        return false;
                    }
                }

                return true;
            }
        };
    }

    public static <T, U> BiPredicate<T, U> and(final BiPredicate<? super T, ? super U> first,
            final BiPredicate<? super T, ? super U> second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);

        return new BiPredicate<T, U>() {
            @Override
            public boolean test(T t, U u) {
                return first.test(t, u) && second.test(t, u);
            }
        };
    }

    public static <T, U> BiPredicate<T, U> and(final BiPredicate<? super T, ? super U> first,
            final BiPredicate<? super T, ? super U> second, final BiPredicate<? super T, ? super U> third) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(third);

        return new BiPredicate<T, U>() {
            @Override
            public boolean test(T t, U u) {
                return first.test(t, u) && second.test(t, u) && third.test(t, u);
            }
        };
    }

    public static <T, U> BiPredicate<T, U> and(final List<BiPredicate<? super T, ? super U>> c) {
        if (c == null || c.size() == 0) {
            throw new IllegalArgumentException();
        }

        return new BiPredicate<T, U>() {
            @Override
            public boolean test(T t, U u) {
                for (BiPredicate<? super T, ? super U> p : c) {
                    if (p.test(t, u) == false) {
                        return false;
                    }
                }

                return true;
            }
        };
    }

    public static BooleanSupplier or(final BooleanSupplier first, final BooleanSupplier second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);

        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return first.getAsBoolean() || second.getAsBoolean();
            }
        };
    }

    public static BooleanSupplier or(final BooleanSupplier first, final BooleanSupplier second,
            final BooleanSupplier third) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(third);

        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return first.getAsBoolean() || second.getAsBoolean() || third.getAsBoolean();
            }
        };
    }

    public static <T> Predicate<T> or(final Predicate<? super T> first, final Predicate<? super T> second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return first.test(t) || second.test(t);
            }
        };
    }

    public static <T> Predicate<T> or(final Predicate<? super T> first, final Predicate<? super T> second,
            final Predicate<? super T> third) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(third);

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return first.test(t) || second.test(t) || third.test(t);
            }
        };
    }

    public static <T> Predicate<T> or(final Collection<Predicate<? super T>> c) {
        if (c == null || c.size() == 0) {
            throw new IllegalArgumentException();
        }

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                for (Predicate<? super T> p : c) {
                    if (p.test(t)) {
                        return true;
                    }
                }

                return false;
            }
        };
    }

    public static <T, U> BiPredicate<T, U> or(final BiPredicate<? super T, ? super U> first,
            final BiPredicate<? super T, ? super U> second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);

        return new BiPredicate<T, U>() {
            @Override
            public boolean test(T t, U u) {
                return first.test(t, u) || second.test(t, u);
            }
        };
    }

    public static <T, U> BiPredicate<T, U> or(final BiPredicate<? super T, ? super U> first,
            final BiPredicate<? super T, ? super U> second, final BiPredicate<? super T, ? super U> third) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(third);

        return new BiPredicate<T, U>() {
            @Override
            public boolean test(T t, U u) {
                return first.test(t, u) || second.test(t, u) || third.test(t, u);
            }
        };
    }

    public static <T, U> BiPredicate<T, U> or(final List<BiPredicate<? super T, ? super U>> c) {
        if (c == null || c.size() == 0) {
            throw new IllegalArgumentException();
        }

        return new BiPredicate<T, U>() {
            @Override
            public boolean test(T t, U u) {
                for (BiPredicate<? super T, ? super U> p : c) {
                    if (p.test(t, u)) {
                        return true;
                    }
                }

                return false;
            }
        };
    }

    public static <K, V> Predicate<Map.Entry<K, V>> testByKey(final Predicate<? super K> predicate) {
        Objects.requireNonNull(predicate);

        return new Predicate<Map.Entry<K, V>>() {
            @Override
            public boolean test(Entry<K, V> entry) {
                return predicate.test(entry.getKey());
            }
        };
    }

    public static <K, V> Predicate<Map.Entry<K, V>> testByValue(final Predicate<? super V> predicate) {
        Objects.requireNonNull(predicate);

        return new Predicate<Map.Entry<K, V>>() {
            @Override
            public boolean test(Entry<K, V> entry) {
                return predicate.test(entry.getValue());
            }
        };
    }

    public static <K, V> Consumer<Map.Entry<K, V>> acceptByKey(final Consumer<? super K> consumer) {
        Objects.requireNonNull(consumer);

        return new Consumer<Map.Entry<K, V>>() {
            @Override
            public void accept(Entry<K, V> entry) {
                consumer.accept(entry.getKey());
            }
        };
    }

    public static <K, V> Consumer<Map.Entry<K, V>> acceptByValue(final Consumer<? super V> consumer) {
        Objects.requireNonNull(consumer);

        return new Consumer<Map.Entry<K, V>>() {
            @Override
            public void accept(Entry<K, V> entry) {
                consumer.accept(entry.getValue());
            }
        };
    }

    public static <K, V, R> Function<Map.Entry<K, V>, R> applyByKey(final Function<? super K, R> func) {
        Objects.requireNonNull(func);

        return new Function<Map.Entry<K, V>, R>() {
            @Override
            public R apply(Entry<K, V> entry) {
                return func.apply(entry.getKey());
            }
        };
    }

    public static <K, V, R> Function<Map.Entry<K, V>, R> applyByValue(final Function<? super V, R> func) {
        Objects.requireNonNull(func);

        return new Function<Map.Entry<K, V>, R>() {
            @Override
            public R apply(Entry<K, V> entry) {
                return func.apply(entry.getValue());
            }
        };
    }

    public static <K, V, KK> Function<Map.Entry<K, V>, Map.Entry<KK, V>> mapKey(final Function<? super K, KK> func) {
        Objects.requireNonNull(func);

        return new Function<Map.Entry<K, V>, Map.Entry<KK, V>>() {
            @Override
            public Map.Entry<KK, V> apply(Entry<K, V> entry) {
                return new SimpleImmutableEntry<>(func.apply(entry.getKey()), entry.getValue());
            }
        };
    }

    public static <K, V, VV> Function<Map.Entry<K, V>, Map.Entry<K, VV>> mapValue(final Function<? super V, VV> func) {
        Objects.requireNonNull(func);

        return new Function<Map.Entry<K, V>, Map.Entry<K, VV>>() {
            @Override
            public Map.Entry<K, VV> apply(Entry<K, V> entry) {
                return new SimpleImmutableEntry<>(entry.getKey(), func.apply(entry.getValue()));
            }
        };
    }

    public static <K, V> Predicate<Map.Entry<K, V>> testKeyVal(final BiPredicate<? super K, ? super V> predicate) {
        Objects.requireNonNull(predicate);

        return new Predicate<Map.Entry<K, V>>() {
            @Override
            public boolean test(Entry<K, V> entry) {
                return predicate.test(entry.getKey(), entry.getValue());
            }
        };
    }

    public static <K, V> Consumer<Map.Entry<K, V>> acceptKeyVal(final BiConsumer<? super K, ? super V> consumer) {
        Objects.requireNonNull(consumer);

        return new Consumer<Map.Entry<K, V>>() {
            @Override
            public void accept(Entry<K, V> entry) {
                consumer.accept(entry.getKey(), entry.getValue());
            }
        };
    }

    public static <K, V, R> Function<Map.Entry<K, V>, R> applyKeyVal(final BiFunction<? super K, ? super V, R> func) {
        Objects.requireNonNull(func);

        return new Function<Map.Entry<K, V>, R>() {
            @Override
            public R apply(Entry<K, V> entry) {
                return func.apply(entry.getKey(), entry.getValue());
            }
        };
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Number> ToIntFunction<T> numToInt() {
        return (ToIntFunction) NUM_TO_INT_FUNC;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Number> ToLongFunction<T> numToLong() {
        return (ToLongFunction) NUM_TO_LONG_FUNC;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Number> ToDoubleFunction<T> numToDouble() {
        return (ToDoubleFunction) NUM_TO_DOUBLE_FUNC;
    }

    /**
     * 
     * @param limit
     * @param predicate
     * @return
     */
    public static <T> Predicate<T> limitThenFilter(final int limit, final Predicate<T> predicate) {
        Objects.requireNonNull(predicate);

        return new Predicate<T>() {
            private final AtomicInteger counter = new AtomicInteger(limit);

            @Override
            public boolean test(T t) {
                return counter.getAndDecrement() > 0 && predicate.test(t);
            }
        };
    }

    public static <T, U> BiPredicate<T, U> limitThenFilter(final int limit, final BiPredicate<T, U> predicate) {
        Objects.requireNonNull(predicate);

        return new BiPredicate<T, U>() {
            private final AtomicInteger counter = new AtomicInteger(limit);

            @Override
            public boolean test(T t, U u) {
                return counter.getAndDecrement() > 0 && predicate.test(t, u);
            }
        };
    }

    public static <T> Predicate<T> filterThenLimit(final Predicate<T> predicate, final int limit) {
        Objects.requireNonNull(predicate);

        return new Predicate<T>() {
            private final AtomicInteger counter = new AtomicInteger(limit);

            @Override
            public boolean test(T t) {
                return predicate.test(t) && counter.getAndDecrement() > 0;
            }
        };
    }

    public static <T, U> BiPredicate<T, U> filterThenLimit(final BiPredicate<T, U> predicate, final int limit) {
        Objects.requireNonNull(predicate);

        return new BiPredicate<T, U>() {
            private final AtomicInteger counter = new AtomicInteger(limit);

            @Override
            public boolean test(T t, U u) {
                return predicate.test(t, u) && counter.getAndDecrement() > 0;
            }
        };
    }

    /**
     * Returns a stateful <code>Function</code> which should not be used in
     * parallel stream.
     * 
     * @return
     */
    public static <T> Function<T, Indexed<T>> indexed() {
        return new Function<T, Indexed<T>>() {
            private final AtomicLong idx = new AtomicLong(0);

            @Override
            public Indexed<T> apply(T t) {
                return Indexed.of(t, idx.getAndIncrement());
            }
        };
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Function<T, Integer> compareTo(final T target) {
        return new Function<T, Integer>() {
            @Override
            public Integer apply(T t) {
                return MoreObjects.compare(t, target);
            }
        };
    }

    @SuppressWarnings("rawtypes")
    public static <T> Function<T, Integer> compareTo(final T target, final Comparator<? super T> cmp) {
        // Objects.requireNonNull(cmp);

        if (cmp == null || cmp == Comparators.naturalOrder()) {
            return (Function) compareTo((Comparable) target);
        }

        return new Function<T, Integer>() {
            @Override
            public Integer apply(T t) {
                return MoreObjects.compare(t, target, cmp);
            }
        };
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> BiFunction<T, T, Integer> compare() {
        return COMPARE;
    }

    public static <T> BiFunction<T, T, Integer> compare(final Comparator<? super T> cmp) {
        // Objects.requireNonNull(cmp);

        if (cmp == null || cmp == Comparators.naturalOrder()) {
            return COMPARE;
        }

        return new BiFunction<T, T, Integer>() {
            @Override
            public Integer apply(T a, T b) {
                return MoreObjects.compare(a, b, cmp);
            }
        };
    }

    public static <T> Predicate<T> p(final Predicate<T> predicate) {
        return predicate;
    }

    public static <U, T> Predicate<T> p(final U u, final BiPredicate<T, U> biPredicate) {
        return Predicates.create(u, biPredicate);
    }

    public static <T, U> BiPredicate<T, U> p(final BiPredicate<T, U> biPredicate) {
        return biPredicate;
    }

    public static <T> Consumer<T> c(final Consumer<T> consumer) {
        return consumer;
    }

    public static <U, T> Consumer<T> c(final U u, final BiConsumer<T, U> biConsumer) {
        return Consumers.create(u, biConsumer);
    }

    public static <T, U> BiConsumer<T, U> c(final BiConsumer<T, U> biConsumer) {
        return biConsumer;
    }

    public static <T, R> Function<T, R> f(final Function<T, R> function) {
        return function;
    }

    public static <U, T, R> Function<T, R> f(final U u, final BiFunction<T, U, R> biFunction) {
        return Functions.create(u, biFunction);
    }

    public static <T, U, R> BiFunction<T, U, R> f(final BiFunction<T, U, R> biFunction) {
        return biFunction;
    }

    public static <T> BinaryOperator<T> throwingMerger() {
        return BinaryOperators.THROWING_MERGER;
    }

    public static <T> BinaryOperator<T> ignoringMerger() {
        return BinaryOperators.IGNORING_MERGER;
    }

    public static <T> BinaryOperator<T> replacingMerger() {
        return BinaryOperators.REPLACING_MERGER;
    }

    public static class Factory {
        private static final IntFunction<boolean[]> BOOLEAN_ARRAY = new IntFunction<boolean[]>() {
            @Override
            public boolean[] apply(int len) {
                return new boolean[len];
            }
        };

        private static final IntFunction<char[]> CHAR_ARRAY = new IntFunction<char[]>() {
            @Override
            public char[] apply(int len) {
                return new char[len];
            }
        };

        private static final IntFunction<byte[]> BYTE_ARRAY = new IntFunction<byte[]>() {
            @Override
            public byte[] apply(int len) {
                return new byte[len];
            }
        };

        private static final IntFunction<short[]> SHORT_ARRAY = new IntFunction<short[]>() {
            @Override
            public short[] apply(int len) {
                return new short[len];
            }
        };

        private static final IntFunction<int[]> INT_ARRAY = new IntFunction<int[]>() {
            @Override
            public int[] apply(int len) {
                return new int[len];
            }
        };

        private static final IntFunction<long[]> LONG_ARRAY = new IntFunction<long[]>() {
            @Override
            public long[] apply(int len) {
                return new long[len];
            }
        };

        private static final IntFunction<float[]> FLOAT_ARRAY = new IntFunction<float[]>() {
            @Override
            public float[] apply(int len) {
                return new float[len];
            }
        };

        private static final IntFunction<double[]> DOUBLE_ARRAY = new IntFunction<double[]>() {
            @Override
            public double[] apply(int len) {
                return new double[len];
            }
        };

        private static final IntFunction<String[]> STRING_ARRAY = new IntFunction<String[]>() {
            @Override
            public String[] apply(int len) {
                return new String[len];
            }
        };

        private static final IntFunction<Object[]> OBJECT_ARRAY = new IntFunction<Object[]>() {
            @Override
            public Object[] apply(int len) {
                return new Object[len];
            }
        };

        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super List> LIST_FACTORY = new IntFunction<List>() {
            @Override
            public List apply(int len) {
                return new ArrayList<>(len);
            }
        };

        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super LinkedList> LINKED_LIST_FACTORY = new IntFunction<LinkedList>() {
            @Override
            public LinkedList apply(int len) {
                return new LinkedList<>();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super Set> SET_FACTORY = new IntFunction<Set>() {
            @Override
            public Set apply(int len) {
                return new HashSet<>(len);
            }
        };

        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super LinkedHashSet> LINKED_HASH_SET_FACTORY = new IntFunction<LinkedHashSet>() {
            @Override
            public LinkedHashSet apply(int len) {
                return new LinkedHashSet<>(len);
            }
        };

        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super TreeSet> TREE_SET_FACTORY = new IntFunction<TreeSet>() {
            @Override
            public TreeSet apply(int len) {
                return new TreeSet<>();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super Queue> QUEUE_FACTORY = new IntFunction<Queue>() {
            @Override
            public Queue apply(int len) {
                return new LinkedList();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super Deque> DEQUE_FACTORY = new IntFunction<Deque>() {
            @Override
            public Deque apply(int len) {
                return new LinkedList();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super ArrayDeque> ARRAY_DEQUE_FACTORY = new IntFunction<ArrayDeque>() {
            @Override
            public ArrayDeque apply(int len) {
                return new ArrayDeque(len);
            }
        };

        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super LinkedBlockingQueue> LINKED_BLOCKING_QUEUE_FACTORY = new IntFunction<LinkedBlockingQueue>() {
            @Override
            public LinkedBlockingQueue apply(int len) {
                return new LinkedBlockingQueue(len);
            }
        };

        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super ConcurrentLinkedQueue> CONCURRENT_LINKED_QUEUE_FACTORY = new IntFunction<ConcurrentLinkedQueue>() {
            @Override
            public ConcurrentLinkedQueue apply(int len) {
                return new ConcurrentLinkedQueue();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super PriorityQueue> PRIORITY_QUEUE_FACTORY = new IntFunction<PriorityQueue>() {
            @Override
            public PriorityQueue apply(int len) {
                return new PriorityQueue(len);
            }
        };

        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super Map> MAP_FACTORY = new IntFunction<Map>() {
            @Override
            public Map apply(int len) {
                return new HashMap<>(len);
            }
        };

        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super LinkedHashMap> LINKED_HASH_MAP_FACTORY = new IntFunction<LinkedHashMap>() {
            @Override
            public LinkedHashMap apply(int len) {
                return new LinkedHashMap<>(len);
            }
        };

        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super IdentityHashMap> IDENTITY_HASH_MAP_FACTORY = new IntFunction<IdentityHashMap>() {
            @Override
            public IdentityHashMap apply(int len) {
                return new IdentityHashMap<>(len);
            }
        };

        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super TreeMap> TREE_MAP_FACTORY = new IntFunction<TreeMap>() {
            @Override
            public TreeMap apply(int len) {
                return new TreeMap<>();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super ConcurrentHashMap> CONCURRENT_HASH_MAP_FACTORY = new IntFunction<ConcurrentHashMap>() {
            @Override
            public ConcurrentHashMap apply(int len) {
                return new ConcurrentHashMap(len);
            }
        };

        private Factory() {
            // singleton.
        }

        public static IntFunction<boolean[]> ofBooleanArray() {
            return BOOLEAN_ARRAY;
        }

        public static IntFunction<char[]> ofCharArray() {
            return CHAR_ARRAY;
        }

        public static IntFunction<byte[]> ofByteArray() {
            return BYTE_ARRAY;
        }

        public static IntFunction<short[]> ofShortArray() {
            return SHORT_ARRAY;
        }

        public static IntFunction<int[]> ofIntArray() {
            return INT_ARRAY;
        }

        public static IntFunction<long[]> ofLongArray() {
            return LONG_ARRAY;
        }

        public static IntFunction<float[]> ofFloatArray() {
            return FLOAT_ARRAY;
        }

        public static IntFunction<double[]> ofDoubleArray() {
            return DOUBLE_ARRAY;
        }

        public static IntFunction<String[]> ofStringArray() {
            return STRING_ARRAY;
        }

        public static IntFunction<Object[]> ofObjectArray() {
            return OBJECT_ARRAY;
        }

        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<List<T>> ofList() {
            return (IntFunction) LIST_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<LinkedList<T>> ofLinkedList() {
            return (IntFunction) LINKED_LIST_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Set<T>> ofSet() {
            return (IntFunction) SET_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<LinkedHashSet<T>> ofLinkedHashSet() {
            return (IntFunction) LINKED_HASH_SET_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<SortedSet<T>> ofSortedSet() {
            return (IntFunction) TREE_SET_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<NavigableSet<T>> ofNavigableSet() {
            return (IntFunction) TREE_SET_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<TreeSet<T>> ofTreeSet() {
            return (IntFunction) TREE_SET_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Queue<T>> ofQueue() {
            return (IntFunction) QUEUE_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Deque<T>> ofDeque() {
            return (IntFunction) DEQUE_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<ArrayDeque<T>> ofArrayDeque() {
            return (IntFunction) ARRAY_DEQUE_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<LinkedBlockingQueue<T>> ofLinkedBlockingQueue() {
            return (IntFunction) LINKED_BLOCKING_QUEUE_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<ConcurrentLinkedQueue<T>> ofConcurrentLinkedQueue() {
            return (IntFunction) CONCURRENT_LINKED_QUEUE_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<PriorityQueue<T>> ofPriorityQueue() {
            return (IntFunction) PRIORITY_QUEUE_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<Map<K, V>> ofMap() {
            return (IntFunction) MAP_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<LinkedHashMap<K, V>> ofLinkedHashMap() {
            return (IntFunction) LINKED_HASH_MAP_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<IdentityHashMap<K, V>> ofIdentityHashMap() {
            return (IntFunction) IDENTITY_HASH_MAP_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<SortedMap<K, V>> ofSortedMap() {
            return (IntFunction) TREE_MAP_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<NavigableMap<K, V>> ofNavigableMap() {
            return (IntFunction) TREE_MAP_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<TreeMap<K, V>> ofTreeMap() {
            return (IntFunction) TREE_MAP_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<ConcurrentMap<K, V>> ofConcurrentMap() {
            return (IntFunction) CONCURRENT_HASH_MAP_FACTORY;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<ConcurrentHashMap<K, V>> ofConcurrentHashMap() {
            return (IntFunction) CONCURRENT_HASH_MAP_FACTORY;
        }

        public static final class IntFunctions extends Factory {
            private IntFunctions() {
                // singleton.
            }
        }
    }

    public static final class Suppliers {
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super List> LIST = new Supplier<List>() {
            @Override
            public List get() {
                return new ArrayList();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final Supplier<? super LinkedList> LINKED_LIST = new Supplier<LinkedList>() {
            @Override
            public LinkedList get() {
                return new LinkedList();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final Supplier<? super Set> SET = new Supplier<Set>() {
            @Override
            public Set get() {
                return new HashSet();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final Supplier<? super LinkedHashSet> LINKED_HASH_SET = new Supplier<LinkedHashSet>() {
            @Override
            public LinkedHashSet get() {
                return new LinkedHashSet();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final Supplier<? super TreeSet> TREE_SET = new Supplier<TreeSet>() {
            @Override
            public TreeSet get() {
                return new TreeSet();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final Supplier<? super Queue> QUEUE = new Supplier<Queue>() {
            @Override
            public Queue get() {
                return new LinkedList();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final Supplier<? super Deque> DEQUE = new Supplier<Deque>() {
            @Override
            public Deque get() {
                return new LinkedList();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final Supplier<? super ArrayDeque> ARRAY_DEQUE = new Supplier<ArrayDeque>() {
            @Override
            public ArrayDeque get() {
                return new ArrayDeque();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final Supplier<? super LinkedBlockingQueue> LINKED_BLOCKING_QUEUE = new Supplier<LinkedBlockingQueue>() {
            @Override
            public LinkedBlockingQueue get() {
                return new LinkedBlockingQueue();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final Supplier<? super ConcurrentLinkedQueue> CONCURRENT_LINKED_QUEUE = new Supplier<ConcurrentLinkedQueue>() {
            @Override
            public ConcurrentLinkedQueue get() {
                return new ConcurrentLinkedQueue();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final Supplier<? super PriorityQueue> PRIORITY_QUEUE = new Supplier<PriorityQueue>() {
            @Override
            public PriorityQueue get() {
                return new PriorityQueue();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final Supplier<? super Map> MAP = new Supplier<Map>() {
            @Override
            public Map get() {
                return new HashMap();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final Supplier<? super LinkedHashMap> LINKED_HASH_MAP = new Supplier<LinkedHashMap>() {
            @Override
            public LinkedHashMap get() {
                return new LinkedHashMap();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final Supplier<? super IdentityHashMap> IDENTITY_HASH_MAP = new Supplier<IdentityHashMap>() {
            @Override
            public IdentityHashMap get() {
                return new IdentityHashMap();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final Supplier<? super TreeMap> TREE_MAP = new Supplier<TreeMap>() {
            @Override
            public TreeMap get() {
                return new TreeMap();
            }
        };

        @SuppressWarnings("rawtypes")
        private static final Supplier<? super ConcurrentHashMap> CONCURRENT_HASH_MAP = new Supplier<ConcurrentHashMap>() {
            @Override
            public ConcurrentHashMap get() {
                return new ConcurrentHashMap();
            }
        };

        private static final Supplier<StringBuilder> STRING_BUILDER = new Supplier<StringBuilder>() {
            @Override
            public StringBuilder get() {
                return new StringBuilder();
            }
        };

        private Suppliers() {
            // singleton.
        }

        @SuppressWarnings("rawtypes")
        public static <T> Supplier<List<T>> ofList() {
            return (Supplier) LIST;
        }

        @SuppressWarnings("rawtypes")
        public static <T> Supplier<LinkedList<T>> ofLinkedList() {
            return (Supplier) LINKED_LIST;
        }

        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Set<T>> ofSet() {
            return (Supplier) SET;
        }

        @SuppressWarnings("rawtypes")
        public static <T> Supplier<LinkedHashSet<T>> ofLinkedHashSet() {
            return (Supplier) LINKED_HASH_SET;
        }

        @SuppressWarnings("rawtypes")
        public static <T> Supplier<SortedSet<T>> ofSortedSet() {
            return (Supplier) TREE_SET;
        }

        @SuppressWarnings("rawtypes")
        public static <T> Supplier<NavigableSet<T>> ofNavigableSet() {
            return (Supplier) TREE_SET;
        }

        @SuppressWarnings("rawtypes")
        public static <T> Supplier<TreeSet<T>> ofTreeSet() {
            return (Supplier) TREE_SET;
        }

        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Queue<T>> ofQueue() {
            return (Supplier) QUEUE;
        }

        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Deque<T>> ofDeque() {
            return (Supplier) DEQUE;
        }

        @SuppressWarnings("rawtypes")
        public static <T> Supplier<ArrayDeque<T>> ofArrayDeque() {
            return (Supplier) ARRAY_DEQUE;
        }

        @SuppressWarnings("rawtypes")
        public static <T> Supplier<LinkedBlockingQueue<T>> ofLinkedBlockingQueue() {
            return (Supplier) LINKED_BLOCKING_QUEUE;
        }

        @SuppressWarnings("rawtypes")
        public static <T> Supplier<ConcurrentLinkedQueue<T>> ofConcurrentLinkedQueue() {
            return (Supplier) CONCURRENT_LINKED_QUEUE;
        }

        @SuppressWarnings("rawtypes")
        public static <T> Supplier<PriorityQueue<T>> ofPriorityQueue() {
            return (Supplier) PRIORITY_QUEUE;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<Map<K, V>> ofMap() {
            return (Supplier) MAP;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<LinkedHashMap<K, V>> ofLinkedHashMap() {
            return (Supplier) LINKED_HASH_MAP;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<IdentityHashMap<K, V>> ofIdentityHashMap() {
            return (Supplier) IDENTITY_HASH_MAP;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<SortedMap<K, V>> ofSortedMap() {
            return (Supplier) TREE_MAP;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<NavigableMap<K, V>> ofNavigableMap() {
            return (Supplier) TREE_MAP;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<TreeMap<K, V>> ofTreeMap() {
            return (Supplier) TREE_MAP;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<ConcurrentMap<K, V>> ofConcurrentMap() {
            return (Supplier) CONCURRENT_HASH_MAP;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<ConcurrentHashMap<K, V>> ofConcurrentHashMap() {
            return (Supplier) CONCURRENT_HASH_MAP;
        }

        public static Supplier<StringBuilder> ofStringBuilder() {
            return STRING_BUILDER;
        }
    }

    public static final class Predicates {

        private Predicates() {
            // singleton.
        }

        public static <T, U> Predicate<T> create(final U u, final BiPredicate<? super T, ? super U> predicate) {
            Objects.requireNonNull(predicate);

            return new Predicate<T>() {
                @Override
                public boolean test(T t) {
                    return predicate.test(t, u);
                }
            };
        }

        public static <T> Predicate<T> distinctBy() {
            return new Predicate<T>() {
                private final Map<Object, Object> map = new ConcurrentHashMap<>();

                @Override
                public boolean test(T value) {
                    return map.put(value, NONE) == null;
                }
            };
        }

        public static <T> Predicate<T> distinctBy(final Function<? super T, ?> mapper) {
            return new Predicate<T>() {
                private final Map<Object, Object> map = new ConcurrentHashMap<>();

                @Override
                public boolean test(T value) {
                    return map.put(mapper.apply(value), NONE) == null;
                }
            };
        }
    }

    public static final class BiPredicates {

        @SuppressWarnings("rawtypes")
        private static final BiPredicate ALWAYS_TRUE = new BiPredicate() {
            @Override
            public boolean test(Object t, Object u) {
                return true;
            }
        };

        @SuppressWarnings("rawtypes")
        private static final BiPredicate ALWAYS_FALSE = new BiPredicate() {
            @Override
            public boolean test(Object t, Object u) {
                return false;
            }
        };

        @SuppressWarnings("rawtypes")
        private static final BiPredicate EQUAL = new BiPredicate() {
            @Override
            public boolean test(Object t, Object u) {
                return Objects.equals(t, u);
            }
        };

        @SuppressWarnings("rawtypes")
        private static final BiPredicate NOT_EQUAL = new BiPredicate() {
            @Override
            public boolean test(Object t, Object u) {
                return !Objects.equals(t, u);
            }
        };

        @SuppressWarnings("rawtypes")
        private static final BiPredicate<? extends Comparable, ? extends Comparable> GREATER_THAN = new BiPredicate<Comparable, Comparable>() {
            @Override
            public boolean test(Comparable t, Comparable u) {
                return MoreObjects.compare(t, u) > 0;
            }
        };

        @SuppressWarnings("rawtypes")
        private static final BiPredicate<? extends Comparable, ? extends Comparable> GREATER_EQUAL = new BiPredicate<Comparable, Comparable>() {
            @Override
            public boolean test(Comparable t, Comparable u) {
                return MoreObjects.compare(t, u) >= 0;
            }
        };

        @SuppressWarnings("rawtypes")
        private static final BiPredicate<? extends Comparable, ? extends Comparable> LESS_THAN = new BiPredicate<Comparable, Comparable>() {
            @Override
            public boolean test(Comparable t, Comparable u) {
                return MoreObjects.compare(t, u) < 0;
            }
        };

        @SuppressWarnings("rawtypes")
        private static final BiPredicate<? extends Comparable, ? extends Comparable> LESS_EQUAL = new BiPredicate<Comparable, Comparable>() {
            @Override
            public boolean test(Comparable t, Comparable u) {
                return MoreObjects.compare(t, u) <= 0;
            }
        };

        private BiPredicates() {
            // singleton.
        }

        public static <T, U> BiPredicate<T, U> alwaysTrue() {
            return ALWAYS_TRUE;
        }

        public static <T, U> BiPredicate<T, U> alwaysFalse() {
            return ALWAYS_FALSE;
        }
    }

    public static final class Consumers {
        private Consumers() {
            // singleton.
        }

        public static <T, U> Consumer<T> create(final U u, final BiConsumer<? super T, ? super U> action) {
            Objects.requireNonNull(action);

            return new Consumer<T>() {
                @Override
                public void accept(T t) {
                    action.accept(t, u);
                }
            };
        }

        /**
         * Returns a <code>Consumer</code> which calls the specified
         * <code>func</code>.
         * 
         * @param func
         * @return
         */
        public static <T> Consumer<T> create(final Function<? super T, ?> func) {
            Objects.requireNonNull(func);

            return new Consumer<T>() {
                @Override
                public void accept(T t) {
                    func.apply(t);
                }
            };
        }
    }

    public static final class BiConsumers {

        @SuppressWarnings("rawtypes")
        private static final BiConsumer DO_NOTHING = new BiConsumer() {
            @Override
            public void accept(Object t, Object u) {
                // do nothing.
            }
        };

        private static final BiConsumer<Collection<Object>, Object> ADD = new BiConsumer<Collection<Object>, Object>() {
            @Override
            public void accept(Collection<Object> t, Object u) {
                t.add(u);
            }
        };

        private static final BiConsumer<Collection<Object>, Collection<Object>> ADD_ALL = new BiConsumer<Collection<Object>, Collection<Object>>() {
            @Override
            public void accept(Collection<Object> t, Collection<Object> u) {
                t.addAll(u);
            }
        };

        private static final BiConsumer<Collection<Object>, Object> REMOVE = new BiConsumer<Collection<Object>, Object>() {
            @Override
            public void accept(Collection<Object> t, Object u) {
                t.remove(u);
            }
        };

        private static final BiConsumer<Collection<Object>, Collection<Object>> REMOVE_ALL = new BiConsumer<Collection<Object>, Collection<Object>>() {
            @Override
            public void accept(Collection<Object> t, Collection<Object> u) {
                t.removeAll(u);
            }
        };

        private static final BiConsumer<Map<Object, Object>, Map.Entry<Object, Object>> PUT = new BiConsumer<Map<Object, Object>, Map.Entry<Object, Object>>() {
            @Override
            public void accept(Map<Object, Object> t, Map.Entry<Object, Object> u) {
                t.put(u.getKey(), u.getValue());
            }
        };

        private static final BiConsumer<Map<Object, Object>, Map<Object, Object>> PUT_ALL = new BiConsumer<Map<Object, Object>, Map<Object, Object>>() {
            @Override
            public void accept(Map<Object, Object> t, Map<Object, Object> u) {
                t.putAll(u);
            }
        };

        private static final BiConsumer<Map<Object, Object>, Object> REMOVE_BY_KEY = new BiConsumer<Map<Object, Object>, Object>() {
            @Override
            public void accept(Map<Object, Object> t, Object u) {
                t.remove(u);
            }
        };

        private static final BiConsumer<StringBuilder, Object> APPEND = new BiConsumer<StringBuilder, Object>() {
            @Override
            public void accept(StringBuilder t, Object u) {
                t.append(u);
            }
        };

        private BiConsumers() {
            // singleton.
        }

        public static <T, U> BiConsumer<T, U> doNothing() {
            return DO_NOTHING;
        }

        public static <T, C extends Collection<? super T>> BiConsumer<C, T> ofAdd() {
            return (BiConsumer<C, T>) ADD;
        }

        public static <T, C extends Collection<T>> BiConsumer<C, C> ofAddAll() {
            return (BiConsumer<C, C>) ADD_ALL;
        }

        public static <T, C extends Collection<? super T>> BiConsumer<C, T> ofRemove() {
            return (BiConsumer<C, T>) REMOVE;
        }

        public static <T, C extends Collection<T>> BiConsumer<C, C> ofRemoveAll() {
            return (BiConsumer<C, C>) REMOVE_ALL;
        }

        public static <K, V, M extends Map<K, V>, E extends Map.Entry<K, V>> BiConsumer<M, E> ofPut() {
            return (BiConsumer<M, E>) PUT;
        }

        public static <K, V, M extends Map<K, V>> BiConsumer<M, M> ofPutAll() {
            return (BiConsumer<M, M>) PUT_ALL;
        }

        public static <K, V, M extends Map<K, V>> BiConsumer<M, K> ofRemoveByKey() {
            return (BiConsumer<M, K>) REMOVE_BY_KEY;
        }

        public static <T> BiConsumer<StringBuilder, T> ofAppend() {
            return (BiConsumer<StringBuilder, T>) APPEND;
        }

        /**
         * Returns a <code>BiConsumer</code> which calls the specified
         * <code>func</code>.
         * 
         * @param func
         * @return
         */
        public static <T, U> BiConsumer<T, U> create(final BiFunction<? super T, ? super U, ?> func) {
            Objects.requireNonNull(func);

            return new BiConsumer<T, U>() {
                @Override
                public void accept(T t, U u) {
                    func.apply(t, u);
                }
            };
        }
    }

    public static final class Functions {
        private Functions() {
            // singleton.
        }

        public static <T, U, R> Function<T, R> create(final U u, final BiFunction<? super T, ? super U, R> func) {
            Objects.requireNonNull(func);

            return new Function<T, R>() {
                @Override
                public R apply(T t) {
                    return func.apply(t, u);
                }
            };
        }

        /**
         * Returns a <code>Function</code> which calls the specified
         * <code>action</code> and always return an empty<code>Optional</code>
         * if <code>action</code> is executed successfully.
         * 
         * @param action
         * @return
         */
        public static <T, R> Function<T, Optional<R>> create(final Consumer<? super T> action) {
            Objects.requireNonNull(action);

            return new Function<T, Optional<R>>() {
                @Override
                public Optional<R> apply(T t) {
                    action.accept(t);
                    return Optional.empty();
                }
            };
        }
    }

    public static final class BiFunctions {

        private static final BiFunction<Object, Object, Object> RETURN_FIRST = new BiFunction<Object, Object, Object>() {
            @Override
            public Object apply(Object t, Object u) {
                return t;
            }
        };

        private static final BiFunction<Object, Object, Object> RETURN_SECOND = new BiFunction<Object, Object, Object>() {
            @Override
            public Object apply(Object t, Object u) {
                return u;
            }
        };

        private static final BiFunction<Collection<Object>, Object, Collection<Object>> ADD = new BiFunction<Collection<Object>, Object, Collection<Object>>() {
            @Override
            public Collection<Object> apply(Collection<Object> t, Object u) {
                t.add(u);
                return t;
            }
        };

        private static final BiFunction<Collection<Object>, Collection<Object>, Collection<Object>> ADD_ALL = new BiFunction<Collection<Object>, Collection<Object>, Collection<Object>>() {
            @Override
            public Collection<Object> apply(Collection<Object> t, Collection<Object> u) {
                t.addAll(u);
                return t;
            }
        };

        private static final BiFunction<Collection<Object>, Object, Collection<Object>> REMOVE = new BiFunction<Collection<Object>, Object, Collection<Object>>() {
            @Override
            public Collection<Object> apply(Collection<Object> t, Object u) {
                t.remove(u);
                return t;
            }
        };

        private static final BiFunction<Collection<Object>, Collection<Object>, Collection<Object>> REMOVE_ALL = new BiFunction<Collection<Object>, Collection<Object>, Collection<Object>>() {
            @Override
            public Collection<Object> apply(Collection<Object> t, Collection<Object> u) {
                t.removeAll(u);
                return t;
            }
        };

        private static final BiFunction<Map<Object, Object>, Map.Entry<Object, Object>, Map<Object, Object>> PUT = new BiFunction<Map<Object, Object>, Map.Entry<Object, Object>, Map<Object, Object>>() {
            @Override
            public Map<Object, Object> apply(Map<Object, Object> t, Map.Entry<Object, Object> u) {
                t.put(u.getKey(), u.getValue());
                return t;
            }
        };

        private static final BiFunction<Map<Object, Object>, Map<Object, Object>, Map<Object, Object>> PUT_ALL = new BiFunction<Map<Object, Object>, Map<Object, Object>, Map<Object, Object>>() {
            @Override
            public Map<Object, Object> apply(Map<Object, Object> t, Map<Object, Object> u) {
                t.putAll(u);
                return t;
            }
        };

        private static final BiFunction<Map<Object, Object>, Object, Map<Object, Object>> REMOVE_BY_KEY = new BiFunction<Map<Object, Object>, Object, Map<Object, Object>>() {
            @Override
            public Map<Object, Object> apply(Map<Object, Object> t, Object u) {
                t.remove(u);
                return t;
            }
        };

        private static final BiFunction<StringBuilder, Object, StringBuilder> APPEND = new BiFunction<StringBuilder, Object, StringBuilder>() {
            @Override
            public StringBuilder apply(StringBuilder t, Object u) {
                return t.append(u);
            }
        };

        private BiFunctions() {
            // singleton.
        }

        public static <T, U> BiFunction<T, U, T> returnFirst() {
            return (BiFunction<T, U, T>) RETURN_FIRST;
        }

        public static <T, U> BiFunction<T, U, U> returnSecond() {
            return (BiFunction<T, U, U>) RETURN_SECOND;
        }

        public static <T, C extends Collection<? super T>> BiFunction<C, T, C> ofAdd() {
            return (BiFunction<C, T, C>) ADD;
        }

        public static <T, C extends Collection<T>> BiFunction<C, C, C> ofAddAll() {
            return (BiFunction<C, C, C>) ADD_ALL;
        }

        public static <T, C extends Collection<? super T>> BiFunction<C, T, C> ofRemove() {
            return (BiFunction<C, T, C>) REMOVE;
        }

        public static <T, C extends Collection<T>> BiFunction<C, C, C> ofRemoveAll() {
            return (BiFunction<C, C, C>) REMOVE_ALL;
        }

        public static <K, V, M extends Map<K, V>, E extends Map.Entry<K, V>> BiFunction<M, E, M> ofPut() {
            return (BiFunction<M, E, M>) PUT;
        }

        public static <K, V, M extends Map<K, V>> BiFunction<M, M, M> ofPutAll() {
            return (BiFunction<M, M, M>) PUT_ALL;
        }

        public static <K, V, M extends Map<K, V>, U> BiFunction<M, K, M> ofRemoveByKey() {
            return (BiFunction<M, K, M>) REMOVE_BY_KEY;
        }

        public static <T> BiFunction<StringBuilder, T, StringBuilder> ofAppend() {
            return (BiFunction<StringBuilder, T, StringBuilder>) APPEND;
        }

        /**
         * Returns a <code>BiFunction</code> which calls the specified
         * <code>action</code> and always return an empty<code>Optional</code>
         * if <code>action</code> is executed successfully.
         * 
         * @param action
         * @return
         */
        public static <T, U, R> BiFunction<T, U, Optional<R>> create(final BiConsumer<? super T, ? super U> action) {
            Objects.requireNonNull(action);

            return new BiFunction<T, U, Optional<R>>() {
                @Override
                public Optional<R> apply(T t, U u) {
                    action.accept(t, u);
                    return Optional.empty();
                }
            };
        }
    }

    public static final class BinaryOperators {

        @SuppressWarnings("rawtypes")
        private static final BinaryOperator THROWING_MERGER = new BinaryOperator() {
            @Override
            public Object apply(Object t, Object u) {
                throw new IllegalStateException(String.format("Duplicate key (attempted merging values %s and %s)", t,
                    u));
            }
        };

        @SuppressWarnings("rawtypes")
        private static final BinaryOperator IGNORING_MERGER = new BinaryOperator() {
            @Override
            public Object apply(Object t, Object u) {
                return t;
            }
        };

        @SuppressWarnings("rawtypes")
        private static final BinaryOperator REPLACING_MERGER = new BinaryOperator() {
            @Override
            public Object apply(Object t, Object u) {
                return u;
            }
        };

        private static final BinaryOperator<Collection<Object>> ADD_ALL_TO_FIRST = new BinaryOperator<Collection<Object>>() {
            @Override
            public Collection<Object> apply(Collection<Object> t, Collection<Object> u) {
                t.addAll(u);
                return t;
            }
        };

        private static final BinaryOperator<Collection<Object>> ADD_ALL_TO_BIGGER = new BinaryOperator<Collection<Object>>() {
            @Override
            public Collection<Object> apply(Collection<Object> t, Collection<Object> u) {
                if (t.size() >= u.size()) {
                    t.addAll(u);
                    return t;
                } else {
                    u.addAll(t);
                    return u;
                }
            }
        };

        private static final BinaryOperator<Collection<Object>> REMOVE_ALL_FROM_FIRST = new BinaryOperator<Collection<Object>>() {
            @Override
            public Collection<Object> apply(Collection<Object> t, Collection<Object> u) {
                t.removeAll(u);
                return t;
            }
        };

        private static final BinaryOperator<Map<Object, Object>> PUT_ALL_TO_FIRST = new BinaryOperator<Map<Object, Object>>() {
            @Override
            public Map<Object, Object> apply(Map<Object, Object> t, Map<Object, Object> u) {
                t.putAll(u);
                return t;
            }
        };

        private static final BinaryOperator<Map<Object, Object>> PUT_ALL_TO_BIGGER = new BinaryOperator<Map<Object, Object>>() {
            @Override
            public Map<Object, Object> apply(Map<Object, Object> t, Map<Object, Object> u) {
                if (t.size() >= u.size()) {
                    t.putAll(u);
                    return t;
                } else {
                    u.putAll(t);
                    return u;
                }
            }
        };

        private static final BinaryOperator<StringBuilder> APPEND_TO_FIRST = new BinaryOperator<StringBuilder>() {
            @Override
            public StringBuilder apply(StringBuilder t, StringBuilder u) {
                return t.append(u);
            }
        };

        private static final BinaryOperator<StringBuilder> APPEND_TO_BIGGER = new BinaryOperator<StringBuilder>() {
            @Override
            public StringBuilder apply(StringBuilder t, StringBuilder u) {
                if (t.length() >= u.length()) {
                    return t.append(u);
                } else {
                    return u.append(t);
                }
            }
        };

        private static final BinaryOperator<String> CONCAT = new BinaryOperator<String>() {
            @Override
            public String apply(String t, String u) {
                return t + u;
            }
        };

        private static final BinaryOperator<Integer> ADD_INTEGER = new BinaryOperator<Integer>() {
            @Override
            public Integer apply(Integer t, Integer u) {
                return t.intValue() + u.intValue();
            }
        };

        private static final BinaryOperator<Long> ADD_LONG = new BinaryOperator<Long>() {
            @Override
            public Long apply(Long t, Long u) {
                return t.longValue() + u.longValue();
            }
        };

        private static final BinaryOperator<Double> ADD_DOUBLE = new BinaryOperator<Double>() {
            @Override
            public Double apply(Double t, Double u) {
                return t.doubleValue() + u.doubleValue();
            }
        };

        private static final BinaryOperator<BigInteger> ADD_BIG_INTEGER = new BinaryOperator<BigInteger>() {
            @Override
            public BigInteger apply(BigInteger t, BigInteger u) {
                return t.add(u);
            }
        };

        private static final BinaryOperator<BigDecimal> ADD_BIG_DECIMAL = new BinaryOperator<BigDecimal>() {
            @Override
            public BigDecimal apply(BigDecimal t, BigDecimal u) {
                return t.add(u);
            }
        };

        @SuppressWarnings({ "rawtypes" })
        private static final BinaryOperator<Comparable> MIN = new BinaryOperator<Comparable>() {
            @Override
            public Comparable apply(Comparable t, Comparable u) {
                return MoreObjects.compare(t, u) <= 0 ? t : u;
            }
        };

        @SuppressWarnings("rawtypes")
        private static final BinaryOperator<Comparable> MAX = new BinaryOperator<Comparable>() {
            @Override
            public Comparable apply(Comparable t, Comparable u) {
                return MoreObjects.compare(t, u) >= 0 ? t : u;
            }
        };

        private BinaryOperators() {
            // singleton.
        }

        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofAddAllToFirst() {
            return (BinaryOperator<C>) ADD_ALL_TO_FIRST;
        }

        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofAddAllToBigger() {
            return (BinaryOperator<C>) ADD_ALL_TO_BIGGER;
        }

        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofRemoveAllFromFirst() {
            return (BinaryOperator<C>) REMOVE_ALL_FROM_FIRST;
        }

        @SuppressWarnings("unchecked")
        public static <K, V, M extends Map<K, V>> BinaryOperator<M> ofPutAllToFirst() {
            return (BinaryOperator<M>) PUT_ALL_TO_FIRST;
        }

        @SuppressWarnings("unchecked")
        public static <K, V, M extends Map<K, V>> BinaryOperator<M> ofPutAllToBigger() {
            return (BinaryOperator<M>) PUT_ALL_TO_BIGGER;
        }

        public static BinaryOperator<StringBuilder> ofAppendToFirst() {
            return APPEND_TO_FIRST;
        }

        public static BinaryOperator<StringBuilder> ofAppendToBigger() {
            return APPEND_TO_BIGGER;
        }

        public static BinaryOperator<String> ofConcat() {
            return CONCAT;
        }

        public static BinaryOperator<Integer> ofAddInt() {
            return ADD_INTEGER;
        }

        public static BinaryOperator<Long> ofAddLong() {
            return ADD_LONG;
        }

        public static BinaryOperator<Double> ofAddDouble() {
            return ADD_DOUBLE;
        }

        public static BinaryOperator<BigInteger> ofAddBigInteger() {
            return ADD_BIG_INTEGER;
        }

        public static BinaryOperator<BigDecimal> ofAddBigDecimal() {
            return ADD_BIG_DECIMAL;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <T extends Comparable<? super T>> BinaryOperator<T> min() {
            return (BinaryOperator) MIN;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <T extends Comparable<? super T>> BinaryOperator<T> max() {
            return (BinaryOperator) MAX;
        }

        public static <T> BinaryOperator<T> minBy(final Comparator<? super T> comparator) {
            Objects.requireNonNull(comparator);

            return new BinaryOperator<T>() {
                @Override
                public T apply(T t, T u) {
                    return comparator.compare(t, u) <= 0 ? t : u;
                }
            };
        }

        public static <T> BinaryOperator<T> maxBy(final Comparator<? super T> comparator) {
            Objects.requireNonNull(comparator);

            return new BinaryOperator<T>() {
                @Override
                public T apply(T t, T u) {
                    return comparator.compare(t, u) >= 0 ? t : u;
                }
            };
        }
    }
}
