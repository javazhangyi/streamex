/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.streamex.util;

import java.util.Objects;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public final class Indexed<T> extends AbstractIndexed {
    private final T value;

    Indexed(long index, T value) {
        super(index);
        this.value = value;
    }

    public static <T> Indexed<T> of(T value, int index) {
        return new Indexed<>(index, value);
    }

    public static <T> Indexed<T> of(T value, long index) {
        return new Indexed<>(index, value);
    }

    public T value() {
        return value;
    }

    @Override
    public int hashCode() {
        return (int) index + (value == null ? 0 : value.hashCode()) * 31;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Indexed && ((Indexed<T>) obj).index == index && Objects.equals(((Indexed<T>) obj).value,
            value);
    }

    @Override
    public String toString() {
        return "[" + index + "]=" + Objects.toString(value);
    }
}
