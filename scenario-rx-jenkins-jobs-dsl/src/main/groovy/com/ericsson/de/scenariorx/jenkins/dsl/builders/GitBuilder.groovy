package com.ericsson.de.scenariorx.jenkins.dsl.builders

import com.ericsson.de.scenariorx.jenkins.dsl.Constants


import javaposse.jobdsl.dsl.helpers.ScmContext

class GitBuilder {

    String remoteUrl = Constants.GIT_URL
    String remoteName = Constants.GIT_REMOTE
    String remoteRefspec = ''
    String gitBranch = Constants.GIT_BRANCH
    boolean addGerritTrigger = false

    def build(ScmContext scm) {
        scm.with {
            git {
                remote {
                    url remoteUrl
                    name remoteName
                    refspec remoteRefspec
                }
                branch gitBranch
                extensions {
                    cleanBeforeCheckout()
                    if (addGerritTrigger) {
                        choosingStrategy {
                            gerritTrigger()
                        }
                    }
                }
            }
        }
    }
}
