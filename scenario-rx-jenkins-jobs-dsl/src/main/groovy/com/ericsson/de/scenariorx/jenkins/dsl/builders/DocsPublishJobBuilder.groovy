package com.ericsson.de.scenariorx.jenkins.dsl.builders

import com.ericsson.de.scenariorx.jenkins.dsl.Constants
import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

class DocsPublishJobBuilder extends FreeStyleJobBuilder {

    static final String DESCRIPTION = 'Documentation publishing'

    final String docsBuildJobName

    DocsPublishJobBuilder(String name, String docsBuildJobName) {
        super(name, DESCRIPTION)
        this.docsBuildJobName = docsBuildJobName
    }

    @Override
    Job build(DslFactory factory) {
        super.build(factory).with {
            steps {
                copyArtifacts(docsBuildJobName) {
                    buildSelector {
                        latestSuccessful()
                    }
                    includePatterns "${Constants.DOCS_DIRECTORY}/${Constants.DOCS_ZIP}"
                }
                shell """\
                    targetDir=/proj/PDU_OSS_CI_TAF/taflanding
                    rm -rf \${targetDir}/${Constants.JOBS_PREFIX}
                    unzip ${Constants.DOCS_DIRECTORY}/${Constants.DOCS_ZIP} -d \${targetDir}/${Constants.JOBS_PREFIX}
                    """.stripIndent()
            }
        }
    }
}
