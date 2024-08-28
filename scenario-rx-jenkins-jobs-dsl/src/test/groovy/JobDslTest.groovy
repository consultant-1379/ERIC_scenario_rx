import com.ericsson.de.scenariorx.jenkins.dsl.Constants
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.MemoryJobManagement
import javaposse.jobdsl.dsl.ScriptRequest
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.fail

class JobDslTest {

    private MemoryJobManagement jobManagement
    private DslScriptLoader scriptLoader

    @Before
    void setUp() throws Exception {
        jobManagement = new MemoryJobManagement()
        scriptLoader = new DslScriptLoader(jobManagement)
    }

    @Test
    void scriptsCompile() throws Exception {
        def directory = new File(Constants.JOBS_DIRECTORY)
        def requests = collectScriptRequestsFrom directory
        try {
            scriptLoader.runScripts requests
        } catch (e) {
            fail "No exceptions were expected, but got ${e}"
        }
    }

    static List<ScriptRequest> collectScriptRequestsFrom(File directory) {
        def requests = []
        directory.eachFileMatch(~/.*\.groovy/) { script ->
            String scriptBody = script.text
            requests << new ScriptRequest(scriptBody, [] as URL[], false, script.absolutePath)
        }
        requests
    }
}
