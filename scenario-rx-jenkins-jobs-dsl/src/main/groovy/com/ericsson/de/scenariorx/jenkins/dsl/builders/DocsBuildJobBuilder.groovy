package com.ericsson.de.scenariorx.jenkins.dsl.builders

import com.ericsson.de.scenariorx.jenkins.dsl.Constants
import com.ericsson.de.scenariorx.jenkins.dsl.utils.Git
import com.ericsson.de.scenariorx.jenkins.dsl.utils.Maven
import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

class DocsBuildJobBuilder extends FreeStyleJobBuilder {

    static final String DESCRIPTION = 'Documentation building'

    DocsBuildJobBuilder(String name) {
        super(name, DESCRIPTION)
    }

    @Override
    Job build(DslFactory factory) {
        super.build(factory).with {
            scm {
                Git.simple delegate
            }
            steps {
                Maven.goal delegate, "clean site"
                shell """\
                    cd ${Constants.DOCS_DIRECTORY}
                    zip ${Constants.DOCS_ZIP} -r *
                    """.stripIndent()
            }
            publishers {
                archiveArtifacts "${Constants.DOCS_DIRECTORY}/${Constants.DOCS_ZIP}"
            }
        }
    }
}
