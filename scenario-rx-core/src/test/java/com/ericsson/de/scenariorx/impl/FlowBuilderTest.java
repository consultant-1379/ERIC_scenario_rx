package com.ericsson.de.scenariorx.impl;

import static com.ericsson.de.scenariorx.api.RxApiImpl.flow;
import static com.ericsson.de.scenariorx.impl.Api.fromIterable;
import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;
import com.ericsson.de.scenariorx.api.RxDataSource;
import com.ericsson.de.scenariorx.api.RxExceptionHandler;
import com.ericsson.de.scenariorx.api.RxFlow;
import com.ericsson.de.scenariorx.api.RxFlowBuilder;
import com.ericsson.de.scenariorx.api.RxFlowBuilderInterfaces;
import com.ericsson.de.scenariorx.api.RxTestStep;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import rx.Observable;

public class FlowBuilderTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private RxFlowBuilderInterfaces.Steps<RxFlow> nopFlow = flow().addTestStep(ScenarioTest.nop());

    private RxDataSource dataSource1 = fromIterable("dataSource1", newArrayList("1", "2", "3"));
    private RxDataSource dataSource2 = fromIterable("dataSource2", newArrayList("a", "b", "c"));

    @Test
    public void withVUsers_shouldThrowIllegalStateException_whenCalledMoreThanOnce() throws Exception {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(FlowBuilder.ERROR_V_USERS_NOT_ONCE);

        nopFlow.withVUsers(2).withVUsers(3);
    }

    @Test
    public void withVUsers_shouldThrowIllegalStateException_whenCalledAfter_withVUsersAuto() throws Exception {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(FlowBuilder.ERROR_V_USERS_NOT_ONCE);

        nopFlow.withVUsersAuto().withVUsers(3);
    }

    @Test
    public void withVUsers_shouldThrowIllegalArgumentException_whenVUsers_lessThanZero() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(FlowBuilder.ERROR_V_USERS_NEGATIVE);

        nopFlow.withVUsers(-1);
    }

    @Test
    public void withVUsers_shouldThrowIllegalArgumentException_whenVUsers_zero() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(FlowBuilder.ERROR_V_USERS_NEGATIVE);

        nopFlow.withVUsers(0);
    }

    @Test
    public void withVUsersAuto_shouldThrowIllegalStateException_whenCalledMoreThanOnce() throws Exception {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(FlowBuilder.ERROR_V_USERS_NOT_ONCE);

        nopFlow.withVUsersAuto().withVUsersAuto();
    }

    @Test
    public void withVUsersAuto_shouldThrowIllegalStateException_whenNoDataSources() throws Exception {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(FlowBuilder.ERROR_V_USERS_AUTO_NO_DATA_SOURCES);

        nopFlow.withVUsersAuto().build();
    }

    @Test
    public void withVUsersAuto_shouldThrowIllegalStateException_whenCalledAfter_withVUsers() throws Exception {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(FlowBuilder.ERROR_V_USERS_NOT_ONCE);

        nopFlow.withVUsers(3).withVUsersAuto();
    }

    @Test
    public void withVUsersAuto_shouldThrowIllegalStateException_whenAtLeastOneNonSharedDataSource() throws Exception {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(FlowBuilder.ERROR_V_USERS_AUTO_NON_SHARED +
                FlowBuilder.HINT_NON_SHARED + dataSource2.getName());

        nopFlow.withVUsersAuto()
                .withDataSources(dataSource1.shared(), dataSource2)
                .build();
    }

    @Test
    public void withVUsersAuto_shouldSetVUsers_toNumDataRecords_whenLessThan_vUserThreshold() throws Exception {
        int vUserThreshold = ((FlowBuilder) nopFlow).vUserThreshold();
        int numDataRecords = vUserThreshold - 1;
        RxDataSource<Integer> dataSource = fromIterable("threshold", ScenarioTest.numbers(numDataRecords)).shared();

        Flow flow = nopFlow
                .withVUsersAuto()
                .withDataSources(dataSource)
                .build();

        assertThat(flow.dataSource.vUsers).isEqualTo(numDataRecords);
    }

    @Test
    public void withVUsersAuto_shouldSetVUsers_toVUserThreshold_whenLessThan_NumDataRecords() throws Exception {
        int vUserThreshold = ((FlowBuilder) nopFlow).vUserThreshold();
        int numDataRecords = vUserThreshold + 1;
        RxDataSource<Integer> dataSource = fromIterable("threshold", ScenarioTest.numbers(numDataRecords)).shared();

        Flow flow = nopFlow
                .withVUsersAuto()
                .withDataSources(dataSource)
                .build();

        assertThat(flow.dataSource.vUsers).isEqualTo(vUserThreshold);
    }

    @Test
    public void withVUsersAuto_shouldSetVUsers_toSmallestDataSourceSize_whenMultipleDataSources() throws Exception {
        RxDataSource<Integer> dataSource10 = fromIterable("10", ScenarioTest.numbers(10)).shared();
        RxDataSource<Integer> dataSource20 = fromIterable("20", ScenarioTest.numbers(20)).shared();
        RxDataSource<Integer> dataSource30 = fromIterable("30", ScenarioTest.numbers(30)).shared();

        Flow flow = nopFlow
                .withVUsersAuto()
                .withDataSources(dataSource10, dataSource20, dataSource30)
                .build();

        assertThat(flow.dataSource.vUsers).isEqualTo(10);
    }

    @Test
    public void withDataSources_shouldThrowIllegalStateException_whenCalledMoreThanOnce() throws Exception {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(FlowBuilder.ERROR_DATA_SOURCES_NOT_ONCE);

        nopFlow.withDataSources(dataSource1).withDataSources(dataSource2);
    }

    @Test
    public void withDataSources_shouldThrowNullPointerException_whenDataSourcesArray_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(FlowBuilder.ERROR_DATA_SOURCES_NULL);

        nopFlow.withDataSources((RxDataSource[]) null);
    }

    @Test
    public void withDataSources_shouldThrowIllegalArgumentException_whenDataSourcesArray_empty() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(FlowBuilder.ERROR_DATA_SOURCES_EMPTY);

        nopFlow.withDataSources();
    }

    @Test
    public void withDataSources_shouldThrowNullPointerException_whenSingleDataSource_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(FlowBuilder.ERROR_DATA_SOURCE_NULL);

        nopFlow.withDataSources((RxDataSource) null);
    }

    @Test
    public void withDataSources_shouldThrowNullPointerException_whenFirstDataSource_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(FlowBuilder.ERROR_DATA_SOURCE_NULL);

        nopFlow.withDataSources(null, dataSource2);
    }

    @Test
    public void withDataSources_shouldThrowNullPointerException_whenSecondDataSource_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(FlowBuilder.ERROR_DATA_SOURCE_NULL);

        nopFlow.withDataSources(dataSource1, null);
    }

    @Test
    public void addTestStep_shouldThrowNullPointerException_whenTestStep_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(FlowBuilder.ERROR_TEST_STEP_NULL);

        flow().addTestStep(null);
    }

    @Test
    public void withBefore_shouldThrowIllegalStateException_whenCalledMoreThanOnce() throws Exception {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(FlowBuilder.ERROR_WITH_BEFORE_NOT_ONCE);
        thrown.expectMessage(FlowBuilder.HINT_SINGLE_CALL);

        ((FlowBuilder) flow().withBefore(ScenarioTest.nop()))
                .withBefore(ScenarioTest.nop());
    }

    @Test
    public void withAfter_shouldThrowIllegalStateException_whenCalledMoreThanOnce() throws Exception {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(FlowBuilder.ERROR_WITH_AFTER_NOT_ONCE);
        thrown.expectMessage(FlowBuilder.HINT_SINGLE_CALL);

        ((FlowBuilder) nopFlow.withAfter(ScenarioTest.nop()))
                .withAfter(ScenarioTest.nop());
    }

    @Test
    public void addSubFlow_shouldThrowNullPointerException_whenSubFlowBuilder_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(FlowBuilder.ERROR_SUBFLOW_NULL);

        flow().addSubFlow((RxFlowBuilder) null);
    }

    @Test
    public void addSubFlow_shouldThrowNullPointerException_whenSubFlow_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(FlowBuilder.ERROR_SUBFLOW_NULL);

        flow().addSubFlow((RxFlow) null);
    }

    @Test
    public void split_shouldThrowNullPointerException_whenSubFlowBuilders_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(FlowBuilder.ERROR_SUBFLOWS_NULL);

        flow().split((RxFlowBuilder[]) null);
    }

    @Test
    public void split_shouldThrowNullPointerException_whenOneOfSubFlowBuilders_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(FlowBuilder.ERROR_SUBFLOW_NULL);

        flow().split(nopFlow, null, nopFlow);
    }

    @Test
    public void split_shouldThrowNullPointerException_whenSubFlowBuilder_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(FlowBuilder.ERROR_SUBFLOW_NULL);

        flow().split((FlowBuilder) null);
    }

    @Test
    public void split_shouldThrowNullPointerException_whenOneOfSubFlows_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(FlowBuilder.ERROR_SUBFLOW_NULL);

        RxFlow flow = nopFlow.build();

        flow().split(flow, null, flow);
    }

    @Test
    public void alwaysRun_onTestStep_shouldSetOnlyOneTestStep_asAlwaysRun() throws Exception {
        RxTestStep testStep1 = ScenarioTest.print("Test Step 1");
        RxTestStep testStep2 = ScenarioTest.print("Test Step 2");
        RxTestStep testStep3 = ScenarioTest.print("Test Step 3");

        Flow flow = flow()
                .addTestStep(testStep1)
                .addTestStep(testStep2).alwaysRun()
                .addTestStep(testStep3)
                .build();

        assertThat(testStep1.isAlwaysRun()).isFalse();
        assertThat(testStep2.isAlwaysRun()).isFalse();
        assertThat(testStep3.isAlwaysRun()).isFalse();

        assertThat(RxTestStep.class.cast(flow.testSteps.get(0)).isAlwaysRun()).isFalse();
        assertThat(RxTestStep.class.cast(flow.testSteps.get(1)).isAlwaysRun()).isTrue();
        assertThat(RxTestStep.class.cast(flow.testSteps.get(2)).isAlwaysRun()).isFalse();
    }

    @Test
    public void withExceptionHandler_shouldThrowNullPointerException_whenExceptionHandler_null() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(FlowBuilder.ERROR_EXCEPTION_HANDLER_NULL);

        nopFlow.withExceptionHandler(null);
    }

    @Test
    public void withExceptionHandler_shouldThrowIllegalStateException_whenCalledMoreThanOnce() throws Exception {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(FlowBuilder.ERROR_EXCEPTION_HANDLER_NOT_ONCE);
        thrown.expectMessage(FlowBuilder.HINT_EXCEPTION_HANDLER);

        nopFlow.withExceptionHandler(RxExceptionHandler.PROPAGATE).withExceptionHandler(RxExceptionHandler.IGNORE);
    }

    @Test
    public void build_shouldHaveOneVUser_whenNotSpecifiedDuringBuild() throws Exception {
        Flow flow = nopFlow.build();

        assertThat(flow.dataSource.vUsers).isEqualTo(1);
    }

    @Test
    public void build_shouldHaveOneEmptyDataRecord_whenDataSourceNotSpecifiedDuringBuild() throws Exception {
        Flow flow = nopFlow.build();

        Observable<RxDataRecordWrapper> dataSource = flow.dataSource.getDataSource();
        assertThat(dataSource).isNotNull();
        assertThat(dataSource.count().toBlocking().single()).isEqualTo(1);

        RxDataRecordWrapper onlyRecord = dataSource.toBlocking().single();

        assertThat(onlyRecord.toString()).isEqualTo("{}");
    }

    @Test
    public void build_exceptionHandler_null_byDefault() throws Exception {
        Flow flow = nopFlow.build();

        assertThat(flow.exceptionHandler).isNull();
    }

    @Test
    public void doNotAllowOnlyCyclicDataSources() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("cyclic");

        nopFlow.withDataSources(dataSource1.cyclic(), dataSource2.cyclic()).build();
    }

    @Test
    public void doNotAllowSingleCyclicDataSource() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("cyclic");

        nopFlow.withDataSources(dataSource1.cyclic()).build();
    }
}