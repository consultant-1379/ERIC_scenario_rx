<div class="note"></div>
This is documentation for TAF Scenarios Rx (new version). [Getting_Started](#Getting_Started) <br/>
Documentation for Legacy Version is located in [TAF User Documentation](http://taf.lmera.ericsson.se/taflanding/userdocs/Latest/taf_concepts/taf-scenarios.html) <br/>
For migration guide for Legacy Version refer to [Migration Guide](migration_guide.html) <br/>

<div class="note"></div>
All examples below are located in [ERIC_scenario_rx repository](https://gerrit.ericsson.se/#/admin/projects/OSS/com.ericsson.cifwk/ERIC_scenario_rx), `path scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/`.

## Contents

<!-- MACRO{toc} -->

## Introduction

TAF Scenario is an advanced way to define and schedule test execution using Java API. Scenarios give flexibility of [executing tests in parallel](#split), repeating same test code with different [input data](#data-driven-testing) and simulating events combining multiple executions of Flows. Scenarios allow defining a wide variety of tests: from Acceptance and Integration tests to End-to-End and Performance tests. For debugging advanced executions, Scenarios provide visualization support.

<img src="images/scenario.png" />

In a nutshell TAF Scenario is a combination of [Test Flows](#flows) where a Test Flow is a sequence of user actions. For example, Flow "Send email" may consist of the following actions: login → compose email → send email → logout. These actions are called [Test Steps](#test-steps).

### Scenario Features

* Java API
* Can be used for:
    - Acceptance Tests
    - Integration Tests
    - End-to-end Tests
    - Unit Tests
* [Visualization of execution](#visualization)

### Scenario Example 

The simplest Scenario example.

<!-- MACRO{snippet|id=SIMPLEST_SCENARIO_EXAMPLE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

This code will sequentially call methods annotated with `Ids.TEST_STEP_1` and `Ids.TEST_STEP_2`:

<img src="images/buildingblocks.png" />

## Scenario building blocks

* [Operator](#operators) is a reusable API to SUT
* [Test Step](#test-step) is an atomic business action that parametrizes calls to operator(s)
* [Flow](#flow) is a sequence of Test Steps
* [Scenario](#scenario) is a combination of Flows 

<img src="images/buildingblocks2.png" />

<a name="operators"></a>
### Operators

Operators are classes annotated with `@com.ericsson.cifwk.taf.annotations.Operator`. They provide a Java API to the SUT (System Under Test). Usually accessing system via [HttpTool](http://taf.lmera.ericsson.se/taflanding/userdocs/Latest/tools/http-tool.html), [TAF UI](http://taf.lmera.ericsson.se/taflanding/userdocs/Latest/tools/taf-ui-sdk.html) or [CliTool](http://taf.lmera.ericsson.se/taflanding/userdocs/Latest/tools/cli-api.html).

Operators are reusable by different Test Steps and initialized by injecting `@Inject Provider<Operator>` into the Test Step class.

There are multiple operators created for a shared usage:

* [Login/Logout](https://confluence-nam.lmera.ericsson.se/display/ENMT/Security+Reusable+Use+Cases)
* [Working with Nodes](https://confluence-nam.lmera.ericsson.se/display/ENMT/Configuration+Management+Resuable+Use+Cases)
* [More…](https://confluence-nam.lmera.ericsson.se/display/ENMT/ENM+Architecture+for+Reusable+TAF+Testware)

<div class="note"></div>
Operators should not contain state, i.e. static variables, instance variables etc, because they can be accessed by multiple vUsers simultaneously.

<a name="test-steps"></a>
### Test Steps

* Test Step is a basic building block of a Test:
    - Test Step is an annotated Java method that executes atomic action (e.g. Login, Send Email). Test Steps are reused in different Flows. 
    - Test Step is an atomic test action, which encapsulates a single business function.
    - To increase reusability Test Step can be parameterized using parameters or [data sources](#data-sources). For example, parameter may be username or password.
    - Test steps must be added to a flow before any [data sources](#data-sources). 
    - Usually, Test Steps use [operators](#operators) to access SUT
    - Besides an interaction with SUT, Test Step typically contain assertions to verify state or response of SUT. This is where Scenario become *Test* Scenario.
* A combination of Test Steps that represents a sequence of user actions is called [Test Flow](#flows). Let's say, we have a Flow that may perform sending of an email. It may consist of the following actions (Test Steps): login → compose email → send email → logout.   
    - Flow can be run multiple times with different data. For example, email may be sent with different headers. This is achieved by adding [data sources](#data-sources). If you think of a Data Source as a table, Flow will be repeated for each row, passing row columns to Test Steps.

#### Test Step vs Operator

* Test Step
    - Typically contains assertion (`@Output`)
    - May contain Test Specific logic
    - Works with [Scenario Context](#context) and [Data Sources](#data-sources)
* Operator
    - More generic
    - Reusable

#### Test Step Example

* Java method annotated with `@TestStep` with ID
* Arguments are annotated with `@Input` to allow [parametrization](#test-step-parameters)
* If Test Step creates an object (e.g. a node) it can be [returned](#test-step-reuse) and used in subsequent TestSteps.

```java
    @Inject
    SUToperator operator;

    @TestStep(id = "login")
    public void multipleRecords(@Input("username") String username, @Input("password") String password) {
        SUToperator.login(username, password);
    }
```

In this example username and password could be passed as [parameters](#test-step-reuse), or injected from [scenario context](#context) or [data source](#data-sources)

<a name="flows"></a>
### Test Flow

Test Flow is a sequence of [Test Steps](#test-steps). As Test Steps are methods that accept arguments, Test Flow could be repeated multiple times with different Data Records - this concept is called [Data Driven Testing](#data-sources).

* Combination of Test Steps that represents sequence of user action
* Flow execution may be configured using [vUsers](#vusers) and [DataSources](#data-sources)
 
#### Flow Example 

Flow may perform sending of an email. It may consist of the following actions (Test Steps): login → compose email → send email → logout.

Flow is created using static builder method `com.ericsson.de.scenariorx.impl.Api#flow()`

<!-- MACRO{snippet|id=FLOW_EXAMPLE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

<a name="scenario"></a>
## Defining & Running Scenario

### Getting Started

TAF Scenario can be defined using Java API: Scenario and Flow Builders.

To start creating scenarios add the following dependency to testware:

```xml
    <dependency>
        <groupId>com.ericsson.de</groupId>
        <artifactId>scenario-rx-taf-adaptor</artifactId>
        <version> ... </version>
    </dependency>
```

<div class="note"></div>
All examples in this documentation are located in [ERIC_scenario_rx repository](https://gerrit.ericsson.se/#/admin/projects/OSS/com.ericsson.cifwk/ERIC_scenario_rx), path `scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/`. It may be useful to start learning Rx Scenarios by running these examples.

All builders are located in one class - `TafRxScenarios`, so when starting to define a scenario it might make sense to static import all API methods (IDE should replace it with individual methods before commit):

```java
import static com.ericsson.de.scenariorx.api.TafRxScenarios.*;
```

Let's look at the very simple Scenario *definition* that executes actions from previous examples:

<!-- MACRO{snippet|id=DEFINING_SCENARIO|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

Scenario can be executed by running the following code:

<!-- MACRO{snippet|id=RUNNING_SCENARIO|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

<a name="scenario-runner-builder"></a>
### Building Scenario Runner

Depending on the use case, it may be required to adjust the Scenario execution process. In order to do so, it is possible to build a custom Scenario Runner:  

<!-- MACRO{snippet|id=SCENARIO_RUNNER|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

Scenario builder allows to:

* Add [Scenario Listeners](#listeners)
* Add default [Exception handler](#exception-handling)
* Turn on Scenario [debugging](#debugging) 

<a name="listeners"></a>
### Scenario Listeners

The easiest way to hook into the Scenario execution process is by adding custom Scenario Listeners to the Scenario Runner builder:

<!-- MACRO{snippet|id=SCENARIO_LISTENER|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

Scenario Listener should extend an abstract class `RxScenarioListener`, which allows to override the following methods:
- `public void onScenarioStarted(ScenarioStartedEvent event)` - called right before the Scenario execution
- `public void onScenarioFinished(ScenarioFinishedEvent event)` - called right after the Scenario execution 
- `public void onFlowStarted(FlowStartedEvent event)` - called right before the Flow execution
- `public void onFlowFinished(FlowFinishedEvent event)` - called right after the Flow execution
- `public void onTestStepStarted(TestStepStartedEvent event)` - called right before the Test Step execution
- `public void onTestStepFinished(TestStepFinishedEvent event)` - called right after the Test Step execution

You can add multiple Scenario Listeners to the Scenario Runner builder, but you **must not** add the same Scenario Listener more than once.  

<a name="exception-handling"></a>
### Exception Handling

#### Exception Handlers

In order to handle exceptions that may get thrown in the test steps, Scenarios allow using custom Exception Handlers.
Exception Handlers are classes extending abstract class `RxExceptionHandler` and overriding method `Outcome onException(Throwable e)`.
This method **must** return one of the two possible exception handling outcomes:

- `RxExceptionHandler.Outcome.PROPAGATE_EXCEPTION` - exception should be propagated
- `RxExceptionHandler.Outcome.CONTINUE_FLOW` - flow execution should be continued

Two basic exception handlers for these outcomes are already provided:

- `RxExceptionHandler.PROPAGATE` - does nothing, only returns `Outcome.PROPAGATE_EXCEPTION`
- `RxExceptionHandler.IGNORE` - does nothing, only returns `Outcome.CONTINUE_FLOW`

#### Adding Exception Handlers

There are three main places where Exception Handlers can be added to using corresponding builder methods:

- `RxFlow` - handler for Flow test steps
- `RxScenario` - handler for the whole Scenario
- `RxScenarioRunner` - default handler for everything 

Here is an example with multiple Exception Handlers added to all three levels:

<!-- MACRO{snippet|id=EXCEPTION_HANDLERS|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

It is not allowed to add multiple Exception Handlers to the same builder. This should be done using `RxApi.compositeExceptionHandler`: 

<!-- MACRO{snippet|id=COMPOSITE_EXCEPTION_HANDLER|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

You can add as many regular Exception Handlers (added via `addExceptionHandler`) as you want, but there can be only one final Exception Handler. 
`RxCompositeExceptionHandler` will first execute all regular Exception Handlers ignoring their outcomes, and then final Exception Handler, whose outcome will be returned. 
If no final Exception Handler was set, then by default it will be set to the `RxExceptionHandler.PROPAGATE`.

The default Exception Handler added to the `RxScenarioRunner` works in a bit different way: since it is considered as *default*, 
it gets executed every single time, when an exception is thrown on (or propagated to) a Flow without any Exception Handlers.
It also executes if an exception was not handled anywhere around Scenario at all, i.e. it was thrown out of the Scenario.
If no default Exception Handler was set on Runner, then by default it will be set to the `RxExceptionHandler.PROPAGATE`.

#### Exception propagation

If a Test Step throws an exception, then it gets handled by the Exception Handler from the same Flow (or Subflow) to which that Test Step belongs to.
If there is no Exception Handler on the Flow where exception has been thrown, then it gets propagated up through the Exception Handler chain 
(Subflow → Flow → Scenario → Runner), until it reaches an Exception Handler that handles it and returns `Outcome.CONTINUE_FLOW`.
If none of the Exception Handlers will do so, then the exception will get thrown out of the scenario to the test method.

In the following example a test step `STEP_WITH_EXCEPTION` will throw an exception:

<!-- MACRO{snippet|id=EXCEPTION_PROPAGATION|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

Here is what will happen when the test step `STEP_WITH_EXCEPTION` gets executed:

* Exception gets handled by the `subFlowHandlerPropagate` (Subflow level)
* Handler `subFlowHandlerPropagate` propagates exception on the next level (Subflow -> Flow)
* Exception gets handled by the `flowHandlerPropagate` (Flow level)
* Handler `flowHandlerPropagate` propagates exception on the next level (Flow -> Scenario)
* Exception gets handled by the `scenarioHandlerIgnore` (Scenario level)
* Handler `scenarioHandlerIgnore` finally stops the exception propagation

Here are some alternative scenarios:

* If `subFlowHandlerPropagate` was `RxExceptionHandler.IGNORE`:
    * Subflow execution would be continued
    * All test steps would get executed (except the `STEP_WITH_EXCEPTION`)
* If `flowHandlerPropagate` was `RxExceptionHandler.IGNORE`:
    * Subflow execution would still be stopped, so `TEST_STEP_3` would not get executed
    * Flow execution would be continued, so `TEST_STEP_4` would get executed
* If `scenarioHandlerIgnore` was `RxExceptionHandler.PROPAGATE`:
    * An exception would get thrown out of the scenario in the test method

<a name="data-driven-testing"></a>
## Data Driven Testing

### Data Records

* Data Record is a Java Bean with one or more named fields
* Interface not Class (implementation resides inside Data Source engine)

<!-- MACRO{snippet|id=USER_WITHOUT_FIELD_NAMES|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

* For convenience it makes sense to define constants with field names to use them as input parameters in Test Steps:

<!-- MACRO{snippet|id=USER_WITH_FIELD_NAMES|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

* Serves Input to Test Step
    - Data Record.

    <!-- MACRO{snippet|id=DATA_RECORD|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->
    - Fields of Data Records

    <!-- MACRO{snippet|id=FIELDS_OF_RECORD|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->


<a name="data-sources"></a>
### Data Sources

Data Sources provide Data Records. Each Data Record is an input for one Test execution.

* Data Source is a collection of Data Records

* There are different ways of how to create Data Source. 
    * CSV files
    * Data from database/netsim
    * Java class
    * Java collection
    * e.t.c.

<!-- MACRO{snippet|id=DATA_SOURCE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

### Creating Data Source

<!-- MACRO{snippet|id=CREATING_DATA_SOURCE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

### Data Source example

* For example Flow needs to send email using 2 different users
* The simplest way to create a Data Source is from csv file. Each row will represent one Data Record:

<!-- MACRO{snippet|file=scenario-rx-taf-adaptor/src/test/resources/data/user.csv} -->

In a previous example we passed [parameters](#test-step-parameters) to Test Steps manually:

<!-- MACRO{snippet|id=ANNOTATED_METHOD_WITH_PARAMS|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

It's possible to load these parameters from Data Source, by defining Data Source on flow:

<!-- MACRO{snippet|id=FROM_CSV_DATASOURCE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

Flow "send email" will be executed 2 times with the users from "file.csv":

<div class="note"></div>
Data sources must be added to a flow after all [test steps](#test-steps). 

<img src="images/data_source_example.gif" />

If Data Source field names match Test Step parameters it will be injected automatically. If parameter names differ, it is possible to [explicitly define](#test-step-reuse) which Data Source to use.

### Filtering Data Sources

It is possible to filter Data Sources by field of Data Records by using `equalTo` and `contains` methods. 

In this example only users that have field `ENABLED` set to true will be used:

<!-- MACRO{snippet|id=SIMPLE_FILTERING|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

In some cases it makes sense to create filtered Data Source sub definition. For example, Data Source `allUsers` contains all users. In Flow "enabledUsers" there is a requirement to use only `ENABLED` users, but in Flow "onlyAdmins" use only users with `ADMINISTRATOR` role:

<!-- MACRO{snippet|id=ADVANCED_FILTER_EXAMPLE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

## Flow execution

In order to improve scenario readability and make its structure consistent across all teams, flow builder has defined structure: 

* [Test Steps](#test-steps) are defined first
    * Optional [before steps](#before-after)
    * At least one test step
        * Optional [Always Run](#always-run) can follow 
    * Optional [after steps](#before-after)
* [Data Sources](#data-sources) and [vUsers](#vusers) are defined in the end in any order

### Flow Branching

Test Flows can include each other using `.subFlow` builder method

#### Subflow

* Mechanism to execute a Flow inside another Flow
* Runs with the same vUser as the main flow
* Reusability
* Flexibility

<!-- MACRO{snippet|id=SUBFLOW_EXAMPLE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

<img src="images/subflow.gif" />

<a name="split"></a>
#### Split

<!-- MACRO{snippet|id=SPLIT_EXAMPLE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

* Split – run different SubFlows in parallel
* ⚠ SubFlows will run with different vUser
* Can be used on scenario or flow

<img src="images/split.gif" />

<a name="vusers"></a>
### Multiple vUsers

To simulate multiple users working simultaneously, Flows may be executed in parallel using vUsers. Each vUser represents one user that accesses system.

<!-- MACRO{snippet|id=MULTIPLE_VUSERS|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

All Test Steps will be executed in parallel by two users:

<img src="images/vusers.png" />

### Combining vUsers and Data Sources

If Data Source and vUsers are set on one Flow, it will be executed `Data Records * vUsers count`. For example, if `dataSource` Data Source contains 3 records and Flow has 2 vUsers:

<!-- MACRO{snippet|id=VUSERS_AND_DATA_SOURCE_EXAMPLE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

Flow will be executed 2 * 3 = 6 times:

<img src="images/vusers_1.png" />

To improve performance it is possible to process one Data Source by Multiple vUsers. This is possible by [sharing](#shared-data-sources) Data Source between vUsers: `withDataSources(dataSource.shared())`. 

In this case each vUser will get only one Data Record 

<img src="images/vusers_2.png" />

<a name="vusers-auto"></a>
### Automatic vUsers

As an alternative to manually specifying the number of [vUsers](#vusers), you can use a convenience method `withVUsersAuto()`, which will automatically determine the number of vUsers required to run the [data sources](#data-sources) in parallel:

<!-- MACRO{snippet|id=AUTOMATIC_VUSERS_EXAMPLE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

* `withVUsersAuto()` must be used with **at least one** data source per flow
* All Data Sources in a flow with `withVUsersAuto()` must be [**shared**](#shared-data-sources) 

### Multiple Data Sources

It is possible to define multiple Data Sources on Flow and SubFlow hierarchy level.

<a name="multiple-data-sources-flow"></a>
#### Multiple Data Sources in the Same Flow

* Aggregation inside of the same Flow

<!-- MACRO{snippet|id=MULTIPLE_DATA_SOURCES_IN_THE_SAME_FLOW_EXAMPLE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

The number of times each Test Step will be repeated is equal to Data Record count in the smallest Data Source:

<img src="images/multiple_ds.gif" />

It is also possible to repeat flow for Data Record count in the *largest* Data Source using [cyclic](#cyclic-data-sorces).

#### Nested Data Sources

* multiplication Flow and subflow level

<!-- MACRO{snippet|id=NESTED_DATA_SOURCES_EXAMPLE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

<img src="images/nested_ds.gif" />

<a name="shared-data-sources"></a>
### Shared Data Sources

In regular Data Source each vUser will iterate through all Data Source records. As opposed *in Shared Data Source*, each record will be processed only by one of vUsers.

Shared Data Source might be used:

* When Data Records represent resources which could not be accessed by multiple users at same time (for example nodes)
* To speed up Data Source processing by adding Shared Data source and [vUsers](#vusers) to Flow (see also [Automatic vUsers](#vusers-auto)).

"Shared" is a strategy which defines how Data Source will be processed in the scope of one flow execution.

#### Shared Data Source in multiple flows

If you use the same shared Data Source in multiple flows, each flow will split Data Records of the Data Source among vUsers of that flow.

<!-- MACRO{snippet|id=SHARED_DATA_SOURCE_IN_MULTIPLE_FLOWS|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/RxSharedDataSourcesExample.java} -->

Output in both cases `[subFlow1: Data Record 1, subFlow1: Data Record 2, subFlow2: Data Record 1, subFlow2: Data Record 2]`

Same logic applies when running two flows in parallel.

<!-- TODO graph -->

<!-- MACRO{snippet|id=SHARED_DATA_SOURCE_IN_MULTIPLE_FLOWS_RUNNING_TWO_FLOWS_IN_PARALLEL|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/RxSharedDataSourcesExample.java} -->

Output will always be: `[parallelFlow1: Data Record 2, parallelFlow1: Data Record 1, parallelFlow2: Data Record 1, parallelFlow2: Data Record 2]`

<!-- TODO graph -->

#### Shared Data Source in Sub Flows

If Data Source is shared in a Sub Flow, Subflow will share its Data Source on each parent iteration.

<!-- MACRO{snippet|id=SHARED_DATA_SOURCE_IN_SUB_FLOWS_EXAMPLE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/RxSharedDataSourcesExample.java} -->

Output will be: `[flow1: Data Record 1, flow1: Data Record 2, flow1: Data Record 1, flow1: Data Record 2]`

<!-- TODO graph -->

<a name="cyclic-data-sorces"></a>
#### Cyclic Data Sources

If Flow has two Data Sources with different size, [repeat will be equal to Data Record count in the smallest Data Source](#multiple-data-sources-flow). 
It is also possible to repeat flow for Data Record count in the *largest* Data Source by repeating the smallest Data Source multiple times.

For example given two Data Sources:

**users.csv**

<!-- MACRO{snippet|file=scenario-rx-taf-adaptor/src/test/resources/data/user.csv} -->

**nodes.csv**

<!-- MACRO{snippet|file=scenario-rx-taf-adaptor/src/test/resources/data/node.csv} -->

Following Flow will execute two times with combination: `admin1 - SGSN-14B` and `admin2 - LTE01ERB`

<!-- MACRO{snippet|id=NON_CYCLIC_DATA_SOURCE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

It is also possible to "cycle" `admins` Data Source to provide rotation of users who work with node:

<!-- MACRO{snippet|id=CYCLIC_DATA_SOURCE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

In this case Flow will execute for all `nodes`, repeating `admin1` twice: `admin1 - SGSN-14B`, `admin2 - LTE01ERB` and `admin1 - LTE08dg2`

<div class="note"></div>
At least one of Data Sources defined on one Flow should be not cyclic to avoid forever loop

<a name="context-data-sorces"></a>
### Pass Data Between Flows

* Passing data between Flows via context may be confusing if Flows have different vUser count, so it is restricted.
* It is possible to pass data between Flows using Data Source reference
* ContextDataSourceDefinition - Initially empty, can be populated only by framework (collectResultsToDataSource)
* Framework may have validation to avoid different flows writing to the same DataSource etc

<!-- MACRO{snippet|id=PASS_DATA_BETWEEN_FLOWS|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

If Test Steps return collection of Data Records, all will be added to Data Source:

<!-- MACRO{snippet|id=RETURN_COLLECTION_DATA_RECORDS|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

<!-- MACRO{snippet|id=PASS_COLLECTION_OF_DATA_BETWEEN_FLOWS|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->



### AlwaysRun/Setup/Finalization

Setup and Finalization Test Steps have specific behavior:

<a name="always-run"></a>
#### Always Run

By default, if a Test Step fails (e.g. because of a failed assertion), scenario execution stops, and exception is propagated to test method. It is possible to mark Test Steps with `.alwaysRun` so it will be executed even in case if previous test steps failed.

* Regular Test Step
* Gets Data Record
* Cleanup for vUser
* Recommended way to do cleanup

<!-- MACRO{snippet|id=ALWAYS_RUN_EXAMPLE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

<img src="images/always_run.gif" />

<a name="before-after"></a>
#### Before and After Steps

Before and After Steps are executed once per flow regardless of the actual vUser and Data Record count.  
⚠ Before and After Steps cannot have any arguments!  
⚠ Before Steps must be called before any other test steps, as a first method of the FlowBuilder.  
⚠ After Steps must be called after the last test step, it is not possible to add any Test Steps after them.  

<!-- MACRO{snippet|id=BEFORE_AND_AFTER_STEPS_EXAMPLE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

<img src="images/before_after.gif" />

## Scenario Context

<!-- TODO picture -->

Scenario Context 

* Context scope is Flow or Scenario (scenario is just a Flow with different builder). To pass values in scope of a suite - use Java/Test Framework capabilities (see next chapter)
* Context works like environment variables in OS. `TestStep#return` will make value available in *subsequent Test Steps of Same Flow* and *Sub Flows of this Flow* but *not* in parent Flow, next Flow or test method.
* This makes context a way to pass values between Test Steps of one Flow Data Record execution by one vUser. In case if there is a requirement to pass data between Flows, Data Source should be used (see next chapter).
* There's no way to set some values before Scenario started, but like Test Steps, Scenario should have withParameter method:

<!-- MACRO{snippet|id=SCENARIO_CONTEXT_EXAMPLE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

## More on Test Steps

<a name="test-step-parameters"></a>
### Test Step Parameters

Test Step parameters are method arguments annotated with `@Input` or `@Output` annotation.

<!-- MACRO{snippet|id=TEST_WITH_PARAMETER|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

Read more on other sources of parameters in the [next section](#test-step-reuse)

### Passing Data Between Test Steps

* `@Input` parameters can be injected from Scenario Context
* Treat Context like Data Source with one Data Record
* Usage of DataRecordBuilder should be a common practice

<!-- MACRO{snippet|id=PASSING_DATA_BETWEEN_STEPS|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ContextExample.java} -->

### Return multiple objects
<!-- MACRO{snippet|id=RETURN_MULTIPLE_OBJECTS_EXAMPLE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ContextExample.java} -->

<a name="test-step-reuse"></a>
### Reuse object in different steps in scope of flow

Test steps can return any object or data records: 

<!-- MACRO{snippet|id=RETURN_ANY_OBJECT_OR_DATA_RECORD|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

These results could then be passed to subsequent Test Steps of flow:

<!-- MACRO{snippet|id=REUSE_OBJECT|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

<div class="note"></div>
Passing mechanism works only in the scope of one flow and vUser. It will throw exception if we add one more flow to the scenario:

<!-- MACRO{snippet|id=IN_THE_SCOPE_OF_ONE_FLOW|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->


To pass data between flows use [Data Sources](#context-data-sorces).

## Adapting Test Steps

When reusing Test Steps, it is recommended to "adapt" Test Step for your testware. For example if you plan to reuse Test Step `CONSUMER` from [previous example](#test-step-parameters) in multiple places, but input fields "object1" and "object2" should be taken from known dataSource, its possible to save definition with adapted params:

<!-- MACRO{snippet|id=ADAPTING_TEST_STEPS|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ContextExample.java} -->

## Renaming Data Source

If Test Steps use Data Source Name directly in `@Input` parameters, it is possible to rename Data Source to match Test Steps in scope of Flow. 

For example, Data Source `differentNameDataSource` used in Flow, where Test Step expects Data Source with name `users_datasource`:

<!-- MACRO{snippet|id=DATA_RECORD|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

Data Source `differentNameDataSource` could be renamed to `users_datasource` in scope of Flow:

<!-- MACRO{snippet|id=RENAME_DATASOURCE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

<a name="reporting"></a>
## Reporting

By default each test method appears as a test case in test report.

### Data Driven Scenarios

It is possible to treat every data record in your Data Source as separate test case, scenario to be treated as test suite. Test Case ID is provided by data source - field "testCaseId".
Add Data Source in special Scenario builder - `dataDrivenScenario()`. When using `dataDrivenScenario()`
Test Steps will have no need to have `@TestId` annotation in argument. Scenario has following code:

<!-- MACRO{snippet|id=SIMPLE_DATA_DRIVEN_SCENARIO|file=scenario-rx-data-driven/src/test/java/com/ericsson/de/scenariorx/examples/DataDrivenScenarioExample.java} -->

Assuming that Data Source has following data records:

```
testCaseId
theTestId0
theTestId1
```

This results in Allure report with 2 Test Cases:
- theTestId0 with testStep1 and testStep2
- theTestId1 with testStep1 and testStep2
As shown in screenshot:

<img src="images/dds1.png" />

There is no need for `@TestId `annotation on test method any more. But you need a `@TestSuite` annotation. This suite
will group all test cases (executed in scenario) in Allure report. `dataDrivenScenario` has additional validations to ensure that Data Source is set and
it has field `testCaseId`.

#### Multiple Flows

In case `dataDrivenScenario` will have multiple flows:

<!-- MACRO{snippet|id=DATA_DRIVEN_MULTIPLE_FLOWS|file=scenario-rx-data-driven/src/test/java/com/ericsson/de/scenariorx/examples/DataDrivenScenarioExample.java} -->

Result will be the same as in previous case.

#### Multiple vUsers

In case `dataDrivenScenario` or Flow will have multiple vUsers, Test Steps will be grouped in one Test Case. This will result in Allure
report with 2 Test Cases:
- theTestId0
  * testStep1 (Data Record 1)
  * testStep2 (Data Record 1)
- theTestId1
  * testStep1 (Data Record 2)
  * testStep2 (Data Record 2)

As shown in a screenshot:

<img src="images/dds2.png" />

<a name="debugging"></a>
## Debugging Scenario

There are two main ways how you can get debugging assistance from Scenarios:

- By enabling [debug logging](#logging)
- By turning on [Scenario execution graph visualization](#visualization)

<a name="logging"></a>
### Debug logging

- Produces additional verbose logging information about:
    - Flow name and associated Data Source metadata and contents
    - Test Step name and currently executed Data Record and context

- An example for a simple Scenario with two flows:

```
2017-03-07 15:33:08,662 [main] [INFO] [Implementation] Running Flow 'flow' with shared Data Source 'letters' consisting of 3 Data Records: [[{"letters":{"letters":"A"}},{}], [{"letters":{"letters":"B"}},{}], [{"letters":{"letters":"C"}},{}]]

2017-03-07 15:33:09,607 [main] [INFO] [Implementation] Running Flow 'subflow' with shared Data Source 'numbers' consisting of 3 Data Records: [[{"numbers":{"numbers":1}},[{"letters":{"letters":"A"}},{}]], [{"numbers":{"numbers":2}},[{"letters":{"letters":"B"}},{}]], [{"numbers":{"numbers":3}},[{"letters":{"letters":"C"}},{}]]]

2017-03-07 15:33:09,653 [scenario.flow.subflow.vUser-1.3.1] [INFO] [Implementation] Running Test Step 'step' with Data Record [{"numbers":{"numbers":3}},[{"letters":{"letters":"C"}},{}]] and context {scenario.debug.log.enabled=true}
2017-03-07 15:33:09,653 [scenario.flow.subflow.vUser-1.1.1] [INFO] [Implementation] Running Test Step 'step' with Data Record [{"numbers":{"numbers":1}},[{"letters":{"letters":"A"}},{}]] and context {scenario.debug.log.enabled=true}
2017-03-07 15:33:09,658 [scenario.flow.subflow.vUser-1.2.1] [INFO] [Implementation] Running Test Step 'step' with Data Record [{"numbers":{"numbers":2}},[{"letters":{"letters":"B"}},{}]] and context {scenario.debug.log.enabled=true}
```

- Debug logging can be enabled in two ways:
    - By using [Scenario Runner builder](#scenario-runner-builder) method `withDebugLogEnabled()`:  
    
    <!-- MACRO{snippet|id=DEBUG_LOGGING|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->
    - By adding a system property `-Dscenario.debug.log.enabled=true` (it will override builder method)

<a name="visualization"></a>
### Scenario execution graph visualization

- Allows to see how did the Scenario execution happen by generating a graph:
    - Flows and Test Steps are represented as graph vertices
    - Sequential Scenario execution progresses from graph top to bottom
    - Parallel flow execution via multiple vUsers is depicted as vertex branching
    - Test Steps metadata can be seen in a tooltip upon *hovering or clicking* a corresponding vertex
    - Test Steps having errors will be highlighted in red and have stack trace in a tooltip

- Scenarios have two semantically identical graph visualization formats available:
    - **GraphML**
        - Can be viewed in [yEd](https://www.yworks.com/products/yed) tool (free desktop graph editor for all major platforms)
        - yEd does not apply any colorings and layouts by default, it can be done by:
            1. Opening menu "Edit" -> "Properties Mapper..."
            1. Importing Scenario Properties Mapper configuration (`scenario-rx-core\src\test\resources\yEd-properties-mapper.cnfx`)
            1. Selecting and applying configurations "TAF Scenario (Node)" and "TAF Scenario (Edge)"
            1. Clicking on menu "Layout" -> "Hierarchical" or pressing hot key "Alt + Shift + H"
    - **SVG**
        - Can be viewed directly in the browser (Google Chrome is preferred)
        - ⚠ **Note:** Tooltips will not be available in Internet Explorer!

- Scenario execution graph visualization can be turned on in two ways:
    - By using [Scenario Runner builder](#scenario-runner-builder) method `withGraphExportMode()` with one of the `enum ScenarioRunner.GraphExportMode` values:
    
    <!-- MACRO{snippet|id=DEBUG_GRAPH|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->
    - By adding a system property `-Dscenario.debug.graph.mode=<MODE>` (it will override builder method), where `<MODE>` can be one of the:
        - `none` - suppresses graph generation (useful for overriding `DEBUG_GRAPH_MODE`)
        - `graphml` - generate Scenario execution graph in GraphML format 
        - `svg` - generate Scenario execution graph in SVG format
        - `all` - generate Scenario execution graph all available formats

- You can get more information about visualization of Scenarios on a [TAF Confluence](https://confluence-nam.lmera.ericsson.se/display/TAF/Visualization+of+Scenarios)

<!-- TODO ## FAQ -->
<!-- TODO ## Creating Reusable Shared Flows -->
<!-- TODO ## Organizing Test Steps Flows etc -->

## Best practices

* Do not use threads or thread pools in a testware. This will interfere with existing vUser functionality. Please use Scenarios built-in mechanisms (split) to achieve this.
* To improve readability please use multiple lines when defining Scenario options
* Try to create as few wrappers as possible

## Advanced Topics

<a id="batch"/>
### vUser - Data Record allocation

[vUsers](#Multiple_vUsers) define how many Flow instances will run in parallel. If Flow has vUsers and [shared Data Source](#Shared_Data_Sources) set, vUser count defines how many Data Source records will be processed in parallel. For thread safety considerations, Data Source records will be processed in batches, where batch size equals vUser count.

For example, a Flow called with 3 vUsers and shared Data Source with 8 Data Records - `DR1`, `DR2`, `DR3`, `DR4`, `DR5`, `DR6`, `DR7`, `DR8`. It will be separated into 3 batches:

|         | vUser 1 | vUser 2 | vUser 3 |
|---------|---------|---------|---------|
| Batch 1 | `DR1`   | `DR2`   | `DR3`   |
| Batch 2 | `DR4`   | `DR5`   | `DR6`   |
| Batch 3 | `DR7`   | `DR8`   | `-`     |

To simplify debugging parallel Test Steps in testware, **vUser/Data Record allocation is predictable and reproducible**. Data Record **distribution is sequential**, i.e. vUser 1 will always gets first Data Record, vUser 2 - second etc. Data Record **processing is parallel**- i.e. vUsers will process these Data Records at same time. In other words, vUser will get the same Data Record on each Scenario run. This does not apply to cases when Data Source can have different Data Record order on each Scenario run (for example [Context](#Pass_Data_Between_Flows) Data Source).

Another reason for batch processing is requirement for predictable exception/failed assertion handling. If one or more vUsers gets exception while executing Test Step, other vUsers will finish processing Test Steps in current batch normally, next batch will not be started.

Important thing here is that to achieve predictable Data Record allocation, **each batch will start only after previous batch processing is finished**. For example, if vUser 3 finishes processing `DR3` earlier than vUser 1 `DR1` and vUser 2 `DR2`, it will not start processing next Data Record `DR6`, until all Data Records in current batch will be processed by vUser 1 and vUser 2. Given example will have the following timeline:

|-|-|
| <!-- MACRO{snippet|id=BATCH_SYNC|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/SyncExamples.java} -->  | <img src="images/batch_sync.svg"/>  |

This is a trade-off "Predictability vs Speed", that may cause Scenario to execute slightly longer than possible, because some **vUsers may be idle** while other vUsers are processing current batch. Speed trade-off is minimal, however Rx Scenarios **may not be applicable for Performance and Load Testing** as it does not create maximum possible load on SUT. You may consider [Legacy Scenario as alernative](http://taf.lmera.ericsson.se/taflanding/userdocs/Latest/taf_concepts/taf_scenarios/performance_testing.html) for Performance Testing.

#### Speeding Up Data Source processing

In the case where some DataRecords require significantly more time for processing than others, it may delay other vUsers which will be idle until all Records in batch are processed. To decrease Scenario execution time, it is possible to process such Data Records in parallel (split flow).

In example below, Data Source `largeDataSource` is split into two parts - `part1` with type `ERBS` and `part2` with `RadioNode` assuming that processing of different nodes requires different times. Flow `processNode` executed with `part1` is not dependent on `processNode` executed with `part2`. In other words, vUsers processing batch of `part1` do not wait for vUsers processing slower batch of `part2`.

|-|-|
| <!-- MACRO{snippet|id=SPEED_UP|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/LargeDataSourceParallelProcessingExample.java} -->  | <img src="images/speedup.svg"/>  |

### Test Step Synchronization

Test Step execution is **not synchronized** between vUsers. This means that vUser will try to process Test Steps of Flow as soon as possible. In example below, after completing Test Step `TS1`, vUser 3 will start executing next Test Step `TS2` even if other vUsers haven't finished processing `TS1` yet:

|-|-|
| <!-- MACRO{snippet|id=TEST_STEP_NOT_SYNC|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/SyncExamples.java} -->  | <img src="images/test_step_not_sync.svg"/>  |

### Subflow Synchronization

Because [subflows](#Subflow) may have [shared Data Source](#Shared_Data_Sources) set, all subflow vUsers should start at the same time (see [Batch Mechanism](#batch)). Additionally Flows may have [Before and After Steps](#Before_and_After_Steps) defined which should execute only once, regardless of [vUser](#Multiple_vUsers) count. Because of those reasons, **Subflow start is synchronized** i.e. all parent Flow Test Steps *before* sublow must complete before subflow Test Steps start.

|-|-|
| <!-- MACRO{snippet|id=SUBFLOW_SYNC|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/SyncExamples.java} -->  | <img src="images/subflow_sync.svg"/>  |

### Repeat Flow with criteria

It is possible to repeat the execution of a Flow whilst user defined criteria is being satisfied. This is achieved using the Flow Builder method `runWhile(Predicate<RxDataRecordWrapper> predicate)` which takes a [Predicate](https://google.github.io/guava/releases/20.0/api/docs/com/google/common/base/Predicate.html)
as a parameter. The Flow will repeat as long as the user defined Predicate returns true and will cease execution when this criteria is no longer satisfied.

Key points regarding `runWhile`:

1. If all Data Records are processed and predicate returns true, Flow will loop/repeat Data Source until predicate returns false.
2. If predicate returns false on first call, the flow **will not** be executed.

**Example 1:** Flow iterates through datasource whilst a certain field value is not found. Flow will cease execution once found.
<!-- MACRO{snippet|id=REPEAT_WHILE_VALUE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

**Example 2:** Run Flow until a certain count is reached. Datasource contains four Data Records and will repeat whilst the count condition is satisfied.
<!-- MACRO{snippet|id=REPEAT_WHILE_COUNT|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/ScenarioExamples.java} -->

