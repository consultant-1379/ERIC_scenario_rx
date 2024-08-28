package com.ericsson.de.scenariorx.jenkins.dsl.builders

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.jobs.BuildFlowJob

class BuildFlowBuilder extends AbstractJobBuilder {

    final String buildFlowText
    final String buildFlowJobName

    BuildFlowBuilder(String name, String description,
                     String buildFlowText, String buildFlowJobName) {
        super(name, description)
        this.buildFlowText = buildFlowText
        this.buildFlowJobName = buildFlowJobName
    }

    @Override
    Job build(DslFactory factory) {
        def job = super.build(factory)
        job.with {
            (delegate as BuildFlowJob).buildFlow buildFlowText

            blockOn(buildFlowJobName) {
                blockLevel 'GLOBAL'
            }
        }
        return job
    }

    @Override
    Job create(DslFactory factory) {
        factory.buildFlowJob name
    }
}
