import com.ericsson.de.scenariorx.jenkins.dsl.Constants
import javaposse.jobdsl.dsl.views.jobfilter.MatchType
import javaposse.jobdsl.dsl.views.jobfilter.Status

listView(Constants.JOBS_PREFIX) {
    description "Jobs for ${Constants.PROJECT_NAME}"
    jobs {
        regex(/^${Constants.JOBS_PREFIX}.*$/)
    }
    jobFilters {
        status {
            matchType MatchType.EXCLUDE_MATCHED
            status Status.DISABLED
        }
    }
    columns {
        status()
        weather()
        name()
        lastDuration()
        lastSuccess()
        lastBuildConsole()
        buildButton()
        configureProject()
    }
}
