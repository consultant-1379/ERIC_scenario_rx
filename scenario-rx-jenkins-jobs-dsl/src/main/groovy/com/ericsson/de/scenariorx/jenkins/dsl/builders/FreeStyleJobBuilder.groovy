package com.ericsson.de.scenariorx.jenkins.dsl.builders

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

class FreeStyleJobBuilder extends AbstractJobBuilder {

    FreeStyleJobBuilder(String name, String description) {
        super(name, description)
    }

    @Override
    Job create(DslFactory factory) {
        factory.freeStyleJob name
    }
}
