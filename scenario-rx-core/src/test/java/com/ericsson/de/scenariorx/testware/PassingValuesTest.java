package com.ericsson.de.scenariorx.testware;

/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */

import static com.ericsson.de.scenariorx.api.RxApiImpl.contextDataSource;
import static com.ericsson.de.scenariorx.api.RxApiImpl.flow;
import static com.ericsson.de.scenariorx.api.RxApiImpl.scenario;
import static com.ericsson.de.scenariorx.impl.Api.fromIterable;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Named;
import java.util.List;
import java.util.Stack;

import com.ericsson.de.scenariorx.Node;
import com.ericsson.de.scenariorx.api.RxApiImpl;
import com.ericsson.de.scenariorx.api.RxBasicDataRecord;
import com.ericsson.de.scenariorx.api.RxContextDataSource;
import com.ericsson.de.scenariorx.api.RxDataRecord;
import com.ericsson.de.scenariorx.api.RxDataSource;
import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.api.RxTestStep;
import com.ericsson.de.scenariorx.api.ScenarioContext;
import com.ericsson.de.scenariorx.impl.ScenarioTest;
import org.junit.Test;

public class PassingValuesTest extends ScenarioTest {

    private static final String DATA_SOURCE = "dataSource";
    private static final String FIELD = "field";
    private static final String DIFFERENT_NAME = "differentName";

    @Test
    public void passValuesBetweenSteps() throws Exception {
        Counter counter = new Counter();

        final RxTestStep stringProducer = stringProducer();

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(stringProducer)
                        .addTestStep(new InlineInvocation() {
                            @SuppressWarnings("unused")
                            void consumer1(@Named(DATA_SOURCE) Integer fromDataSource,
                                           @Named(ScenarioContext.CONTEXT_RECORD_NAME) ScenarioContext context,
                                           @Named("stringProducer") String fromProducer) {
                                assertThat(fromProducer).isEqualTo("return" + fromDataSource);
                                assertThat(context.getFieldValue(stringProducer.getName())).isEqualTo("return" + fromDataSource);
                            }
                        })
                        .addSubFlow(flow()
                                .addTestStep(new InlineInvocation() {
                                    @SuppressWarnings("unused")
                                    void consumer2(@Named(DATA_SOURCE) Integer fromDataSource,
                                                   @Named(ScenarioContext.CONTEXT_RECORD_NAME) ScenarioContext context,
                                                   @Named("stringProducer") String fromProducer) {
                                        assertThat(fromProducer).isEqualTo("return" + fromDataSource);
                                        assertThat(context.getFieldValue(stringProducer.getName())).isEqualTo("return" + fromDataSource);
                                    }
                                })
                        )
                        .addTestStep(counter)
                        .withVUsers(2)
                        .withDataSources(fromIterable(DATA_SOURCE, asList(1, 2)).shared())
                )
                .build();

        RxApiImpl.run(scenario);

        counter.assertEqualTo(2);
    }

    @Test
    public void passValuesBetweenStepsDifferentName() throws Exception {
        Stack<String> stack = new Stack<>();

        final RxTestStep stringProducer = stringProducer()
                .withParameter(DATA_SOURCE).value(6);

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(stringProducer)
                        .addTestStep(
                                consumerToStack(stack)
                                        .withParameter(FIELD).fromTestStepResult(stringProducer)
                        )
                )
                .build();

        RxApiImpl.run(scenario);

        assertThat(stack).containsExactlyInAnyOrder("return6");
    }

    @Test
    public void passDataRecordBetweenStepsDifferentName() throws Exception {
        Stack<String> stack = new Stack<>();

        final RxTestStep producer = dataRecordProducer();

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(producer)
                        .addTestStep(
                                consumerToStackDifferentName(stack)
                                        .withParameter(DIFFERENT_NAME).fromTestStepResult(producer, FIELD)
                        )
                )
                .build();

        RxApiImpl.run(scenario);

        assertThat(stack).containsExactlyInAnyOrder("value");
    }

    @Test
    public void testScopeOfContext() throws Exception {
        Counter counter = new Counter();

        RxScenario scenario = scenario()
                .withParameter("scenarioParam", "scenarioParam")
                .addFlow(flow()
                        .addTestStep(new InlineInvocation() {
                            @SuppressWarnings("unused")
                            RxDataRecord producer() {
                                return RxBasicDataRecord.builder()
                                        .setField("flowParam", "beforeSubflowCreated")
                                        .setField("scenarioParam", "flowOverride")
                                        .build();
                            }
                        })
                        .addSubFlow(flow()
                                .addTestStep(new InlineInvocation() {
                                    @SuppressWarnings("unused")
                                    RxDataRecord subFlowProducer() {
                                        return RxBasicDataRecord.builder()
                                                .setField("subFlowParam1", "subFlowParam1")
                                                .setField("subFlowParam2", "subFlowParam2")
                                                .setField("flowParam", "override")
                                                .build();
                                    }
                                })
                                .addTestStep(new InlineInvocation() {
                                    @SuppressWarnings("unused")
                                    void subFlowConsumer(
                                            @Named("scenarioParam") String scenarioParam,
                                            @Named("flowParam") String flowParam,
                                            @Named("subFlowParam1") String subFlowParam1,
                                            @Named("subFlowParam2") String subFlowParam2) {

                                        assertThat(scenarioParam).isEqualTo("flowOverride");
                                        assertThat(flowParam).isEqualTo("override");
                                        assertThat(subFlowParam1).isEqualTo("subFlowParam1");
                                        assertThat(subFlowParam2).isEqualTo("subFlowParam2");
                                    }
                                })
                        )
                        .addTestStep(new InlineInvocation() {
                            @SuppressWarnings("unused")
                            RxDataRecord flowOverride() {
                                return RxBasicDataRecord.builder()
                                        .setField("flowParam", "afterSubflowCreated")
                                        .build();
                            }
                        })
                        .addTestStep(new InlineInvocation() {
                            @SuppressWarnings("unused")
                            void flowConsumer(
                                    @Named("scenarioParam") String scenarioParam,
                                    @Named("flowParam") String flowParam) {

                                assertThat(scenarioParam).isEqualTo("flowOverride");
                                assertThat(flowParam).isEqualTo("afterSubflowCreated");
                            }
                        })
                        .addTestStep(counter)
                )
                .build();

        RxApiImpl.run(scenario);

        counter.assertEqualTo(1);
    }

    @Test
    public void testScopeOfContextDataSource() throws Exception {
        Counter counter = new Counter();

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(producerFromDataSource())
                        .addTestStep(new InlineInvocation() {
                            @SuppressWarnings("unused")
                            void consumer(@Named(DATA_SOURCE) Integer fromDataSource,
                                          @Named(ScenarioContext.CONTEXT_RECORD_NAME) ScenarioContext context) {

                                assertThat(context.getFieldValue(FIELD)).isEqualTo("value" + fromDataSource);
                            }
                        })
                        .addTestStep(counter)
                        .withDataSources(fromIterable(DATA_SOURCE, asList(1, 2))))
                .build();

        RxApiImpl.run(scenario);

        counter.assertEqualTo(2);
    }

    @Test
    public void returnMultipleDataRecords() throws Exception {
        Counter counter = new Counter();

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(new InlineInvocation() {
                            @SuppressWarnings("unused")
                            Object producer() {
                                return asList(
                                        RxBasicDataRecord.builder()
                                                .setField("name1", "value1")
                                                .build(),
                                        RxBasicDataRecord.builder()
                                                .setField("name2", "value2")
                                                .build(),
                                        RxBasicDataRecord.builder()
                                                .setField("name2", "override")
                                                .build()
                                );
                            }
                        })
                        .addTestStep(new InlineInvocation() {
                            @SuppressWarnings("unused")
                            void consumer1(@Named("name1") String name1,
                                           @Named("name2") String name2) {

                                assertThat(name1).isEqualTo("value1");
                                assertThat(name2).isEqualTo("override");
                            }
                        })
                        .addTestStep(counter)
                )
                .build();

        RxApiImpl.run(scenario);

        counter.assertEqualTo(1);
    }

    @Test
    public void passDataRecordBetweenFlows() throws Exception {
        final Stack<String> stack = new Stack<>();

        RxContextDataSource<RxDataRecord> contextDataSource = contextDataSource("name", RxDataRecord.class);

        RxScenario scenario = scenario()
                .addFlow(flow("producerFromDataSource")
                        .addTestStep(
                                producerFromDataSource()
                                        .collectResultsToDataSource(contextDataSource)
                        )
                        .withVUsers(2)
                        .withDataSources(fromIterable(DATA_SOURCE, asList(1, 2)).shared())

                )
                .addFlow(flow("consumer")
                        .addTestStep(new InlineInvocation() {
                            @SuppressWarnings("unused")
                            void consumer1(@Named(FIELD) String field) {
                                stack.add(field);
                            }
                        })
                        .withVUsers(2)
                        .withDataSources(contextDataSource.shared())
                )
                .build();

        RxApiImpl.run(scenario);

        assertThat(stack).containsExactlyInAnyOrder("value1", "value2");
    }

    @Test
    public void passMultipleDataRecordsBetweenFlows() throws Exception {
        final Stack<String> stack = new Stack<>();

        RxContextDataSource<RxDataRecord> contextDataSource = contextDataSource("name", RxDataRecord.class);

        RxScenario scenario = scenario()
                .addFlow(flow("producerFromDataSource")
                        .addTestStep(
                                new InlineInvocation() {
                                    @SuppressWarnings("unused")
                                    List<RxDataRecord> multiProducer(@Named(DATA_SOURCE) Integer fromDataSource) {
                                        List<RxDataRecord> hostsWithVUsers = newArrayList();
                                        for (int i = fromDataSource * 3; i < (fromDataSource + 1) * 3; i++) {
                                            hostsWithVUsers.add(RxBasicDataRecord.builder()
                                                    .setField(FIELD, "value" + i)
                                                    .build());
                                        }

                                        return hostsWithVUsers;
                                    }
                                }.collectResultsToDataSource(contextDataSource)
                        )
                        .withVUsers(2)
                        .withDataSources(fromIterable(DATA_SOURCE, asList(0, 1)).shared())
                )
                .addFlow(flow("consumer")
                        .addTestStep(consumerToStack(stack))
                        .withVUsers(3)
                        .withDataSources(contextDataSource.shared())
                )
                .build();

        RxApiImpl.run(scenario);

        assertThat(stack).containsExactlyInAnyOrder("value0", "value1", "value2", "value3", "value4", "value5");
    }

    @Test
    public void passObjectBetweenFlows() throws Exception {
        final Stack<String> stack = new Stack<>();

        RxContextDataSource<RxDataRecord> contextDataSource = contextDataSource("name", RxDataRecord.class);

        RxTestStep stringProducer = stringProducer()
                .collectResultsToDataSource(contextDataSource);


        RxScenario scenario = scenario()
                .addFlow(flow("producerFromDataSource")
                        .addTestStep(
                                stringProducer
                        )
                        .withVUsers(2)
                        .withDataSources(fromIterable(DATA_SOURCE, asList(1, 2)).shared())

                )
                .addFlow(flow("consumer")
                        .addTestStep(
                                consumerToStack(stack)
                                        .withParameter(FIELD).fromTestStepResult(stringProducer)
                        )
                        .withVUsers(2)
                        .withDataSources(contextDataSource.shared())
                )
                .build();

        RxApiImpl.run(scenario);

        assertThat(stack).containsExactlyInAnyOrder("return1", "return2");
    }

    @Test(expected = IllegalStateException.class)
    public void passDataBetweenFlows_oneFlowEmpty() throws Exception {
        RxContextDataSource<RxDataRecord> contextDataSource = contextDataSource("name", RxDataRecord.class);

        RxScenario scenario = scenario()
                .addFlow(flow("consumer")
                        .addTestStep(new InlineInvocation() {
                            @SuppressWarnings("unused")
                            void consumer1(@Named(FIELD) String field) {
                                throw new IllegalStateException("Should not be executed.");
                            }
                        })
                        .withDataSources(contextDataSource)
                )
                .build();

        RxApiImpl.run(scenario);
    }

    @Test
    public void passToDifferentName() throws Exception {
        final Stack<String> stack = new Stack<>();

        RxScenario scenario = scenario()
                .addFlow(flow("producerFromDataSource")
                        .addTestStep(
                                dataRecordProducer()
                        )
                        .addTestStep(
                                consumerToStackDifferentName(stack)
                                        .withParameter(DIFFERENT_NAME).bindTo(FIELD)
                        )
                )
                .build();

        RxApiImpl.run(scenario);

        assertThat(stack).containsExactlyInAnyOrder("value");
    }

    @Test
    public void overrideTest() throws Exception {
        final Stack<String> stack = new Stack<>();

        RxScenario scenario = scenario()
                .addFlow(flow("producerFromDataSource")
                        .addTestStep(
                                new InlineInvocation() {
                                    @SuppressWarnings("unused")
                                    RxDataRecord producer1() {
                                        return RxBasicDataRecord.builder()
                                                .setField(FIELD, "value1")
                                                .build();
                                    }
                                }
                        )
                        .addTestStep(
                                new InlineInvocation() {
                                    @SuppressWarnings("unused")
                                    RxDataRecord producer2() {
                                        return RxBasicDataRecord.builder()
                                                .setField(FIELD, "value2")
                                                .build();
                                    }
                                }
                        )
                        .addTestStep(
                                consumerToStack(stack)
                        )
                        .addTestStep(
                                consumerToStack(stack)
                                        .withParameter(FIELD).bindTo("producer1.field")
                        )
                )
                .build();

        RxApiImpl.run(scenario);

        assertThat(stack).containsExactlyInAnyOrder("value2", "value1");
    }

    @Test
    public void bindInputToDifferentName() throws Exception {
        Stack<String> stack1 = new Stack<>();
        Stack<String> stack2 = new Stack<>();
        Stack<String> stack3 = new Stack<>();

        RxDataSource<Node> nodeDataSource = getNodeDataSource();
        RxDataSource<String> iterableDataSource = fromIterable("numbers", asList("a", "b"));

        final String PARAM_NAME = "param";
        final String PARAM_VALUE = "pampam";

        RxScenario scenario = scenario()
                .withParameter(PARAM_NAME, PARAM_VALUE)
                .addFlow(flow()
                        .addTestStep(
                                consumerToStack(stack1)
                                        .withParameter(FIELD).fromDataSource(nodeDataSource, Node.NETWORK_ELEMENT_ID)
                        )
                        .withDataSources(nodeDataSource)

                )
                .addFlow(flow()
                        .addTestStep(
                                consumerToStack(stack2)
                                        .withParameter(FIELD).fromDataSource(iterableDataSource)
                        )
                        .withDataSources(iterableDataSource)

                )
                .addFlow(flow()
                        .addTestStep(
                                consumerToStack(stack3)
                                        .withParameter(FIELD).fromContext(PARAM_NAME)
                        )
                )
                .build();

        RxApiImpl.run(scenario);

        assertThat(stack1).containsExactlyInAnyOrder("SGSN-MME", "SGSN-14B");
        assertThat(stack2).containsExactlyInAnyOrder("a", "b");
        assertThat(stack3).containsExactlyInAnyOrder(PARAM_VALUE);
    }

    @Test
    public void bindDataSource() throws Exception {
        Stack<String> stack1 = new Stack<>();
        Stack<String> stack2 = new Stack<>();
        Stack<String> stack3 = new Stack<>();

        RxDataSource<Node> nodeDataSource = getNodeDataSource();
        RxDataSource<String> iterableDataSource = fromIterable("iterable", asList("a", "b"));

        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(
                                dataRecordToStackDifferentName(stack1)
                        )
                        .withDataSources(nodeDataSource.rename(DIFFERENT_NAME))

                )
                .addFlow(flow()
                        .addTestStep(
                                dataRecordFieldToStackDifferentName(stack2)
                        )
                        .withDataSources(nodeDataSource.rename(DIFFERENT_NAME))

                )
                .addFlow(flow()
                        .addTestStep(
                                consumerToStackDifferentName(stack3)
                        )
                        .withDataSources(iterableDataSource.rename(DIFFERENT_NAME))

                )
                .build();

        RxApiImpl.run(scenario);

        assertThat(stack1).containsExactlyInAnyOrder("SGSN-MME", "SGSN-14B");
        assertThat(stack2).containsExactlyInAnyOrder("SGSN-MME", "SGSN-14B");
        assertThat(stack3).containsExactlyInAnyOrder("a", "b");
    }

    private InlineInvocation stringProducer() {
        return new InlineInvocation() {
            @SuppressWarnings("unused")
            Object stringProducer(@Named(DATA_SOURCE) Integer fromDataSource) {
                return "return" + fromDataSource;
            }
        };
    }

    private RxTestStep dataRecordProducer() {
        return new InlineInvocation() {
            @SuppressWarnings("unused")
            RxDataRecord producer() {
                return RxBasicDataRecord.builder()
                        .setField(FIELD, "value")
                        .build();
            }
        };
    }

    private RxTestStep producerFromDataSource() {
        return new InlineInvocation() {
            @SuppressWarnings("unused")
            RxDataRecord producer(@Named(DATA_SOURCE) Integer fromDataSource,
                                  @Named(ScenarioContext.CONTEXT_RECORD_NAME) ScenarioContext context) {

                assertThat(context.getFieldValue(FIELD)).isNull();

                return RxBasicDataRecord.builder()
                        .setField(FIELD, "value" + fromDataSource)
                        .build();
            }
        };
    }

    private RxTestStep consumerToStack(final Stack<String> stack) {
        return new InlineInvocation() {
            @SuppressWarnings("unused")
            void consumerToStack(@Named(FIELD) String value) {
                stack.add(value);
            }
        };
    }

    private RxTestStep consumerToStackDifferentName(final Stack<String> stack) {
        return new InlineInvocation() {
            @SuppressWarnings("unused")
            void consumerToStack(@Named(DIFFERENT_NAME) String value) {
                stack.add(value);
            }
        };
    }

    private RxTestStep dataRecordToStackDifferentName(final Stack<String> stack) {
        return new InlineInvocation() {
            @SuppressWarnings("unused")
            void consumerToStack(@Named(DIFFERENT_NAME) Node node) {
                stack.add(node.getNetworkElementId());
            }
        };
    }

    private RxTestStep dataRecordFieldToStackDifferentName(final Stack<String> stack) {
        return new InlineInvocation() {
            @SuppressWarnings("unused")
            void consumerToStack(@Named(DIFFERENT_NAME + "." + Node.NETWORK_ELEMENT_ID) String nodeId) {
                stack.add(nodeId);
            }
        };
    }
}
