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
public interface IndexedBiPredicate<U, T> {

    boolean test(U u, int idx, T e);

    default IndexedBiPredicate<U, T> negate() {
        return (u, idx, e) -> !test(u, idx, e);
    }

    default IndexedBiPredicate<U, T> and(IndexedBiPredicate<? super U, ? super T> other) {
        Objects.requireNonNull(other);

        return (u, idx, e) -> test(u, idx, e) && other.test(u, idx, e);
    }

    default IndexedBiPredicate<U, T> or(IndexedBiPredicate<? super U, ? super T> other) {
        Objects.requireNonNull(other);

        return (u, idx, e) -> test(u, idx, e) || other.test(u, idx, e);
    }
}
