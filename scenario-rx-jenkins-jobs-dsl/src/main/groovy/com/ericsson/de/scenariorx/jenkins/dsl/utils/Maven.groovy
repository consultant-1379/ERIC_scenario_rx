package com.ericsson.de.scenariorx.jenkins.dsl.utils

import javaposse.jobdsl.dsl.helpers.step.StepContext

final class Maven {

    static final String MAVEN_VERSION = 'Maven 3.3.3'
    static final String MAVEN_OPTIONS = '-e -U -V'

    private Maven() {
    }

    static def goal(StepContext steps, goal) {
        steps.with {
            maven {
                mavenInstallation MAVEN_VERSION
                goals "${MAVEN_OPTIONS} ${goal}"
                mavenOpts '-Xms256m'
                mavenOpts '-Xmx512m'
            }
        }
    }

    static def goalSequential(StepContext steps, String... mavenGoals) {
        steps.with {
            mavenGoals.each { goal ->
                maven {
                    mavenInstallation(MAVEN_VERSION)
                    goals("${MAVEN_OPTIONS} ${goal}")
                    mavenOpts('-XX:MaxPermSize=1024m')
                }
            }
        }
    }
}
