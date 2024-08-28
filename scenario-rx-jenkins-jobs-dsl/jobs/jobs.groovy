import com.ericsson.de.scenariorx.jenkins.dsl.builders.*

import javaposse.jobdsl.dsl.DslFactory

def mvnUnitTest = "clean install -T 4"
def mvnDeploy = "clean deploy -DskipTests"

def unitTests = 'Unit tests'
def snapshots = 'Snapshots deployment'

//Gerrit
def aa = new GerritJobBuilder('AA-gerrit-unit-tests', unitTests, mvnUnitTest)
//def ab = new SonarQubeGerritJobBuilder('AB-gerrit-sonar-qube')

//Build Flow
def ba = new SimpleJobBuilder('BA-unit-tests', unitTests, mvnUnitTest)
def bb = new SimpleJobBuilder('BB-deploy-snapshots', snapshots, mvnDeploy)
def bc = new DocsBuildJobBuilder('BC-docs-build')
def bd = new DocsPublishJobBuilder('BD-docs-publish', bc.name)
def be = new SonarQubeJobBuilder("XX-nightly-sonarqube")

def build = new MasterBuildFlowBuilder('B-build-flow','scenario-rx.*-release',
        """\
           build '${ba.name}'
           build '${bb.name}'
           build '${bc.name}'
           build '${bd.name}'
        """.stripIndent())

def release = new ReleaseJobBuilder('XX-release', build.name)

[aa, ba, bb, bc, bd, build, be, release]*.build(this as DslFactory)
