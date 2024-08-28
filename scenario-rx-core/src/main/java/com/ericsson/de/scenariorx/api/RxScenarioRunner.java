package com.ericsson.de.scenariorx.api;

import java.util.List;

import com.ericsson.de.scenariorx.impl.Scenario;
import com.ericsson.de.scenariorx.impl.ScenarioListener;
import com.ericsson.de.scenariorx.impl.ScenarioRunner;
import com.ericsson.de.scenariorx.impl.graph.export.GraphExporter;
import com.ericsson.de.scenariorx.impl.graph.export.GraphMlExporter;
import com.ericsson.de.scenariorx.impl.graph.export.SvgExporter;

public class RxScenarioRunner extends ScenarioRunner {

    static GraphExporter graphMlExporter = new GraphMlExporter();
    static GraphExporter svgExporter = new SvgExporter();

    RxScenarioRunner(DebugGraphMode debugGraphMode,
                     List<ScenarioListener> listeners,
                     RxExceptionHandler defaultExceptionHandler) {
        super(debugGraphMode, listeners, defaultExceptionHandler);
    }

    @Override
    public void run(Scenario scenario) {
        super.run(scenario);
    }

    /**
     * @deprecated use {@link #run(Scenario)} instead.
     */
    @Deprecated
    public void start(Scenario scenario) {
        super.run(scenario);
    }
}
