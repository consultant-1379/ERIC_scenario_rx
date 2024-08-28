package com.ericsson.de.scenariorx.jenkins.dsl.builders

import com.ericsson.de.scenariorx.jenkins.dsl.utils.Maven
import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

class SonarQubeGerritJobBuilder extends GerritJobBuilder {

    private static final String DESCRIPTION_SUFFIX = 'as a part of Gerrit verification process'

    SonarQubeGerritJobBuilder(String name) {
        super(name, "Sonarqube ${DESCRIPTION_SUFFIX}", "clean org.jacoco:jacoco-maven-plugin:prepare-agent install -DskipTests")
    }

    @Override
    Job build(DslFactory factory) {
        super.build(factory).with {
            publishers {
                sonar {
                    installationName("SonarQube")
                    additionalProperties("-Dsonar.java.binaries=target/classes")
                    mavenInstallation Maven.MAVEN_VERSION
                }
            }
        }
    }
}
