package com.ericsson.de.scenariorx.testware;

import static com.ericsson.de.scenariorx.api.RxApiImpl.flow;
import static com.ericsson.de.scenariorx.api.RxApiImpl.runner;
import static com.ericsson.de.scenariorx.api.RxApiImpl.scenario;
import static com.ericsson.de.scenariorx.impl.Api.fromIterable;
import static com.ericsson.de.scenariorx.impl.ScenarioTest.named;
import static com.ericsson.de.scenariorx.impl.ScenarioTest.nop;
import static com.ericsson.de.scenariorx.impl.ScenarioTest.numbers;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.log4j.Logger.getLogger;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Named;
import java.io.ByteArrayOutputStream;

import com.ericsson.de.scenariorx.api.RxApiImpl;
import com.ericsson.de.scenariorx.api.RxDataSource;
import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.api.RxScenarioRunner;
import com.ericsson.de.scenariorx.impl.DebugLogScenarioListener;
import com.ericsson.de.scenariorx.impl.ScenarioTest.InlineInvocation;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.assertj.core.api.AbstractObjectArrayAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DebugLogTest {

    private ByteArrayOutputStream debugOutput = new ByteArrayOutputStream();
    private WriterAppender debugAppender = new WriterAppender(
            new PatternLayout("[%t] [%p] %m\n"),
            debugOutput
    );

    private RxDataSource<Integer> numbers = fromIterable("numbers", newArrayList(1, 2, 3)).shared();
    private RxDataSource<String> letters = fromIterable("letters", newArrayList("A", "B", "C")).shared();

    private RxScenarioRunner runner = runner().withDebugLogEnabled().build();

    @Before
    public void setUp() throws Exception {
        getLogger(DebugLogScenarioListener.class).addAppender(debugAppender);
    }

    @After
    public void tearDown() throws Exception {
        getLogger(DebugLogScenarioListener.class).removeAppender(debugAppender);
    }

    @Test
    public void noDebug() throws Exception {
        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(nop())
                )
                .build();

        RxApiImpl.run(scenario);

        assertThat(debugOutput.toString()).isEmpty();
    }

    @Test
    public void empty() throws Exception {
        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(nop())
                )
                .build();

        runner.run(scenario);

        assertThatDebugOutput().containsExactly(
                "[main] [INFO] Running Scenario 'Scenario'",
                "[main] [INFO] Running Flow 'fork' without Data Source",
                "[Scenario.fork.vUser-1.1] [INFO] Running Test Step 'No Operation' with Data Record [{},{}] and context {}"
        );
    }

    @Test
    public void steps() throws Exception {
        RxScenario scenario = scenario("scenario")
                .addFlow(flow("flow1")
                        .addTestStep(named("step11"))
                        .addTestStep(named("step12"))
                        .addTestStep(named("step13"))
                )
                .addFlow(flow("flow2")
                        .addTestStep(named("step21"))
                        .addTestStep(named("step22"))
                        .addTestStep(named("step23"))
                )
                .build();

        runner.run(scenario);

        assertThatDebugOutput().containsExactly(
                "[main] [INFO] Running Scenario 'scenario'",
                "[main] [INFO] Running Flow 'flow1' without Data Source",
                "[scenario.flow1.vUser-1.1] [INFO] Running Test Step 'step11' with Data Record [{},{}] and context {}",
                "[scenario.flow1.vUser-1.1] [INFO] Running Test Step 'step12' with Data Record [{},{}] and context {}",
                "[scenario.flow1.vUser-1.1] [INFO] Running Test Step 'step13' with Data Record [{},{}] and context {}",
                "[main] [INFO] Running Flow 'flow2' without Data Source",
                "[scenario.flow2.vUser-1.1] [INFO] Running Test Step 'step21' with Data Record [{},{}] and context {}",
                "[scenario.flow2.vUser-1.1] [INFO] Running Test Step 'step22' with Data Record [{},{}] and context {}",
                "[scenario.flow2.vUser-1.1] [INFO] Running Test Step 'step23' with Data Record [{},{}] and context {}"
        );
    }

    @Test
    public void subFlows() throws Exception {
        RxScenario scenario = scenario("scenario")
                .addFlow(flow("flow1")
                        .addTestStep(named("step1"))
                        .addSubFlow(flow("flow2")
                                .addTestStep(named("step2"))
                                .addSubFlow(flow("flow3")
                                        .addTestStep(named("step3"))
                                )
                        )
                )
                .build();

        runner.run(scenario);

        assertThatDebugOutput().containsExactly(
                "[main] [INFO] Running Scenario 'scenario'",
                "[main] [INFO] Running Flow 'flow1' without Data Source",
                "[scenario.flow1.vUser-1.1] [INFO] Running Test Step 'step1' with Data Record [{},{}] and context {}",
                "[main] [INFO] Running Flow 'flow2' without Data Source",
                "[scenario.flow1.flow2.vUser-1.1.1] [INFO] Running Test Step 'step2' with Data Record [{},[{},{}]] and context {}",
                "[main] [INFO] Running Flow 'flow3' without Data Source",
                "[scenario.flow1.flow2.flow3.vUser-1.1.1.1] [INFO] Running Test Step 'step3' with Data Record [{},[{},[{},{}]]] and context {}"
        );
    }

    @Test
    public void vUsers() throws Exception {
        RxDataSource<Integer> numbers = fromIterable("numbers", newArrayList(1, 2, 3)).shared();

        RxScenario scenario = scenario("scenario")
                .addFlow(flow("flow")
                        .addTestStep(named("step"))
                        .withDataSources(numbers)
                        .withVUsersAuto()
                )
                .build();

        runner.run(scenario);

        assertThatDebugOutput().containsExactlyInAnyOrder(
                "[main] [INFO] Running Scenario 'scenario'",
                "[main] [INFO] Running Flow 'flow' with shared Data Source 'numbers' consisting of 3 Data Records: " +
                        "[[{\"numbers\":{\"numbers\":1}},{}], [{\"numbers\":{\"numbers\":2}},{}], [{\"numbers\":{\"numbers\":3}},{}]]",
                "[scenario.flow.vUser-1.1] [INFO] Running Test Step 'step' with Data Record [{\"numbers\":{\"numbers\":1}},{}] and context {}",
                "[scenario.flow.vUser-1.2] [INFO] Running Test Step 'step' with Data Record [{\"numbers\":{\"numbers\":2}},{}] and context {}",
                "[scenario.flow.vUser-1.3] [INFO] Running Test Step 'step' with Data Record [{\"numbers\":{\"numbers\":3}},{}] and context {}"
        );
    }

    @Test
    public void dataSources_single() throws Exception {
        RxScenario scenario = scenario("scenario")
                .addFlow(flow("flow")
                        .addTestStep(named("step"))
                        .withDataSources(numbers)
                )
                .build();

        runner.run(scenario);

        assertThatDebugOutput().containsExactly(
                "[main] [INFO] Running Scenario 'scenario'",
                "[main] [INFO] Running Flow 'flow' with shared Data Source 'numbers' consisting of 3 Data Records: " +
                        "[[{\"numbers\":{\"numbers\":1}},{}], [{\"numbers\":{\"numbers\":2}},{}], [{\"numbers\":{\"numbers\":3}},{}]]",
                "[scenario.flow.vUser-1.1] [INFO] Running Test Step 'step' with Data Record [{\"numbers\":{\"numbers\":1}},{}] and context {}",
                "[scenario.flow.vUser-1.1] [INFO] Running Test Step 'step' with Data Record [{\"numbers\":{\"numbers\":2}},{}] and context {}",
                "[scenario.flow.vUser-1.1] [INFO] Running Test Step 'step' with Data Record [{\"numbers\":{\"numbers\":3}},{}] and context {}"
        );
    }

    @Test
    public void dataSources_multiple_subflows() throws Exception {
        RxScenario scenario = scenario("scenario")
                .addFlow(flow("flow")
                        .addSubFlow(flow("subflow")
                                .addTestStep(named("step"))
                                .withDataSources(numbers)
                        )
                        .withDataSources(letters)
                        .withVUsersAuto()
                )
                .build();

        runner.run(scenario);

        assertThatDebugOutput().containsExactlyInAnyOrder(
                "[main] [INFO] Running Scenario 'scenario'",
                "[main] [INFO] Running Flow 'flow' with shared Data Source 'letters' consisting of 3 Data Records: " +
                        "[[{\"letters\":{\"letters\":\"A\"}},{}], [{\"letters\":{\"letters\":\"B\"}},{}], [{\"letters\":{\"letters\":\"C\"}},{}]]",
                "[main] [INFO] Running Flow 'subflow' with shared Data Source 'numbers' consisting of 3 Data Records: " +
                        "[[{\"numbers\":{\"numbers\":1}},[{\"letters\":{\"letters\":\"A\"}},{}]], [{\"numbers\":{\"numbers\":2}},[{\"letters\":{\"letters\":\"B\"}},{}]], [{\"numbers\":{\"numbers\":3}},[{\"letters\":{\"letters\":\"C\"}},{}]]]",
                "[scenario.flow.subflow.vUser-1.1.1] [INFO] Running Test Step 'step' with Data Record [{\"numbers\":{\"numbers\":1}},[{\"letters\":{\"letters\":\"A\"}},{}]] and context {}",
                "[scenario.flow.subflow.vUser-1.2.1] [INFO] Running Test Step 'step' with Data Record [{\"numbers\":{\"numbers\":2}},[{\"letters\":{\"letters\":\"B\"}},{}]] and context {}",
                "[scenario.flow.subflow.vUser-1.3.1] [INFO] Running Test Step 'step' with Data Record [{\"numbers\":{\"numbers\":3}},[{\"letters\":{\"letters\":\"C\"}},{}]] and context {}"
        );
    }

    @Test
    public void dataSources_multiple_sameFlow() throws Exception {
        RxScenario scenario = scenario("scenario")
                .addFlow(flow("flow")
                        .addTestStep(named("step"))
                        .withDataSources(numbers, letters)
                        .withVUsersAuto()
                )
                .build();

        runner.run(scenario);

        assertThatDebugOutput().containsExactlyInAnyOrder(
                "[main] [INFO] Running Scenario 'scenario'",
                "[main] [INFO] Running Flow 'flow' with multiple Data Sources consisting of 3 Data Records: " +
                        "[[[{\"numbers\":{\"numbers\":1}},{}], [{\"letters\":{\"letters\":\"A\"}},{}]], " +
                        "[[{\"numbers\":{\"numbers\":2}},{}], [{\"letters\":{\"letters\":\"B\"}},{}]], " +
                        "[[{\"numbers\":{\"numbers\":3}},{}], [{\"letters\":{\"letters\":\"C\"}},{}]]]",
                "[scenario.flow.vUser-1.1] [INFO] Running Test Step 'step' with Data Record [[{\"numbers\":{\"numbers\":1}},{}], [{\"letters\":{\"letters\":\"A\"}},{}]] and context {}",
                "[scenario.flow.vUser-1.2] [INFO] Running Test Step 'step' with Data Record [[{\"numbers\":{\"numbers\":2}},{}], [{\"letters\":{\"letters\":\"B\"}},{}]] and context {}",
                "[scenario.flow.vUser-1.3] [INFO] Running Test Step 'step' with Data Record [[{\"numbers\":{\"numbers\":3}},{}], [{\"letters\":{\"letters\":\"C\"}},{}]] and context {}"
        );
    }

    @Test
    public void dataSources_large() throws Exception {
        RxDataSource<Integer> large = fromIterable("large", numbers(20));

        RxScenario scenario = scenario("scenario")
                .addFlow(flow("flow")
                        .addTestStep(named("step"))
                        .withDataSources(large)
                )
                .build();

        runner.run(scenario);

        assertThatDebugOutput().contains(
                "[main] [INFO] Running Scenario 'scenario'",
                "[main] [INFO] Running Flow 'flow' with Data Source 'large' consisting of 20 Data Records: " +
                        "[[{\"large\":{\"large\":1}},{}], [{\"large\":{\"large\":2}},{}], [{\"large\":{\"large\":3}},{}], [{\"large\":{\"large\":4}},{}], [{\"large\":{\"large\":5}},{}], " +
                        "[{\"large\":{\"large\":6}},{}], [{\"large\":{\"large\":7}},{}], [{\"large\":{\"large\":8}},{}], [{\"large\":{\"large\":9}},{}], [{\"large\":{\"large\":10}},{}]...]"
        );
    }

    @Test
    public void context() throws Exception {
        final String dataSourceName = "numbers";
        RxDataSource<Integer> numbers = fromIterable(dataSourceName, newArrayList(1, 2, 3));

        RxScenario scenario = scenario("scenario")
                .addFlow(flow("flow")
                        .addTestStep(new InlineInvocation() {
                            @SuppressWarnings("unused")
                            Integer producer(@Named(dataSourceName) Integer number) {
                                return number;
                            }
                        })
                        .addTestStep(new InlineInvocation() {
                            @SuppressWarnings("unused")
                            void consumer(@Named("producer") Integer number) {
                            }
                        })
                        .withDataSources(numbers)
                )
                .build();

        runner.run(scenario);

        assertThatDebugOutput().containsExactly(
                "[main] [INFO] Running Scenario 'scenario'",
                "[main] [INFO] Running Flow 'flow' with Data Source 'numbers' consisting of 3 Data Records: " +
                        "[[{\"numbers\":{\"numbers\":1}},{}], [{\"numbers\":{\"numbers\":2}},{}], [{\"numbers\":{\"numbers\":3}},{}]]",
                "[scenario.flow.vUser-1.1] [INFO] Running Test Step 'producer' with Data Record [{\"numbers\":{\"numbers\":1}},{}] and context {}",
                "[scenario.flow.vUser-1.1] [INFO] Running Test Step 'consumer' with Data Record [{\"numbers\":{\"numbers\":1}},{}] and context {producer=1}",
                "[scenario.flow.vUser-1.1] [INFO] Running Test Step 'producer' with Data Record [{\"numbers\":{\"numbers\":2}},{}] and context {}",
                "[scenario.flow.vUser-1.1] [INFO] Running Test Step 'consumer' with Data Record [{\"numbers\":{\"numbers\":2}},{}] and context {producer=2}",
                "[scenario.flow.vUser-1.1] [INFO] Running Test Step 'producer' with Data Record [{\"numbers\":{\"numbers\":3}},{}] and context {}",
                "[scenario.flow.vUser-1.1] [INFO] Running Test Step 'consumer' with Data Record [{\"numbers\":{\"numbers\":3}},{}] and context {producer=3}"
        );
    }

    private AbstractObjectArrayAssert<?, String> assertThatDebugOutput() {
        return assertThat(debugOutput.toString().split("\\n"));
    }
}
