package com.ericsson.de.scenariorx.testware;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.ericsson.cifwk.taf.testng.AbstractTestListener;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.TestNG;
import ru.yandex.qatools.allure.commons.AllureFileUtils;
import ru.yandex.qatools.allure.model.Attachment;
import ru.yandex.qatools.allure.model.Step;
import ru.yandex.qatools.allure.model.TestCaseResult;
import ru.yandex.qatools.allure.model.TestSuiteResult;

public class AllureTestUtils {

    public static final String ALLURE_RESULTS = "target/allure-results/";

    public static boolean runTestNg(Class testClass) {
        return runTestNg(null, testClass);
    }

    public static boolean runTestNg(String suiteName, Class... testClasses) {
        Preconditions.checkArgument(testClasses.length > 0, "At least one test class should be provided");
        TestNG testNG = new TestNG(false);
        testNG.setTestClasses(testClasses);
        if (suiteName != null) {
            testNG.setDefaultSuiteName(suiteName);
        }
        return runTestNg(testNG);
    }

    public static boolean runTestNg(String suiteFileName) throws IOException {
        TestNG testNG = new TestNG(false);
        URL resource = AllureTestUtils.class.getResource("/" + suiteFileName);
        Preconditions.checkNotNull(resource, "Suite file %s not found", suiteFileName);
        testNG.setTestSuites(newArrayList(resource.getFile()));
        return runTestNg(testNG);
    }

    private static boolean runTestNg(TestNG testNG) {
        SuccessListener successListener = new SuccessListener();
        testNG.addListener(successListener);
        testNG.run();
        return successListener.isPassed();
    }

    public static TestSuiteResult getLatestTestSuite(final String suiteName) {
        List<TestSuiteResult> suites = getSuiteResults();
        ImmutableList<TestSuiteResult> foundSuites = FluentIterable.from(suites).filter(new Predicate<TestSuiteResult>() {
            @Override
            public boolean apply(TestSuiteResult input) {
                return suiteName.equals(input.getName());
            }
        }).toSortedList(new Comparator<TestSuiteResult>() {
            @Override
            public int compare(TestSuiteResult o1, TestSuiteResult o2) {
                return -new Long(o1.getStop()).compareTo(o2.getStop());
            }
        });
        if (foundSuites.isEmpty()) {
            Collection<String> allSuitesInFolder = Collections2.transform(suites, new Function<TestSuiteResult, String>() {
                @Override
                public String apply(TestSuiteResult suite) {
                    return suite.getName();
                }
            });
            throw new RuntimeException(String.format("Suite '%s' was not found among suites [%s]", suiteName, allSuitesInFolder));
        }
        // multiple suites are possible (e.g. mvn clean wasn't executed before previous execution of the same test)
        return foundSuites.get(0);
    }

    public static List<Attachment> getAttachmentsByType(TestCaseResult testCase, final String mimeType) {
        return FluentIterable.from(testCase.getAttachments()).filter(new Predicate<Attachment>() {
            @Override
            public boolean apply(Attachment input) {
                return mimeType.equals(input.getType());
            }
        }).toList();
    }

    public static List<TestCaseResult> getTestCases(TestSuiteResult testSuite, final String testCaseName) {
        FluentIterable<TestCaseResult> testCases = FluentIterable.from(testSuite.getTestCases()).filter(new Predicate<TestCaseResult>() {
            @Override
            public boolean apply(TestCaseResult input) {
                return input.getName().equals(testCaseName);
            }
        });
        return Arrays.asList(Iterables.toArray(testCases, TestCaseResult.class));
    }

    public static Collection<String> getTestCaseNames(TestSuiteResult testSuite) {
        return transform(testSuite.getTestCases(), new Function<TestCaseResult, String>() {
            @Override
            public String apply(TestCaseResult input) {
                return input.getName();
            }
        });
    }

    public static Collection<String> getTestStepNames(TestSuiteResult testSuite, final String testCaseName) {
        return transform(
                FluentIterable.from(testSuite.getTestCases()).firstMatch(new Predicate<TestCaseResult>() {
                    @Override
                    public boolean apply(TestCaseResult input) {
                        return input.getName().equals(testCaseName);
                    }
                }).get().getSteps(), new Function<Step, String>() {
                    @Override
                    public String apply(Step input) {
                        return input.getTitle();
                    }
                }
        );
    }

    private static List<TestSuiteResult> getSuiteResults() {
        File folder = new File(ALLURE_RESULTS);
        try {
            return AllureFileUtils.unmarshalSuites(folder);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private static class SuccessListener extends AbstractTestListener {

        private static final Logger logger = LoggerFactory.getLogger(SuccessListener.class);

        private boolean passed = true;

        @Override
        public void onTestFailure(ITestResult result) {
            passed = false;
            logError(result);
        }

        @Override
        public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
            passed = false;
            logError(result);
        }

        private void logError(ITestResult result) {
            logger.error("Inner test case failed with exception", result.getThrowable());
        }

        public boolean isPassed() {
            return passed;
        }
    }

    public static class VerySpecialException extends RuntimeException {
    }
}
