package com.ericsson.de.scenariorx.jenkins.dsl.builders

import com.ericsson.de.scenariorx.jenkins.dsl.utils.Maven
import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.jobs.MavenJob

import static com.ericsson.de.scenariorx.jenkins.dsl.Constants.*

class SonarQubeJobBuilder extends MavenJobBuilder {

    static final String DESCRIPTION = 'Execute Sonarqube against this repo'

    SonarQubeJobBuilder(String name) {
        super(name, DESCRIPTION)
    }

    @Override
    Job build(DslFactory factory) {
        def job = super.build(factory)
        buildMaven job
    }

    MavenJob buildMaven(MavenJob job) {
        job.with {
            scm {
                git {
                    remote {
                        name 'gm'
                        url "${GERRIT_MIRROR}/${GIT_PROJECT}"
                    }
                    remote {
                        name 'gc'
                        url "${GERRIT_CENTRAL}/${GIT_PROJECT}"
                    }
                    branch GIT_BRANCH
                }
            }
            triggers {
                scm('H 1 * * * ')
            }
            mavenInstallation Maven.MAVEN_VERSION
            goals "-U -V clean org.jacoco:jacoco-maven-plugin:prepare-agent install -DskipTests"
            mavenOpts '-XX:MaxPermSize=1024m'
            publishers {
                sonar {
                    installationName("SonarQube")
                    additionalProperties("-Dsonar.java.binaries=target/classes")
                    mavenInstallation Maven.MAVEN_VERSION
                }
            }
        }
        return job
    }
}
