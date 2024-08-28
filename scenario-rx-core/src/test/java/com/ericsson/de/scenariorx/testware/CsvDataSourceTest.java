package com.ericsson.de.scenariorx.testware;

import static com.ericsson.de.scenariorx.api.RxApiImpl.flow;
import static com.ericsson.de.scenariorx.api.RxApiImpl.scenario;
import static com.ericsson.de.scenariorx.impl.Api.fromCsv;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Named;
import java.util.Stack;

import com.ericsson.de.scenariorx.api.RxApiImpl;
import com.ericsson.de.scenariorx.api.RxDataRecord;
import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.impl.ScenarioTest;
import org.junit.Test;

public class CsvDataSourceTest extends ScenarioTest {

    private static final Stack<String> STACK = new Stack<>();

    @Test
    public void csvDataRecords() throws Exception {
        RxScenario scenario = scenario()
                .addFlow(flow()
                        .addTestStep(new InlineInvocation() {
                            @SuppressWarnings("unused")
                            public void testStep(@Named("username") String username) {
                                STACK.push(username);
                            }
                        })
                        .withDataSources(
                                fromCsv("testDs", "csv/fromCsvTest.csv", RxDataRecord.class)
                        )
                )
                .build();

        RxApiImpl.run(scenario);

        assertThat(STACK).containsExactly("John", "Mike");
    }
}
