//amega
/**
 * Copyright 2021 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.ollir.OllirUtils;
import pt.up.fe.specs.util.SpecsIo;

import java.util.ArrayList;

public class BackendTest {

    @Test
    public void testHelloWorld() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));

        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("Hello, World!", output.trim());
    }

    @Test
    public void testMonteCarloPi() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm"));

        TestUtils.noErrors(result.getReports());

        //var output = result.run();
        //assertEquals("30", output.trim());
    }
    @Test
    public void testLazysort() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Lazysort.jmm"));

        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("30", output.trim());
    }

    @Test
    public void testWhileAndIf() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/WhileAndIf.jmm"));

        TestUtils.noErrors(result.getReports());

        var output = result.run();

        assertEquals(10, output.split("\n").length);
    }

    @Test
    public void testFindMaximum() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/FindMaximum.jmm"));

        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("Result: 28", output.trim());
    }

    @Test
    public void testSimple() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Simple.jmm"));

        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("30", output.trim());
    }

    @Test
    public void testLife() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Life.jmm"));

        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("30", output.trim());
    }

    @Test
    public void testTicTacToe() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/TicTacToe.jmm"));

        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("30", output.trim());
    }

    @Test
    public void testQuicksort() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Quicksort.jmm"));

        TestUtils.noErrors(result.getReports());

        var output = result.run();
        /*assertEquals("1\n" +
                "2\n" +
                "3\n" +
                "4\n" +
                "5\n" +
                "6\n" +
                "7\n" +
                "8\n" +
                "9\n" +
                "10", output.trim());*/
    }
/*
    @Test
    public void testMyClass1() {
        var result = TestUtils.backend(new OllirResult(OllirUtils.parse(SpecsIo.getResource("fixtures/public/ollir/myclass1.ollir")), null, new ArrayList<>()));

        TestUtils.noErrors(result.getReports());

        var output = result.run();
        System.out.println(output);
        //assertEquals("Hello, World!", output.trim());
    }

    @Test
    public void testtest() {
        var result = TestUtils.backend(new OllirResult(OllirUtils.parse(SpecsIo.getResource("fixtures/public/ollir/test.ollir")), null, new ArrayList<>()));

        TestUtils.noErrors(result.getReports());

        var output = result.run();
        System.out.println(output);
        //assertEquals("Hello, World!", output.trim());
    }

    @Test
    public void testMyClass2() {
        var result = TestUtils.backend(new OllirResult(OllirUtils.parse(SpecsIo.getResource("fixtures/public/ollir/myclass2.ollir")), null, new ArrayList<>()));

        TestUtils.noErrors(result.getReports());

        var output = result.run();
        System.out.println(output);
    }

    @Test
    public void testMyClass3() {
        var result = TestUtils.backend(new OllirResult(OllirUtils.parse(SpecsIo.getResource("fixtures/public/ollir/myclass3.ollir")), null, new ArrayList<>()));

        TestUtils.noErrors(result.getReports());

        var output = result.run();
        System.out.println(output);
    }

    @Test
    public void testMyClass4() {
        var result = TestUtils.backend(new OllirResult(OllirUtils.parse(SpecsIo.getResource("fixtures/public/ollir/myclass4.ollir")), null, new ArrayList<>()));

        TestUtils.noErrors(result.getReports());

        var output = result.run();
        System.out.println(output);
    }

    @Test
    public void testFac() {
        var result = TestUtils.backend(new OllirResult(OllirUtils.parse(SpecsIo.getResource("fixtures/public/ollir/Fac.ollir")), null, new ArrayList<>()));

        TestUtils.noErrors(result.getReports());

        var output = result.run();
        System.out.println(output);
    }*/
}
