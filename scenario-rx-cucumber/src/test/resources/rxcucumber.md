class: middle, center
# Cucumber⇄Rx Scenario prototype

.left.footer[Keith Moore]

---

# What is Rx Scenario?

* An advanced way to define and schedule test execution using Java API
* Example:

```java
RxScenario scenario = scenario("scenario")
        .addFlow(
                flow("flow")
                        .addTestStep(
                              annotatedMethod(this, Ids.TEST_STEP_1))
                        .addTestStep(
                              annotatedMethod(this, Ids.TEST_STEP_2))
        )
        .build();
```

---

# What is Rx Scenario?

* Complete rewrite of TAF Scenario
    - More robust concurrency model (RxJava)
    - Improved API based on user feedback
* Internal development - pending open source approval
* No dependencies on TAF
    - Independent implementation
    - Minimal dependencies
    - Adapter modules for different frameworks:
        + TAF
        + Generic
        + ...

---

# What is Rx Scenario?

* Complete rewrite of TAF Scenario
    - More robust concurrency model (RxJava)
    - Improved API based on user feedback
* Internal development - pending open source approval
* No dependencies on TAF
    - Independent implementation
    - Minimal dependencies
    - Adapter modules for different frameworks:
        + TAF
        + Generic
        + ...
        + *Cucumber?*

---



# Motivation I

* Cucumber and Rx Scenarios use similar approach:
    - Scenario definition
    - Java Step definition (Annotated methods with arguments)
    - Implementation
* Cucumber features are subset of Rx Scenario features

---

# Motivation II

Both frameworks have different benefits:

.left-column[

#### Cucumber
* Readable feature syntax
* Convenient IDE integration
]

.right-column[
#### Rx Scenario
* Passing data between test steps
* Parallelization
* Loading data from different sources
* Performance Features
]

--

.clear[

<br><br>
    *Can we use benefits of both frameworks with minimal efforts?*
]    

---

# Reusing functionality

*All features are already implemented in both frameworks. The purpose of the prototype is to combine them together.*

.left-column[
#### Cucumber
- Parsing `.feature` files
- Loading classes (DI)
- Parsing arguments
- Running Hooks
]

.right-column[
#### Rx Scenario
- Runner
- Allure Reporting
]

.clear[]

---

# Cucumber Extensibility

* [Minimal Cucumber Reference Implementation](https://github.com/cucumber-attic/microcuke) in 500 LOC
* Due to modular structure we can adapt Cucumber functionality
    - Improve compatibility
* [Gherkin parser/compiler](https://github.com/cucumber/gherkin-java) is extracted to separate module
    - Parses `.feature` files to beans that could be converted to Scenario

---

# Overview

1. Use Gherkin API to parse `.feature` files
2. Use Cucumber API to load `@Given`/`@When`/`@Then` steps
3. Use Cucumber API to parse step arguments
4. Convert Cucumber structure to Rx Scenario
5. Add additional features like parallelization and Data Sources
6. Execute Cucumber step using Rx Scenario Runner
7. Call Cucumber Plugin API from Rx Scenario Listener to enable make Cucumber plugins (like Intellij IDEA, and console reporting) to work

---

# Internal

* Translation to Scenarios happens in runtime
* Transparent for users

.left-column[

#### Feature:

```gherkin
Scenario Outline: eating
  Given there are <start>
  When I eat <eat> cucumbers
  Then I should have <left>

  Examples:
    | start | eat | left |
    |  12   |  5  |  7   |
    |  20   |  5  |  15  |
```
]

.right-column[

#### Internal representation:

```java
scenario("eating")
  .addFlow(
    flow("eating")
      .addTestStep(call("setup"))
      .addTestStep(call("exec"))
      .addTestStep(call("verify"))
      .withDataSources(dataSource())
  )
  .build();
```
]

---

# Supported Gherkin Features

* Running Features
* DocStrings
* DataTables
* Outlines
* Tags

* (Cucumber) Hooks

---

# Additional Features

* Load Data Source:
  - From file
  - Test Data Management system   
* Run Scenario in *n* parallel threads
* Pass data between steps
* Basic performance features
* Svg/Allure reports  

---

# 2 ways to extend Gherkin Syntax

1. "Given" definitions
2. @Tags

---

# "Given" definitions

```gherkin
  Scenario: Some determinable business situation
*   Given repeating from file data/node.csv
*   Given number of users equals 3
    Given Login
    Given Perform setup for enviroment
     When Add alarm
     Then Validate alarm
```

* Pros:
    - Gherkin syntax
    - IDE Autocompletion
    - IDE Go To Definition

---

# @Tags

Alternative approach

```gherkin
* @DataSource("data/node.csv")
* @VUsers(3)
  Scenario: Some determinable business situation
    Given Login
    Given Perform setup for enviroment
     When Add alarm
     Then Validate alarm
```

* Pros:
    - Might be familiar to some users
* Cons:
    - In Gherkin these are *not* annotations but tags
        + Might be confusing
    - No IDE support

---

# Pass values I

* Scenario Runner allows Cucumber Steps to:
    - Return values
    - Inject `@Named` values
* Can be combined with Cucumber parameters
* Possibility to pass values between steps:

```java
    @When("^I push an item into the stack$")
    public RxDataRecord producer() {
        Object pushed = new Object();
        myStack.push(pushed);

*       return RxBasicDataRecord.builder()
*               .setField("newItem", pushed)
*               .build();
    }

    @Then("^My Item is in a stack$")
    public void consumer(
*     @Named("newItem") Object arg1) {
        assertTrue(arg1 == myStack.peek());
    }
```

---

# Pass values II

* Inject result from previously executed Test Step

```java
    @When("^Add alarm$")
    public DataRecord addAlarm() {
        //...

*       return newAlarm;
    }

    @Then("^Validate alarm$")
    public void validateAlarm(
*        @Named("Add alarm") Alarm alarm) {
        
        //...
    }
```

---

# From Data Source I

* Feature

```gherkin
 Scenario: Data Driven
*    Given repeating from file data/node.csv
     ...
```

--

* `data/node.csv`

```csv
networkElementId,nodeType
SGSN-14B,SGSN-MME
LTE01ERB,ERBS
LTE08dg2,RadioNode
```

--

* Step - inject bean or property

```java
   @Given("^Perform setup for environment$")
   public void perform_setup_for_enviroment(
*        @Named("ds") Node node,
*        @Named("networkElementId") String networkElementId) {
        node.getNetworkElementId();
        //...
    }
```

---

# From Data Source II

* Loading Data Source will work similiar to Gherkin Outline, with difference:
  * Examples could be loaded from external location
  * This location could be parametrized

---

# Parametrization

* Feature

```gherkin
Scenario: 
* Given repeating from Data Source named nodes1
  When assert network element id
```
--

* Define where to take data for `nodes1` in `datadriven.properties`:
* csv:
```properties
dataprovider.nodes1.type=csv
dataprovider.nodes1.location=data/node.csv
```
--

* Override via properties: `-Ddataprovider.nodes1.type=csv` <br/>`-Ddataprovider.nodes1.location=alternative_location.csv`
--

* Configure Data Sources on TAF executor

---

# Fallback

* Possibility to define fallback to file if dataprovider properties are not defined:

```gherkin
Scenario: Load nodes (fallback to file)
* Given repeating from Data Source with name nodes2 or file data/node.csv
  When assert network element id
```

---

# Usage 

* IDE - Intellij IDEA
* CLI - JUnit Plugin

---

# IDE support - IDEA

* Cucumber Intellij IDEA plugin will work with minimal configuration
* Auto Completion
* Goto Definition (ctrl+click in `.feature` file)
* Run single Scenario (right-click in `.feature` file)
* Create Test Step stubs
* Test Result integration

<img style="width: 40%" src="/home/edmnovi/safe/docs/cucumber/idea3.png">
<img style="width: 40%" src="/home/edmnovi/safe/docs/cucumber/idea2.png">

---

# Intellij IDEA Setup

* ⚠ Need to change default Run-Debug configuration:
    * From: `cucumber.api.cli.Main`
    * To: `com.ericsson.de.scenariorx.ScenarioCucumberRunner`

<img style="width: 80%" src="/home/edmnovi/safe/docs/cucumber/idea1.png">

---


# IDE support - IDEA

### DEMO

---

# JUnit integration

* Running Rx Scenario-Cucumber tests from Maven/Jenkins
* Minimal efforts were spent to implement basic plugin

--

### Cucumber

```java
*@RunWith(Cucumber.class)
@CucumberOptions(/* ... */)
public class Test {
}
```

### Rx Runner

```java
*@RunWith(RxCucumberRunner.class)
@CucumberOptions(/* ... */)
public class Test {
}
```

---

# What if

* Q: What if "feature" will be executed with Cucumber Runner?

--

* A: Depends if it:
    * Does not use extended features from Rx - execute normally
    * Uses Extended features - Cucumber will fail on no existing steps/no argument provided

--

* If Scenario-Cucumber is in class path - show descriptive message
* Possibility to add requirement for Scenario Cucumber runner:
    - Run and fail with Cucumber Runner, ignored in Scenario Runner

```java
    @Before
*   @ScenarioIgnore
    public void beforeScenario() {
        throw new IllegalStateException(
                "Please use Scenario Cucumber Runner\n"+
                "http://github.com/rxscenarios/rxscenarios/..."
        );
    }
```

---

# Allure reporting 1

* Execution graph:

<img src="/home/edmnovi/safe/docs/cucumber/allure1.png"/>

---

#### Possible Features

* With possibility to view each Step execution time/inputs/errors:

<img src="/home/edmnovi/safe/docs/cucumber/allure2.png"/>

---

# Allure reporting

### DEMO

---

# Taurus

* Taurus can show and assert metrics from Graphite
* Rx Scenario can put Test Step execution times to Graphite
* Show certain Test Step metrics in Graphite:

```yaml
services:
- module: monitoring
  graphite:
  - address: localhost:81
    label: localhost
    metrics:
*   - com.ericsson.de.scenariorx.teststep.name.*
```

* Fail if Test Step executes longer than 1 second:

```yaml
reporting:
- module: passfail
  criteria:
  - class: bzt.modules.monitoring.MonitoringCriteria
*   subject: localhost/com.ericsson.de.scenariorx.teststep.name.1
    condition: '>'
*   threshold: 1000
    timeframe: 5s    
```

---

# Possible features

* Some features could be implemented with minimal efforts
    - 0.5-2 developer days
* Just map Gherkin to existing Rx Scenario features

---

# Possible features

* Combination of Data Sources
* Data Source filtering
* Integration with Arquillian
* `.toJavaApi()` - convert Gherkin Feature to Rx Scenario
    - For more advanced features like subflows
* Support for [BeforeFeature](https://github.com/cucumber/cucumber-jvm/pull/295)

---

# Always Run 

#### Possible Features

* Possibility to annotate Cucumber step with `@AlwaysRun` to execute step even in case of Test failure
* Works with *same* Data Record

```java
    @Then("^Cleanup nodes$")
*   @AlwaysRun
    public void cleanup_node(@Named("node") Node node) throws Throwable {
        // ....
    }
```

---

# Flexibility

#### Possible Features

* Modifying class loading
* Logging control
* Environment specific properties
* ...

---

# Case Study

### Team America Collection Search Plugin

* Initially written for Cucumber
* Produces same results using Scenario runner (Requires migration Groovy→Java)
* Scenario runner allows use additional features of:
    * Passing data between steps 
    * Loading data from file
    * Running scenario with 2 different users in parallel
* By that we were able to ensure that two users don't affect each others private collections when working in parallel
  - Change is in [Gerrit](https://gerrit.ericsson.se/#/c/2671121/)
  - see `collection_get.feature` 


---

# Next Steps

* Documentation
* Migrate more Testware
* Full featured JUnit plugin
* Full Cucumber reporting support
* Only Java is currently supported (no Groovy etc)
* Java8 Test Steps support
* Advanced tags (combination/negation)
* Cucumber Dry run support
* Run methods annotated with `@TestStep` to support existing TAF testware  

---

# Links

* [Rx Scenario documentation](http://taf.lmera.ericsson.se/taflanding/scenarios-rx/)
* [Team America Collection Search Plugin](https://gerrit.ericsson.se/#/c/2671121/)
* [Prototype source - ERIC_scenario_rx](https://gerrit.ericsson.se/#/admin/projects/OSS/com.ericsson.cifwk/ERIC_scenario_rx), ⌥branch `dCucumber`

---

class: middle, center
# Q&A

---

class: middle, center
# Performance

.left.footer[Keith Moore]

---

# Performance

* Scenario Runner could be used to provide basic performance features 
* Performance Test written in Gherkin and executed with Scenario Runner:

```gherkin
Scenario: Concurrent Data Source processing
* Given performance test
*   And rampup during 30 seconds
*   And number of users equals 3
*   And repeating from file data/performance.csv
*   And repeat for 2 minutes
  When I push an item from datasource into the context
  Then My Item is in a context
```

---

# Performance

```gherkin
Scenario: ...
* Given performance test
    And number of users equals 3
    And rampup during 30 seconds
    And repeating from file data/performance.csv
    And repeat for 2 minutes
```

* Enable concurrent execution
  - Predictability vs. performance
* Disable `svg` reporting
* Enable Performance Reporting

---

# Parallel execution

```gherkin
Scenario: ...
  Given performance test
*   And number of users equals 3
    And rampup during 30 seconds
    And repeating from file data/performance.csv
    And repeat for 2 minutes
```

* Set number of parallel users

---

# Rampup

```gherkin
Scenario: ...
  Given performance test
    And number of users equals 3
*   And rampup during 30 seconds
    And repeating from file data/performance.csv
    And repeat for 2 minutes
```

* Rampup users during defined time period
* Possible to define different Rampup strategies:
    - Add one user per time
    - Add multiple users per time (batch)
    - ...

---

# Data Driven

```gherkin
Scenario: ...
  Given performance test
    And number of users equals 3
    And rampup during 30 seconds
*   And repeating from file data/performance.csv
    And repeat for 2 minutes
```

* Each user will process one record from Data Source
* It's also possible to make all users to use same record

---

# Duration

```gherkin
Scenario: ...
  Given performance test
    And number of users equals 3
    And rampup during 30 seconds
    And repeating from file data/performance.csv
*   And repeat for 2 minutes
```

* Set duration
* Loop Data Source during this time
* Possible to define different conditions

---

# Reporting: Graphite/Grafana

* For real time reporting Graphite/Grafana could be used
* Dockerized solution - can run on workstation or server
* Show Test Step execution times (min/max)
* Graphana allows to use functions (like stdev)
* Save Graph snapshots for Performance Reports

<img src="/home/edmnovi/safe/docs/cucumber/performance1.png"/>

---

# Taurus

* Taurus can show and assert metrics from Graphite
* Rx Scenario can put Test Step execution times to Graphite
* Show certain Test Step metrics in Graphite:

```yaml
services:
- module: monitoring
  graphite:
  - address: localhost:81
    label: localhost
    metrics:
*   - com.ericsson.de.scenariorx.teststep.name.*
```

* Fail if Test Step executes longer than 1 second:

```yaml
reporting:
- module: passfail
  criteria:
  - class: bzt.modules.monitoring.MonitoringCriteria
*   subject: localhost/com.ericsson.de.scenariorx.teststep.name.1
    condition: '>'
*   threshold: 1000
    timeframe: 5s    
```

---

# Reporting: Console

* After each execution Performance Scenario provides summary

```gherkin
Scenario: test
Test duration: 4.001s
Samples count: 3, 0.33% (1) failures
Stats by Test Step: 
  Name: step1, Samples: 3, Min: 0.101s, Max: 1.501s, Avg: 0.569s
  Name: step2, Samples: 1, Min: 4.001s, Max: 4.001s, Avg: 4.001s
```

---

# Reporting

* With minimal efforts it's possible to extend reporting features:
    - Csv/Json/Xml export for Jenkins
    - Additional metrics like standard deviation
* Currently Step execution time is measured
* Potential to extend [Http Tool](http://taf.lmera.ericsson.se/taflanding/http-tool/) to provide **per request** metrics
* It's possible set up [Saiku](http://taf.lmera.ericsson.se/taflanding/performance/snapshot/saiku/saiku_use.html) for advanced post-execution metrics analysis (OLAP)

---

# Performance 

### DEMO

---

class: middle, center
# Q&A

---



