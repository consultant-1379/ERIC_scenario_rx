package com.ericsson.de.scenariorx.jenkins.dsl.builders

import com.ericsson.de.scenariorx.jenkins.dsl.Constants
import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

abstract class AbstractJobBuilder {

    private static final String DESCRIPTION_SUFFIX = 'DO NOT MODIFY! This job has been generated via Job DSL seed job'

    final String name
    final String description

    AbstractJobBuilder(String name, String description) {
        this.name = "${Constants.JOBS_PREFIX}-${name}"
        this.description = "${Constants.PROJECT_NAME} ${description}\n\n${DESCRIPTION_SUFFIX}"
    }

    Job build(DslFactory factory) {
        def job = create factory
        job.with {
            description description
            logRotator 5
            jdk Constants.JDK_1_8
            label Constants.SLAVE_TAF_MAIN
            wrappers {
                timestamps()
                colorizeOutput()
            }
            publishers {
                allowBrokenBuildClaiming()
            }
        }
        return job
    }

    abstract Job create(DslFactory factory)
}
