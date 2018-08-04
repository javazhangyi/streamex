/*
 * Copyright (C) 2017 HaiYang Li
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
package com.landawn.streamex.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * @author haiyangl
 *
 */
public final class MoreObjects {
    private MoreObjects() {
        // singleton.
    }

    /**
     * 
     * @param obj
     * @return the input <code>obj</code>
     */
    public static <T> T println(final T obj) {
        final String str = obj instanceof Object[] ? Arrays.deepToString((Object[]) obj) : Objects.toString(obj);
        System.out.println(str);
        return obj;
    }

    public static void checkIndex(final int index, final int length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException(String.format("Index %d out-of-bounds for length %d", index, length));
        }
    }

    public static void checkFromToIndex(final int fromIndex, final int toIndex, final int length) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length) {
            throw new IndexOutOfBoundsException(String.format("Range [%d, %d) out-of-bounds for length %d", fromIndex,
                toIndex, length));
        }
    }

    public static void checkFromIndexSize(final int fromIndex, final int size, final int length) {
        if ((length | fromIndex | size) < 0 || size > length - fromIndex) {
            throw new IndexOutOfBoundsException(String.format("Range [%d, %<d + %d) out-of-bounds for length %d",
                fromIndex, size, length));
        }
    }

    public static void close(final AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                throw toRuntimeException(e);
            }
        }
    }

    @SafeVarargs
    public static void closeAll(final AutoCloseable... a) {
        if (a == null || a.length == 0) {
            return;
        }

        closeAll(Arrays.asList(a));
    }

    public static void closeAll(final Collection<? extends AutoCloseable> c) {
        if (c == null || c.size() == 0) {
            return;
        }

        Throwable ex = null;

        for (AutoCloseable closeable : c) {
            try {
                close(closeable);
            } catch (Throwable e) {
                if (ex == null) {
                    ex = e;
                } else {
                    ex.addSuppressed(e);
                }
            }
        }

        if (ex != null) {
            throw toRuntimeException(ex);
        }
    }

    public static void closeQuietly(final AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable e) {
                // ignore
            }
        }
    }

    @SafeVarargs
    public static void closeAllQuietly(final AutoCloseable... a) {
        if (a == null || a.length == 0) {
            return;
        }

        closeAllQuietly(Arrays.asList(a));
    }

    public static void closeAllQuietly(final Collection<? extends AutoCloseable> c) {
        if (c == null || c.size() == 0) {
            return;
        }

        for (AutoCloseable closeable : c) {
            closeQuietly(closeable);
        }
    }

    public static <T extends Comparable<? super T>> int compare(final T a, final T b) {
        return a == null ? (b == null ? 0 : -1) : (b == null ? 1 : a.compareTo(b));
    }

    public static <T> int compare(final T a, final T b, final Comparator<? super T> cmp) {
        return a == null ? (b == null ? 0 : -1)
                : (b == null ? 1 : (cmp == null ? Comparators.NATURAL_ORDER : cmp).compare(a, b));
    }

    public static int compareIgnoreCase(final String a, final String b) {
        return a == null ? (b == null ? 0 : -1) : (b == null ? 1 : a.compareToIgnoreCase(b));
    }

    public static RuntimeException toRuntimeException(Throwable e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        } else if (e instanceof ExecutionException || e instanceof InvocationTargetException) {
            return e.getCause() == null ? new RuntimeException(e) : toRuntimeException(e.getCause());
        } else {
            return new RuntimeException(e);
        }
    }
}
