/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.streamex;

import org.junit.runner.RunWith;

import com.landawn.streamex.api.StreamExApiTest;

import junit.framework.TestSuite;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        // 
        StreamExApiTest.class, //
        AverageLongTest.class, //
        BaseStreamExTest.class, //
        CharSpliteratorTest.class, //
        CollapseSpliteratorTest.class, //
        ConstSpliteratorTest.class, //
        CrossSpliteratorTest.class, //
        CustomPoolTest.class, //
        DistinctSpliteratorTest.class, //
        DoubleCollectorTest.class, //
        DoubleStreamExTest.class, //
        EmitterTest.class, //
        EntryStreamTest.class, //
        IntCollectorTest.class, //
        InternalsTest.class, //
        IntStreamExTest.class, //
        JoiningTest.class, //
        LimiterTest.class, //
        LongCollectorTest.class, //
        LongStreamExTest.class, //
        MoreCollectorsTest.class, //
        OrderedCancellableSpliteratorTest.class, //
        PairPermutationSpliteratorTest.class, //
        PairSpliteratorTest.class, //
        PermutationSpliteratorTest.class, //
        PrependSpliteratorTest.class, //
        RangeBasedSpliteratorTest.class, //
        StreamExTest.class, //
        TailConcatSpliteratorTest.class, //
        TreeSpliteratorTest.class, //
        UnknownSizeSpliteratorTest.class, //
        UnorderedCancellableSpliteratorTest.class, //
        WithFirstSpliteratorTest.class, //
        ZipSpliteratorTest.class, //
})

public class AllUnitTest extends TestSuite {
}
