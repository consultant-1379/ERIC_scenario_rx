package com.ericsson.de.scenariorx.api;

import com.ericsson.de.scenariorx.impl.graph.export.GraphExporter;

/**
 * In which format to save Scenario Graph
 */
public enum DebugGraphMode {
    NONE(),
    /**
     * Save Scenario Graph in Graph Ml format for tools like <a href="http://www.yworks.com/products/yed?">Yed</a>
     */
    GRAPH_ML(RxScenarioRunner.graphMlExporter),
    /**
     * Save Scenario Graph as svg image
     */
    SVG(RxScenarioRunner.svgExporter),
    ALL(RxScenarioRunner.graphMlExporter, RxScenarioRunner.svgExporter);

    private GraphExporter[] exporters;

    DebugGraphMode(GraphExporter... exporters) {
        this.exporters = exporters;
    }

    public GraphExporter[] getExporters() {
        return exporters;
    }
}
