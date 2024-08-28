package com.ericsson.de.scenariorx.jenkins.dsl.utils

import com.ericsson.de.scenariorx.jenkins.dsl.Constants
import javaposse.jobdsl.dsl.ContextHelper
import javaposse.jobdsl.dsl.helpers.triggers.GerritEventContext
import javaposse.jobdsl.dsl.helpers.triggers.TriggerContext

final class Gerrit {

    private Gerrit() {
    }

    static def patchsetCreated(TriggerContext triggers) {
        gerritTrigger(triggers) {
            (delegate as GerritEventContext).patchsetCreated()
        }
    }

    static def refUpdated(TriggerContext triggers) {
        gerritTrigger(triggers) {
            (delegate as GerritEventContext).refUpdated()
        }
    }

    private static def gerritTrigger(TriggerContext triggers, Closure eventsClosure) {
        triggers.with {
            gerrit {
                events {
                    ContextHelper.executeInContext eventsClosure, delegate
                }
                project Constants.GIT_PROJECT, Constants.GIT_BRANCH

                configure {
                    it << serverName(Constants.GERRIT_SERVER)
                }
            }
        }
    }
}
