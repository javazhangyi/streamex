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
package com.landawn.streamex.function;

import java.util.Objects;
import java.util.function.Function;

/**
 * @author Haiyang Li
 *
 */
public interface QuadFunction<A, B, C, D, R> {

    R apply(A a, B b, C c, D d);

    default <V> QuadFunction<A, B, C, D, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);

        return (a, b, c, d) -> after.apply(apply(a, b, c, d));
    }
}