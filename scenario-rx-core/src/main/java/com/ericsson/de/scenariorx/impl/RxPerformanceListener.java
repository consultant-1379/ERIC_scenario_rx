package com.ericsson.de.scenariorx.impl;

import static java.lang.String.format;

import java.io.DataOutputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.de.scenariorx.api.RxScenarioListener;
import com.ericsson.de.scenariorx.api.events.RxScenarioEvent;
import com.google.common.eventbus.Subscribe;

public class RxPerformanceListener extends RxScenarioListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RxPerformanceListener.class);

    private final String host;
    private final int port;

    static RxScenarioListener get(String host, int port) {
        try {
            new Socket(host, port);
            LOGGER.error("Connected to Graphite at {}:{}", host, port);
            return new RxPerformanceListener(host, port);
        } catch (Exception e) {
            LOGGER.error("Unable to connect to Graphite at {}:{}, no performance data will be reported", host, port);
            return new RxScenarioListener() {
            };
        }
    }


    private RxPerformanceListener(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Subscribe
    public void onScenarioStarted(RxScenarioEvent.RxScenarioStartedEvent event) {
        // intentionally do nothing
    }

    @Subscribe
    public void debugTestStep(ScenarioEventBus.InternalTestStepFinishedEvent event) throws Exception {
        Socket conn = new Socket(host, port);
        DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

        String plainText = format("com.ericsson.de.scenariorx.teststep.%s.%s %s %s \n",
                event.getName().replaceAll("[\\W]|_", "").toLowerCase(),
                event.getExecution().vUser.toString().replace(".", "_"),
                event.getResult().endTime - event.getResult().startTime,
                event.getResult().endTime / 1000
        );

        dos.writeBytes(plainText);
        conn.close();
    }
}
