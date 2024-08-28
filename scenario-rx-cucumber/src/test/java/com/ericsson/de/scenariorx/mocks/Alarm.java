package com.ericsson.de.scenariorx.mocks;

/*
 * COPYRIGHT Ericsson (c) 2015.
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */

import com.ericsson.de.scenariorx.api.RxDataRecord;

public interface Alarm extends RxDataRecord {
    String NAME = "name";
    String STATUS = "status";

    String getName();

    String getStatus();

}
