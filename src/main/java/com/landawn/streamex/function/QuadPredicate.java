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

/**
 * @author Haiyang Li
 *
 */
public interface QuadPredicate<A, B, C, D> {

    boolean test(A a, B b, C c, D d);

    default QuadPredicate<A, B, C, D> and(QuadPredicate<? super A, ? super B, ? super C, ? super D> other) {
        Objects.requireNonNull(other);

        return (a, b, c, d) -> test(a, b, c, d) && other.test(a, b, c, d);
    }

    default QuadPredicate<A, B, C, D> or(QuadPredicate<? super A, ? super B, ? super C, ? super D> other) {
        Objects.requireNonNull(other);

        return (a, b, c, d) -> test(a, b, c, d) || other.test(a, b, c, d);
    }

    default QuadPredicate<A, B, C, D> negate() {
        return (a, b, c, d) -> !test(a, b, c, d);
    }
}
