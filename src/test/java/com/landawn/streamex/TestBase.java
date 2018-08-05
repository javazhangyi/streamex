/*
 * Copyright 2015, 2016 Tagir Valeev
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
package com.landawn.streamex;

import junit.framework.TestCase;

/**
 * @author haiyangl
 *
 */
public class TestBase extends TestCase {

    //    @Test
    //    public void test_01() {
    //        List<File> testFiles = IOUtil.listFiles(new File("./src/test/java"), true, true);
    //        testFiles.stream().filter(f -> !(f.getName().equals("TestBase.java") || f.getName().equals("AllUnitTest.java")))
    //                .filter(f -> f.getName().endsWith("Test.java")).forEach(f -> {
    //                    List<String> lines = IOUtil.readLines(f);
    //                    for (int i = 0; i < lines.size(); i++) {
    //                        if (lines.get(i).endsWith("Test  extends TestCase {")) {
    //                            lines.set(i, lines.get(i).replace("Test  extends TestCase {", "Test extends TestCase {"));
    //                        }
    //                    }
    //
    //                    IOUtil.writeLines(f, lines);
    //
    //                    N.println("suite.addTestSuite(" + f.getName().replace(".java", ".class") + ");");
    //                });
    //    }

}
