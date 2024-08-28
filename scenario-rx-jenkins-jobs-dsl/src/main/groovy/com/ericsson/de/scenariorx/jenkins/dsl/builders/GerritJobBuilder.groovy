package com.ericsson.de.scenariorx.jenkins.dsl.builders

import com.ericsson.de.scenariorx.jenkins.dsl.utils.Gerrit
import com.ericsson.de.scenariorx.jenkins.dsl.utils.Git
import com.ericsson.de.scenariorx.jenkins.dsl.utils.Maven
import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

class GerritJobBuilder extends FreeStyleJobBuilder {

    private static final String DESCRIPTION_SUFFIX = 'as a part of Gerrit verification process'

    final String mavenGoal

    GerritJobBuilder(String name,
                     String description,
                     String mavenGoal) {
        super(name, "${description} ${DESCRIPTION_SUFFIX}")

        this.mavenGoal = mavenGoal
    }

    @Override
    Job build(DslFactory factory) {
        def job = super.build(factory)
        job.with {
            concurrentBuild()
            scm {
                Git.gerrit delegate
            }
            triggers {
                Gerrit.patchsetCreated delegate
            }
            steps {
                Maven.goal delegate, mavenGoal
            }
        }
        return job
    }
}
