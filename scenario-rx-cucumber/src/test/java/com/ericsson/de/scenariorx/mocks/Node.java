package com.ericsson.de.scenariorx.mocks;

import com.ericsson.de.scenariorx.api.RxDataRecord;

public interface Node extends RxDataRecord {
    String NODE_TYPE = "nodeType";
    String NETWORK_ELEMENT_ID = "networkElementId";

    String getNodeType();

    String getNetworkElementId();
}
