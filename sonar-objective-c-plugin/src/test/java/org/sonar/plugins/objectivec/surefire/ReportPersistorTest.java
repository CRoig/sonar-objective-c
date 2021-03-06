/*
 * Copyright (c) 2018 Tobias Raatiniemi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sonar.plugins.objectivec.surefire;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.measures.CoreMetrics;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class ReportPersistorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private DefaultInputFile classNameTestFile;
    private DefaultInputFile firstClassNameTestFile;
    private DefaultInputFile secondClassNameTestFile;

    private SensorContextTester context;
    private ReportPersistor persistor;

    @Before
    public void setUp() {
        context = SensorContextTester.create(temporaryFolder.getRoot());
        persistor = ReportPersistor.create(context);

        classNameTestFile = createFile("TestTarget/ClassNameTest.m");
        firstClassNameTestFile = createFile("TestTarget/FirstClassNameTest.m");
        secondClassNameTestFile = createFile("TestTarget/SecondClassNameTest.m");
    }

    @After
    public void tearDown() {
        temporaryFolder.delete();
    }

    @Nonnull
    private DefaultInputFile createFile(@Nonnull String relativePath) {
        return new DefaultInputFile(context.module().key(), relativePath)
                .setLanguage("bla")
                .setType(InputFile.Type.TEST)
                .initMetadata("1\n2\n3\n4\n5\n6");
    }

    private void addFileToFs(@Nonnull DefaultInputFile inputFile) {
        context.fileSystem().add(inputFile);
    }

    @Nonnull
    private <T extends Serializable> T getMeasure(@Nonnull String componentKey, @Nonnull String testKey) {
        Measure<T> measure = context.measure(componentKey, testKey);

        return measure.value();
    }

    @Test
    public void saveReports_withoutReports() {
        persistor.saveReports(Collections.emptyList());

        // TODO: How can we verify that no measures are added.
    }

    @Test
    public void saveReports_withEmptyReport() {
        TestReport testReport = TestReport.create("TestTarget.xctest", Collections.emptyList());
        addFileToFs(classNameTestFile);

        persistor.saveReports(Collections.singletonList(testReport));

        // TODO: How can we verify that no measures are added.
    }

    @Test
    public void saveReports_withOneTestCaseReport() {
        TestCase testCase = TestCase.success("ClassNameTest", "testMethodName", 0.002);
        TestSuite testSuite = TestSuite.create("ClassNameTest", Collections.singletonList(testCase));
        TestReport testReport = TestReport.create("TestTarget.xctest", Collections.singletonList(testSuite));
        addFileToFs(classNameTestFile);

        persistor.saveReports(Collections.singletonList(testReport));

        assertEquals(Integer.valueOf(1), getMeasure("projectKey:TestTarget/ClassNameTest.m", CoreMetrics.TESTS_KEY));
        assertEquals(Integer.valueOf(0), getMeasure("projectKey:TestTarget/ClassNameTest.m", CoreMetrics.TEST_FAILURES_KEY));
        assertEquals(Long.valueOf(2), getMeasure("projectKey:TestTarget/ClassNameTest.m", CoreMetrics.TEST_EXECUTION_TIME_KEY));
    }

    @Test
    public void saveReports_withMultipleTestCasesReport() {
        List<TestCase> firstTestCases = new ArrayList<>();
        firstTestCases.add(TestCase.success("FirstClassNameTest", "testMethodName_withCondition", 0.003));
        firstTestCases.add(TestCase.success("FirstClassNameTest", "testMethodName", 0.001));
        List<TestCase> secondTestCases = new ArrayList<>();
        secondTestCases.add(TestCase.success("SecondClassNameTest", "testMethodName_withCondition", 0.001));
        secondTestCases.add(TestCase.success("SecondClassNameTest", "testMethodName", 0.001));
        List<TestSuite> testSuites = new ArrayList<>();
        testSuites.add(TestSuite.create("FirstClassNameTest", firstTestCases));
        testSuites.add(TestSuite.create("SecondClassNameTest", secondTestCases));
        TestReport testReport = TestReport.create("TestTarget.xctest", testSuites);
        addFileToFs(firstClassNameTestFile);
        addFileToFs(secondClassNameTestFile);

        persistor.saveReports(Collections.singletonList(testReport));

        assertEquals(Integer.valueOf(2), getMeasure("projectKey:TestTarget/FirstClassNameTest.m", CoreMetrics.TESTS_KEY));
        assertEquals(Integer.valueOf(0), getMeasure("projectKey:TestTarget/FirstClassNameTest.m", CoreMetrics.TEST_FAILURES_KEY));
        assertEquals(Long.valueOf(4), getMeasure("projectKey:TestTarget/FirstClassNameTest.m", CoreMetrics.TEST_EXECUTION_TIME_KEY));
        assertEquals(Integer.valueOf(2), getMeasure("projectKey:TestTarget/SecondClassNameTest.m", CoreMetrics.TESTS_KEY));
        assertEquals(Integer.valueOf(0), getMeasure("projectKey:TestTarget/SecondClassNameTest.m", CoreMetrics.TEST_FAILURES_KEY));
        assertEquals(Long.valueOf(2), getMeasure("projectKey:TestTarget/SecondClassNameTest.m", CoreMetrics.TEST_EXECUTION_TIME_KEY));
    }

    @Test
    public void saveReports_withMultipleReports() {
        List<TestCase> firstTestCases = new ArrayList<>();
        firstTestCases.add(TestCase.success("FirstClassNameTest", "testMethodName_withCondition", 0.003));
        firstTestCases.add(TestCase.success("FirstClassNameTest", "testMethodName", 0.001));
        List<TestCase> secondTestCases = new ArrayList<>();
        secondTestCases.add(TestCase.success("SecondClassNameTest", "testMethodName_withCondition", 0.001));
        secondTestCases.add(TestCase.success("SecondClassNameTest", "testMethodName", 0.001));
        List<TestReport> testReports = new ArrayList<>();
        testReports.add(TestReport.create("FirstTestTarget.xctest", Collections.singletonList(TestSuite.create("FirstClassNameTest", firstTestCases))));
        testReports.add(TestReport.create("SecondTestTarget.xctest", Collections.singletonList(TestSuite.create("SecondClassNameTest", secondTestCases))));
        addFileToFs(firstClassNameTestFile);
        addFileToFs(secondClassNameTestFile);

        persistor.saveReports(testReports);

        assertEquals(Integer.valueOf(2), getMeasure("projectKey:TestTarget/FirstClassNameTest.m", CoreMetrics.TESTS_KEY));
        assertEquals(Integer.valueOf(0), getMeasure("projectKey:TestTarget/FirstClassNameTest.m", CoreMetrics.TEST_FAILURES_KEY));
        assertEquals(Long.valueOf(4), getMeasure("projectKey:TestTarget/FirstClassNameTest.m", CoreMetrics.TEST_EXECUTION_TIME_KEY));
        assertEquals(Integer.valueOf(2), getMeasure("projectKey:TestTarget/SecondClassNameTest.m", CoreMetrics.TESTS_KEY));
        assertEquals(Integer.valueOf(0), getMeasure("projectKey:TestTarget/SecondClassNameTest.m", CoreMetrics.TEST_FAILURES_KEY));
        assertEquals(Long.valueOf(2), getMeasure("projectKey:TestTarget/SecondClassNameTest.m", CoreMetrics.TEST_EXECUTION_TIME_KEY));
    }

    @Test
    public void saveReports_withErrorReport() {
        TestCase testCase = TestCase.failure("ClassNameTest", "testMethodName_withFailure");
        TestSuite testSuite = TestSuite.create("ClassNameTest", Collections.singletonList(testCase));
        TestReport testReport = TestReport.create("TestTarget.xctest", Collections.singletonList(testSuite));
        addFileToFs(classNameTestFile);

        persistor.saveReports(Collections.singletonList(testReport));

        assertEquals(Integer.valueOf(1), getMeasure("projectKey:TestTarget/ClassNameTest.m", CoreMetrics.TESTS_KEY));
        assertEquals(Integer.valueOf(1), getMeasure("projectKey:TestTarget/ClassNameTest.m", CoreMetrics.TEST_FAILURES_KEY));
        assertEquals(Long.valueOf(0), getMeasure("projectKey:TestTarget/ClassNameTest.m", CoreMetrics.TEST_EXECUTION_TIME_KEY));
    }

    @Test
    public void saveReports_withClassNameCategory() {
        TestCase testCase = TestCase.success("BaseClassName_CategoryNameTests", "testMethodName", 0.002);
        TestSuite testSuite = TestSuite.create("BaseClassName_CategoryNameTests", Collections.singletonList(testCase));
        TestReport testReport = TestReport.create("TestTarget.xctest", Collections.singletonList(testSuite));
        addFileToFs(createFile("TestTarget/BaseClassName+CategoryNameTests.m"));

        persistor.saveReports(Collections.singletonList(testReport));

        assertEquals(Integer.valueOf(1), getMeasure("projectKey:TestTarget/BaseClassName+CategoryNameTests.m", CoreMetrics.TESTS_KEY));
        assertEquals(Integer.valueOf(0), getMeasure("projectKey:TestTarget/BaseClassName+CategoryNameTests.m", CoreMetrics.TEST_FAILURES_KEY));
        assertEquals(Long.valueOf(2), getMeasure("projectKey:TestTarget/BaseClassName+CategoryNameTests.m", CoreMetrics.TEST_EXECUTION_TIME_KEY));
    }

    @Test
    public void saveReports_withTestClassInRoot() {
        TestCase testCase = TestCase.success("ClassNameTest", "testMethodName", 0.002);
        TestSuite testSuite = TestSuite.create("ClassNameTest", Collections.singletonList(testCase));
        TestReport testReport = TestReport.create("TestTarget.xctest", Collections.singletonList(testSuite));
        addFileToFs(createFile("ClassNameTest.m"));

        persistor.saveReports(Collections.singletonList(testReport));

        assertEquals(Integer.valueOf(1), getMeasure("projectKey:ClassNameTest.m", CoreMetrics.TESTS_KEY));
        assertEquals(Integer.valueOf(0), getMeasure("projectKey:ClassNameTest.m", CoreMetrics.TEST_FAILURES_KEY));
        assertEquals(Long.valueOf(2), getMeasure("projectKey:ClassNameTest.m", CoreMetrics.TEST_EXECUTION_TIME_KEY));
    }
}
