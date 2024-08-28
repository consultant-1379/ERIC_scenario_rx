/*
 * COPYRIGHT Ericsson (c) 2017.
 *
 *  The copyright to the computer program(s) herein is the property of
 *  Ericsson Inc. The programs may be used and/or copied only with written
 *  permission from Ericsson Inc. or in accordance with the terms and
 *  conditions stipulated in the agreement/contract under which the
 *  program(s) have been supplied.
 */

package com.ericsson.de.scenariorx.impl;

import static com.ericsson.de.scenariorx.impl.Api.fromDataRecords;
import static org.assertj.core.api.Assertions.assertThat;

import com.ericsson.de.scenariorx.Node;
import com.ericsson.de.scenariorx.api.RxDataSource;
import com.google.common.collect.Iterables;
import org.junit.Test;

public class DataSourceStrategyTest extends ScenarioTest {

    @Test(timeout = 10000L)
    public void cyclicDoesNotLoopForever() throws Exception {
        RxDataSource<Node> ds1 = fromDataRecords("ds1",
                getNode("1", "2", 3),
                getNode("1", "2", 3),
                getNode("1", "2", 3)
        );

        RxDataSource<Node> loop = fromDataRecords("loop",
                getNode("1", "2", 3),
                getNode("1", "2", 3)
        ).cyclic();

        RxDataSource<Node> loop2 = fromDataRecords("loop2",
                getNode("1", "2", 3),
                getNode("1", "2", 3)
        ).cyclic();

        RxDataSource<Node> loop3 = fromDataRecords("loop3",
                getNode("1", "2", 3),
                getNode("1", "2", 3)
        ).cyclic();

        RxDataSource<Node> shared = fromDataRecords("shared",
                getNode("1", "2", 3),
                getNode("1", "2", 3),
                getNode("1", "2", 3)
        ).shared();

        DataSourceStrategy multiple = DataSourceStrategy.fromDefinitions(
                new RxDataSource[]{
                        ds1,
                        loop,
                        loop2,
                        shared,
                        loop3

                }, 2
        );

        Integer totalIterationCount = multiple.provide().count().toBlocking().single();
        assertThat(totalIterationCount).isEqualTo(Iterables.size(shared));
    }

    @Test(timeout = 10000L)
    public void cyclicSharedCopied() throws Exception {
        RxDataSource<Node> copied = fromDataRecords("copied",
                getNode("1", "2", 3),
                getNode("1", "2", 3),
                getNode("1", "2", 3),
                getNode("1", "2", 3)
        );

        RxDataSource<Node> cyclic = fromDataRecords("cyclic",
                getNode("1", "2", 3),
                getNode("1", "2", 3)
        ).cyclic();

        RxDataSource<Node> shared = fromDataRecords("shared",
                getNode("1", "2", 3),
                getNode("1", "2", 3),
                getNode("1", "2", 3)
        ).shared();

        DataSourceStrategy multiple = DataSourceStrategy.fromDefinitions(
                new RxDataSource[]{
                        copied,
                        cyclic,
                        shared,
                }, 2
        );

        Integer totalIterationCount = multiple.provide().count().toBlocking().single();
        assertThat(totalIterationCount).isEqualTo(Iterables.size(shared));
    }

    @Test(timeout = 10000L)
    public void cyclicCopied() throws Exception {
        RxDataSource<Node> largerCopied = fromDataRecords("largerCopied",
                getNode("1", "2", 3),
                getNode("1", "2", 3),
                getNode("1", "2", 3),
                getNode("1", "2", 3),
                getNode("1", "2", 3)
        );

        RxDataSource<Node> smallerCopied = fromDataRecords("copied2",
                getNode("1", "2", 3),
                getNode("1", "2", 3),
                getNode("1", "2", 3),
                getNode("1", "2", 3)
        );

        RxDataSource<Node> cyclic = fromDataRecords("cyclic",
                getNode("1", "2", 3),
                getNode("1", "2", 3)
        ).cyclic();


        final int vUsers = 2;
        DataSourceStrategy multiple = DataSourceStrategy.fromDefinitions(
                new RxDataSource[]{
                        largerCopied,
                        smallerCopied,
                        cyclic,
                }, vUsers
        );

        Integer totalIterationCount = multiple.provide().count().toBlocking().single();
        assertThat(totalIterationCount).isEqualTo(Iterables.size(smallerCopied) * vUsers);
    }

}