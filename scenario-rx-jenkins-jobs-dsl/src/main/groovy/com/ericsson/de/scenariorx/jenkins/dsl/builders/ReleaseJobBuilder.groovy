package com.ericsson.de.scenariorx.jenkins.dsl.builders

import com.ericsson.de.scenariorx.jenkins.dsl.Constants
import com.ericsson.de.scenariorx.jenkins.dsl.utils.Maven
import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.jobs.MavenJob

@SuppressWarnings("GroovyAssignabilityCheck")
class ReleaseJobBuilder extends MavenJobBuilder {

    static final String DESCRIPTION = 'Release to Nexus'
    final String buildFlowJobName

    ReleaseJobBuilder(String name, String buildFlowJobName) {
        super(name, DESCRIPTION)
        this.buildFlowJobName = buildFlowJobName

    }

    @Override
    Job build(DslFactory factory) {
        def job = super.build(factory)
        buildMaven job
    }

    MavenJob buildMaven(MavenJob job) {
        job.with {
            label(Constants.SLAVE_DOCKER_POD_H)
            jdk(Constants.JDK_1_8_DOCKER)
            blockOn(buildFlowJobName) {
                blockLevel 'GLOBAL'
            }
            scm {
                git {
                    remote {
                        name 'gm'
                        url "${Constants.GERRIT_MIRROR}/${Constants.GIT_PROJECT}"
                    }
                    remote {
                        name 'gc'
                        url "${Constants.GERRIT_CENTRAL}/${Constants.GIT_PROJECT}"
                    }
                    branch Constants.GIT_BRANCH
                    extensions {
                        perBuildTag()
                        cleanAfterCheckout()
                        disableRemotePoll()
                    }
                    configure {
                        def ext = it / 'extensions'
                        def pkg = 'hudson.plugins.git.extensions.impl'
                        ext / "${pkg}.UserExclusion" << excludedUsers('Jenkins Release')
                        ext / "${pkg}.UserIdentity" << name('Jenkins Release')
                    }
                }
            }

            preBuildSteps {
                shell """\
                    export GIT_URL=\${GIT_URL_1}

                    #cannot push back to gerrit mirror so need to set url to GC
                    repo=\$(echo \$GIT_URL | sed 's#.*OSS/##g')

                    git remote set-url --push gc \${GERRIT_CENTRAL}/OSS/\${repo}

                    git checkout ${Constants.GIT_BRANCH} || git checkout -b ${Constants.GIT_BRANCH}
                    git reset --hard gm/${Constants.GIT_BRANCH}
                    """.stripIndent()
            }

            mavenInstallation Maven.MAVEN_VERSION

            goals """ ${Maven.MAVEN_OPTIONS} -Dresume=false -DlocalCheckout=true release:prepare -DpreparationGoals="clean install -DskipTests -Dmaven.javadoc.skip=true" release:perform -Dgoals="clean deploy -DskipTests -Dmaven.javadoc.skip=true"
 """.stripIndent()
            mavenOpts '-XX:MaxPermSize=1024m'
            configure {
                it / 'runPostStepsIfResult' << name('SUCCESS')
            }
            publishers {
                git {
                    pushOnlyIfSuccess()
                    branch 'gc', Constants.GIT_BRANCH
                }
            }
        }
        return job
    }
}
