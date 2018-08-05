/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.streamex;

import com.landawn.streamex.api.StreamExApiTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public class AllUnitTest extends TestSuite {

    public static Test suite() {
        TestSuite suite = new TestSuite("All Unit Test");

        suite.addTestSuite(StreamExApiTest.class);
        suite.addTestSuite(AverageLongTest.class);
        suite.addTestSuite(BaseStreamExTest.class);
        suite.addTestSuite(CharSpliteratorTest.class);
        suite.addTestSuite(CollapseSpliteratorTest.class);
        suite.addTestSuite(ConstSpliteratorTest.class);
        suite.addTestSuite(CrossSpliteratorTest.class);
        suite.addTestSuite(CustomPoolTest.class);
        suite.addTestSuite(DistinctSpliteratorTest.class);
        suite.addTestSuite(DoubleCollectorTest.class);
        suite.addTestSuite(DoubleStreamExTest.class);
        suite.addTestSuite(EmitterTest.class);
        suite.addTestSuite(EntryStreamTest.class);
        suite.addTestSuite(IntCollectorTest.class);
        suite.addTestSuite(InternalsTest.class);
        suite.addTestSuite(IntStreamExTest.class);
        suite.addTestSuite(JoiningTest.class);
        suite.addTestSuite(LimiterTest.class);
        suite.addTestSuite(LongCollectorTest.class);
        suite.addTestSuite(LongStreamExTest.class);
        suite.addTestSuite(MoreCollectorsTest.class);
        suite.addTestSuite(OrderedCancellableSpliteratorTest.class);
        suite.addTestSuite(PairPermutationSpliteratorTest.class);
        suite.addTestSuite(PairSpliteratorTest.class);
        suite.addTestSuite(PermutationSpliteratorTest.class);
        suite.addTestSuite(PrependSpliteratorTest.class);
        suite.addTestSuite(RangeBasedSpliteratorTest.class);
        suite.addTestSuite(StreamExTest.class);
        suite.addTestSuite(TailConcatSpliteratorTest.class);
        suite.addTestSuite(TreeSpliteratorTest.class);
        suite.addTestSuite(UnknownSizeSpliteratorTest.class);
        suite.addTestSuite(UnorderedCancellableSpliteratorTest.class);
        suite.addTestSuite(WithFirstSpliteratorTest.class);
        suite.addTestSuite(ZipSpliteratorTest.class);

        return suite;
    }
}
