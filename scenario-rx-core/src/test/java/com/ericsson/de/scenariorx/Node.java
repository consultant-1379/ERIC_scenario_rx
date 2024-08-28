package com.ericsson.de.scenariorx;

import com.ericsson.de.scenariorx.api.RxDataRecord;

public interface Node extends RxDataRecord {
    String NODE_TYPE = "nodeType";
    String NETWORK_ELEMENT_ID = "networkElementId";
    String PORT = "port";

    String getNodeType();

    String getNetworkElementId();

    Integer getPort();
}
