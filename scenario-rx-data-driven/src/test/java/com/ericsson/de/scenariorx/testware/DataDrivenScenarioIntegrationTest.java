package com.ericsson.de.scenariorx.testware;

import static com.ericsson.cifwk.taf.scenario.ext.exporter.ScenarioExecutionGraphListenerBuilder.TAF_SCENARIO_DEBUG_ENABLED;
import static com.google.common.collect.Iterables.getLast;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.ericsson.cifwk.taf.scenario.ext.exporter.SvgExporter;
import org.assertj.core.api.Assertions;
import org.junit.After;
import ru.yandex.qatools.allure.model.Attachment;
import ru.yandex.qatools.allure.model.Status;
import ru.yandex.qatools.allure.model.TestCaseResult;
import ru.yandex.qatools.allure.model.TestSuiteResult;


public class DataDrivenScenarioIntegrationTest {

    public static final String ROOT_SUITE = "Data Driven Suite";

    public static final String TEST_CLASS = DataDrivenTest.class.getSimpleName();

    public static final String TEST_STEP_TITLE_1A = TEST_CLASS + "#" + DataDrivenTest.TEST_STEP_1;
    public static final String TEST_STEP_TITLE_1B = TEST_CLASS + "#" + DataDrivenTest.TEST_STEP_1;
    public static final String TEST_STEP_TITLE_2 = TEST_CLASS + "#" + DataDrivenTest.TEST_STEP_2;
    public static final String TEST_STEP_TITLE_3 = TEST_CLASS + "#" + DataDrivenTest.TEST_STEP_3;
    public static final String TEST_STEP_TITLE_4 = TEST_CLASS + "#" + DataDrivenTest.TEST_STEP_4;
    public static final String TEST_STEP_FAILED_TITLE = TEST_CLASS + "#" + DataDrivenTest.TEST_STEP_THROW_EXCEPTION;
    public static final String ASSERTION_FAILED_TITLE = TEST_CLASS + "#" + DataDrivenTest.TEST_STEP_ASSERTION_FAILED;

    public static final String PARALLEL_THREADS = "parallelThreads";

    private static final String TEST_CASE_NOT_STARTED = "TestId not found as test case didn't start successfully, refer to log for details";

    @org.junit.Test
    public void allureReportSequential() throws Exception {
        AllureTestUtils.runTestNg(ROOT_SUITE, DataDrivenTest.class);
        testXmlReports();
    }

    @org.junit.Test
    public void allureReportParallel() throws Exception {
        System.setProperty(PARALLEL_THREADS, "2");

        AllureTestUtils.runTestNg(ROOT_SUITE, DataDrivenTest.class);
        testXmlReports();
    }

    @org.junit.Test
    public void allureReportParallelMoreVUsers() throws Exception {
        System.setProperty(PARALLEL_THREADS, "10");

        AllureTestUtils.runTestNg(ROOT_SUITE, DataDrivenTest.class);
        testXmlReports();
    }

    @After
    public void tearDown() throws Exception {
        System.clearProperty(PARALLEL_THREADS);
        System.clearProperty(TAF_SCENARIO_DEBUG_ENABLED);
    }

    private void testXmlReports() throws Exception {

        TestSuiteResult testSuite1 = AllureTestUtils.getLatestTestSuite(DataDrivenTest.TEST_SUITE_1);
        Assertions.assertThat(AllureTestUtils.getTestCaseNames(testSuite1)).containsOnly(DataDrivenTest.TEST_CASE_1, DataDrivenTest.TEST_CASE_2);
        Assertions.assertThat(AllureTestUtils.getTestStepNames(testSuite1, DataDrivenTest.TEST_CASE_1)).containsOnly(TEST_STEP_TITLE_1A, TEST_STEP_TITLE_2);
        Assertions.assertThat(AllureTestUtils.getTestStepNames(testSuite1, DataDrivenTest.TEST_CASE_2)).containsOnly(TEST_STEP_TITLE_1B, TEST_STEP_TITLE_2);
        TestCaseResult testCase1 = AllureTestUtils.getTestCases(testSuite1, DataDrivenTest.TEST_CASE_1).get(0);
        assertThat(testCase1.getStart()).isLessThanOrEqualTo(testCase1.getStop());

        TestSuiteResult testSuite2 = AllureTestUtils.getLatestTestSuite(DataDrivenTest.TEST_SUITE_2);
        Assertions.assertThat(AllureTestUtils.getTestCaseNames(testSuite2)).containsOnly(DataDrivenTest.TEST_CASE_1, DataDrivenTest.TEST_CASE_2);
        Assertions.assertThat(AllureTestUtils.getTestStepNames(testSuite2, DataDrivenTest.TEST_CASE_1)).containsOnly(TEST_STEP_TITLE_1A, TEST_STEP_TITLE_2, TEST_STEP_TITLE_3, TEST_STEP_TITLE_4);
        Assertions.assertThat(AllureTestUtils.getTestStepNames(testSuite2, DataDrivenTest.TEST_CASE_2)).containsOnly(TEST_STEP_TITLE_1B, TEST_STEP_TITLE_2, TEST_STEP_TITLE_3, TEST_STEP_TITLE_4);

        TestSuiteResult testSuite3 = AllureTestUtils.getLatestTestSuite(DataDrivenTest.TEST_SUITE_3);
        Assertions.assertThat(AllureTestUtils.getTestCaseNames(testSuite3)).containsOnly(DataDrivenTest.TEST_CASE_1, DataDrivenTest.TEST_CASE_2);
        Assertions.assertThat(AllureTestUtils.getTestStepNames(testSuite3, DataDrivenTest.TEST_CASE_1)).containsOnly(TEST_STEP_TITLE_1A, TEST_STEP_FAILED_TITLE, TEST_STEP_TITLE_3);
        Assertions.assertThat(AllureTestUtils.getTestStepNames(testSuite3, DataDrivenTest.TEST_CASE_2)).containsOnly(TEST_STEP_TITLE_1B, TEST_STEP_FAILED_TITLE, TEST_STEP_TITLE_3);
        assertThat(testSuite3.getTestCases().get(0).getSteps().get(1).getStatus()).isEqualTo(Status.BROKEN);
        assertThat(testSuite3.getTestCases().get(0).getStatus()).isEqualTo(Status.BROKEN);
        assertThat(testSuite3.getTestCases().get(1).getSteps().get(1).getStatus()).isEqualTo(Status.BROKEN);
        assertThat(testSuite3.getTestCases().get(1).getStatus()).isEqualTo(Status.BROKEN);

        TestSuiteResult testSuite4 = AllureTestUtils.getLatestTestSuite(DataDrivenTest.TEST_SUITE_4);
        Assertions.assertThat(AllureTestUtils.getTestCaseNames(testSuite4)).containsOnly(DataDrivenTest.TEST_CASE_1, DataDrivenTest.TEST_CASE_2);
        Assertions.assertThat(AllureTestUtils.getTestStepNames(testSuite4, DataDrivenTest.TEST_CASE_1)).containsOnly(TEST_STEP_TITLE_1A, TEST_STEP_FAILED_TITLE);
        assertThat(testSuite4.getTestCases().get(0).getSteps().get(1).getStatus()).isEqualTo(Status.BROKEN);
        assertThat(testSuite4.getTestCases().get(0).getStatus()).isEqualTo(Status.BROKEN);
        Assertions.assertThat(AllureTestUtils.getTestStepNames(testSuite4, DataDrivenTest.TEST_CASE_2)).containsOnly(TEST_STEP_TITLE_1B, TEST_STEP_FAILED_TITLE);
        assertThat(testSuite4.getTestCases().get(1).getSteps().get(1).getStatus()).isEqualTo(Status.BROKEN);
        assertThat(testSuite4.getTestCases().get(1).getStatus()).isEqualTo(Status.BROKEN);

        TestSuiteResult testSuite5 = AllureTestUtils.getLatestTestSuite(DataDrivenTest.TEST_SUITE_5);
        Assertions.assertThat(AllureTestUtils.getTestCaseNames(testSuite5)).containsOnly(DataDrivenTest.TEST_CASE_1, DataDrivenTest.TEST_CASE_2);
        Assertions.assertThat(AllureTestUtils.getTestStepNames(testSuite5, DataDrivenTest.TEST_CASE_1)).containsOnly(TEST_STEP_TITLE_1A, TEST_STEP_FAILED_TITLE, TEST_STEP_TITLE_3);
        assertThat(testSuite5.getTestCases().get(0).getSteps().get(1).getStatus()).isEqualTo(Status.BROKEN);
        assertThat(testSuite5.getTestCases().get(0).getStatus()).isEqualTo(Status.BROKEN);
        Assertions.assertThat(AllureTestUtils.getTestStepNames(testSuite5, DataDrivenTest.TEST_CASE_2)).containsOnly(TEST_STEP_TITLE_1B, TEST_STEP_FAILED_TITLE, TEST_STEP_TITLE_3);
        assertThat(testSuite5.getTestCases().get(1).getSteps().get(1).getStatus()).isEqualTo(Status.BROKEN);
        assertThat(testSuite5.getTestCases().get(1).getStatus()).isEqualTo(Status.BROKEN);

        TestSuiteResult testSuite6 = AllureTestUtils.getLatestTestSuite(DataDrivenTest.TEST_SUITE_6);
        Assertions.assertThat(AllureTestUtils.getTestCaseNames(testSuite6)).containsOnly(DataDrivenTest.TEST_CASE_1);
        Assertions.assertThat(AllureTestUtils.getTestStepNames(testSuite6, DataDrivenTest.TEST_CASE_1)).containsOnly(TEST_STEP_TITLE_1A, TEST_STEP_FAILED_TITLE, TEST_STEP_TITLE_2, TEST_STEP_TITLE_3);
        assertThat(testSuite6.getTestCases().get(0).getSteps().get(1).getStatus()).isEqualTo(Status.PASSED);
        assertThat(testSuite6.getTestCases().get(0).getStatus()).isEqualTo(Status.PASSED);

        TestSuiteResult testSuite7 = AllureTestUtils.getLatestTestSuite(DataDrivenTest.TEST_SUITE_7);
        Assertions.assertThat(AllureTestUtils.getTestCaseNames(testSuite7)).containsOnly(DataDrivenTest.TEST_CASE_1, DataDrivenTest.TEST_CASE_2);
        Assertions.assertThat(AllureTestUtils.getTestStepNames(testSuite7, DataDrivenTest.TEST_CASE_1)).containsOnly(TEST_STEP_TITLE_1A, ASSERTION_FAILED_TITLE);
        assertThat(testSuite7.getTestCases().get(0).getSteps().get(1).getStatus()).isEqualTo(Status.FAILED);
        assertThat(testSuite7.getTestCases().get(0).getStatus()).isEqualTo(Status.FAILED);
        Assertions.assertThat(AllureTestUtils.getTestStepNames(testSuite7, DataDrivenTest.TEST_CASE_2)).containsOnly(TEST_STEP_TITLE_1B, ASSERTION_FAILED_TITLE);
        assertThat(testSuite7.getTestCases().get(1).getSteps().get(1).getStatus()).isEqualTo(Status.FAILED);
        assertThat(testSuite7.getTestCases().get(1).getStatus()).isEqualTo(Status.FAILED);

        TestSuiteResult testSuite8 = AllureTestUtils.getLatestTestSuite(DataDrivenTest.TEST_SUITE_8);
        TestCaseResult testCase = getLast(testSuite8.getTestCases());
        List<Attachment> attachments = AllureTestUtils.getAttachmentsByType(testCase, SvgExporter.SVG_MIME);
        assertThat(attachments.get(0).getTitle().contains("testAttachments"));

        TestSuiteResult rootSuite = AllureTestUtils.getLatestTestSuite(ROOT_SUITE);
        Assertions.assertThat(AllureTestUtils.getTestCaseNames(rootSuite)).contains(DataDrivenTest.TEST_CASE_1, DataDrivenTest.TEST_CASE_2);


        TestSuiteResult testSuite9 = AllureTestUtils.getLatestTestSuite(DataDrivenTest.TEST_SUITE_9);
        Assertions.assertThat(AllureTestUtils.getTestCaseNames(testSuite9)).containsOnly(DataDrivenTest.TEST_CASE_3, TEST_CASE_NOT_STARTED);
        assertThat(testSuite9.getTestCases().get(0).getStatus()).isEqualTo(Status.BROKEN);
        assertThat(testSuite9.getTestCases().get(1).getStatus()).isEqualTo(Status.PASSED);

        TestSuiteResult testSuite10 = AllureTestUtils.getLatestTestSuite(DataDrivenTest.TEST_SUITE_10);
        Assertions.assertThat(AllureTestUtils.getTestCaseNames(testSuite10)).containsOnly(TEST_CASE_NOT_STARTED);
        assertThat(testSuite10.getTestCases().get(0).getStatus()).isEqualTo(Status.BROKEN);

    }
}
