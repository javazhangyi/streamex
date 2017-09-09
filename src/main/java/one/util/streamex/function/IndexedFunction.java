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
package one.util.streamex.function;

import java.util.Objects;

/**
 * @author Haiyang Li
 *
 */
public interface IndexedFunction<T, R> {

    R apply(int idx, T e);

    default <V> IndexedFunction<V, R> compose(IndexedFunction<? super V, ? extends T> before) {
        Objects.requireNonNull(before);

        return (idx, v) -> apply(idx, before.apply(idx, v));
    }

    default <V> IndexedFunction<T, V> andThen(IndexedFunction<? super R, ? extends V> after) {
        Objects.requireNonNull(after);

        return (idx, t) -> after.apply(idx, apply(idx, t));
    }
}

