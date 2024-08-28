package com.ericsson.de.scenariorx.impl;

import static com.ericsson.de.scenariorx.api.RxApiImpl.flow;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;
import com.ericsson.de.scenariorx.api.RxFlow;
import com.ericsson.de.scenariorx.impl.FlowExecutionContext.DataRecordsToExecutions;
import com.ericsson.de.scenariorx.impl.Internals.Exec;
import com.ericsson.de.scenariorx.impl.Internals.InternalScenarioContext;
import com.ericsson.de.scenariorx.impl.Internals.VUser;
import com.google.common.collect.Maps;
import org.assertj.core.api.ListAssert;
import org.junit.Test;

public class FlowExecutionContextTest {

    @Test
    public void toExecutions_regular() throws Exception {
        List<Exec> parents = executions("a", "b", "c");
        List<RxDataRecordWrapper> dataRecords = dataRecords("q", "w", "e", "r", "t", "y");

        List<Exec> newExecs = toExecutions(2, parents, 0, dataRecords);

        assertThatExecutionVUserIds(newExecs).containsExactly("1.1", "1.2", "2.1", "2.2", "3.1", "3.2");
    }

    @Test
    public void toExecutions_last() throws Exception {
        List<Exec> parents = executions("a", "b", "c");
        List<RxDataRecordWrapper> dataRecords = dataRecords("q", "w", "e");

        List<Exec> newExecs = toExecutions(2, parents, 0, dataRecords);

        assertThatExecutionVUserIds(newExecs).containsExactly("1.1", "1.2", "2.1");
    }

    @Test
    public void toExecutions_first() throws Exception {
        List<Exec> parents = rootExecutions();
        List<RxDataRecordWrapper> dataRecords = dataRecords("q", "w", "e");

        List<Exec> newExecs = toExecutions(3, parents, 0, dataRecords);

        assertThatExecutionVUserIds(newExecs).containsExactly("1", "2", "3");
    }

    @Test
    public void toExecutions_shift() throws Exception {
        List<Exec> parents = rootExecutions();
        List<RxDataRecordWrapper> dataRecords = dataRecords("q", "w", "e");

        List<Exec> newExecs = toExecutions(3, parents, 10, dataRecords);

        assertThatExecutionVUserIds(newExecs).containsExactly("11", "12", "13");
    }

    private List<Exec> executions(String... values) {
        List<Exec> executions = newArrayListWithCapacity(values.length);
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            executions.add(exec(i + 1, value));
        }
        return executions;
    }

    private List<Exec> rootExecutions() {
        Map<String, Object> parameters = emptyMap();
        Exec exec = Exec.rootExec(parameters);
        return singletonList(exec);
    }

    private Exec exec(int number, String value) {
        VUser vUser = VUser.ROOT.child(number);
        RxDataRecordWrapper dataRecord = ScenarioTest.getDataRecords("ds_name", value);
        InternalScenarioContext context = new InternalScenarioContext(Maps.<String, Object>newHashMap());
        return new Exec("flow", vUser, context, null, dataRecord);
    }

    private List<RxDataRecordWrapper> dataRecords(String... values) {
        List<RxDataRecordWrapper> dataRecords = newArrayListWithCapacity(values.length);
        for (String value : values) {
            dataRecords.add(ScenarioTest.getDataRecords("ds_name", value));
        }
        return dataRecords;
    }

    private List<Exec> toExecutions(int vUsers, List<Exec> parents, int vUserOffset,
                                    List<RxDataRecordWrapper> dataRecords) {
        RxFlow flow = flow("flow").addTestStep(ScenarioTest.nop()).withVUsers(vUsers).build();
        return new DataRecordsToExecutions(flow, parents, vUserOffset).call(dataRecords);
    }

    private ListAssert<Object> assertThatExecutionVUserIds(List<Exec> newExecs) {
        return assertThat(newExecs).extracting("vUser.id");
    }
}