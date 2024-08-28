package com.ericsson.de.scenariorx.impl.graph.export;

import com.ericsson.de.scenariorx.impl.graph.ScenarioExecutionGraph;

public interface GraphExporter {

    void export(ScenarioExecutionGraph graph, String pathname);

}
