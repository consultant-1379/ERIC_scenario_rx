package com.ericsson.de.scenariorx.impl;

import static com.ericsson.de.scenariorx.impl.DataSourceRx.copy;
import static com.ericsson.de.scenariorx.impl.DataSourceRx.multiply;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import com.ericsson.de.scenariorx.RxJavaExploratoryTest;
import com.ericsson.de.scenariorx.api.RxDataRecordWrapper;
import org.junit.Test;
import rx.Observable;

public class DataSourceRxTest {

    @Test
    public void flowSubflowDataSources() throws Exception {
        Observable<RxDataRecordWrapper> flowDataSource = newDataSource("1", "2");
        Observable<RxDataRecordWrapper> subFlowDataSource = newDataSource("a", "b", "c");

        multiply(flowDataSource, subFlowDataSource)
                .subscribe(RxJavaExploratoryTest.PrinterAction.INSTANCE);
    }

    @Test
    public void notSharedDataSource() throws Exception {
        int vUsers = 3;
        Observable<RxDataRecordWrapper> notSharedDataSource = newDataSource("a", "b", "c");

        copy(notSharedDataSource, vUsers)
                .buffer(vUsers)
                .subscribe(RxJavaExploratoryTest.PrinterAction.INSTANCE);
    }

    @Test
    public void copyTest() throws Exception {
        int vUsers = 8;
        Observable<RxDataRecordWrapper> notSharedDataSource = newDataSource("1-a-x", "1-a-o", "1-a-x", "1-a-o", "1-a-x", "1-a-o", "1-a-x", "1-a-o");

        copy(notSharedDataSource, 2)
                .buffer(vUsers)
                .subscribe(RxJavaExploratoryTest.PrinterAction.INSTANCE);
    }

    private static Observable<RxDataRecordWrapper> newDataSource(String... strings) {
        List<RxDataRecordWrapper> dataRecords = newArrayList();

        for (String s : strings) {
            dataRecords.add(ScenarioTest.getDataRecords("ds_name", s));
        }

        return Observable.from(dataRecords);
    }
}