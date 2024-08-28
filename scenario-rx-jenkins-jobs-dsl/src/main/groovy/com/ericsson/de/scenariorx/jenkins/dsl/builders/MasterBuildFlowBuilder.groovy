package com.ericsson.de.scenariorx.jenkins.dsl.builders

import com.ericsson.de.scenariorx.jenkins.dsl.Constants
import com.ericsson.de.scenariorx.jenkins.dsl.utils.Gerrit
import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

class MasterBuildFlowBuilder extends BuildFlowBuilder {

    static final String DESCRIPTION = "Build flow upon branch '${Constants.GIT_BRANCH}' update"
    final String buildFlowJobName

    MasterBuildFlowBuilder(String name, String buildFlowJobName, String buildFlowText) {
        super(name, DESCRIPTION, buildFlowText, buildFlowJobName)
    }

    @Override
    Job build(DslFactory factory) {
       super.build(factory).with {
            triggers {
                Gerrit.refUpdated delegate
            }
        }
    }
}
