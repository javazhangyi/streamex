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

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public final class IndexedInt extends AbstractIndexed {
    private final int value;

    IndexedInt(long index, int value) {
        super(index);
        this.value = value;
    }

    public static IndexedInt of(int value, int index) {
        return new IndexedInt(index, value);
    }

    public static IndexedInt of(int value, long index) {
        return new IndexedInt(index, value);
    }

    public int value() {
        return value;
    }

    @Override
    public int hashCode() {
        return (int) index + value * 31;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IndexedInt && ((IndexedInt) obj).index == index && ((IndexedInt) obj).value == value;
    }

    @Override
    public String toString() {
        return "[" + index + "]=" + value;
    }

}
