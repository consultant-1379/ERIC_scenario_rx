It's possible to have RxScenarios and Legacy Scenarios in one test ware simultaneously to allow gradual migration. Backwards compatibility is achieved on Test Step level. I.e. same Test Steps could be used in both RxScenarios and LegacyScenarios. Migration could be done in 2 phases. When all flows that use Test Steps are migrated to RxScenarios, phase 2 could be started.

## What's new/Highlights

* To make scenario structure consistent, builders have [defined order](index.html#Flow_execution) (i.e. you can't call `addTestStep` on Flow after calling `withDataSources` ...)
* New flexible API for 
    - [Test Step Parameters](index.html#Test_Step_Parameters) from Context, Data Sources, previously executed Test Steps...
    - [Data Source filtering](index.html#Filtering_Data_Sources)
* Changes in shared and cyclic [Data Source strategy](#ChangesinSharedDataSourceStrategy) that address depletion (exhaustion) issue
* [withVUsersAuto()](index.html#Automatic_vUsers) method, which sets vUser count according to Data Source size
* Store Data Source by definition with type to improve readability/reusability
* Predictable and reproducible Data Record/vUser allocation
* More robust implementation 
* ...

## Phase I 
1. Create separate class for Rx Flows e.g. `MainUiFlows` → `MainUiFlowsRx`
2. Pick and copy one Flow to the newly created class
3. [Replace imports](#class_compatibility)
3. Update [Syntax differences](#syntax_differences)
4. Check that scenario and flow builders matches [defined order](index.html#Flow_execution)
5. Review shared [Data Source strategy](#ChangesinSharedDataSourceStrategy)
4. Remove all methods that contains manipulations with Data Sources:
    - `shareDataSource`
    - `resetDataSource`
    - `copyDataSource`
3. Review if Test Steps contain vUser dependent actions and if possible [replace them](#Replacing_vUser_dependent_logic)
4. If Test Steps produces data, [update return values](#MigratingTestSteps)
4. [Check Data Source filters](#CheckDataSourcefilters)
4. If subflow have Data Sources and vUsers it should be [passed as arguments](https://confluence-nam.lmera.ericsson.se/display/TAF/Context+and+Data+Sources#ContextandDataSources-PassDataBetweenFlows)
3. Repeat process for all subflows in a given flow.
5. Replace `runner.start` with `TafRxScenarios.run`

## Phase II

Starts when all flows that use Test Steps are migrated to RxScenarios.

1. Removal of Context and Data Sources from Test Steps

<a name="syntax_differences"></a>
### Syntax differences

|                        Legacy Scenarios                       |                  Rx Scenarios                 |
|---------------------------------------------------------------|-----------------------------------------------|
| dataSource(name)                                              | dataSource(name, DataRecordClass.class)       |
| dataSource(...).withFilter("field='value'")                   | dataSource(...).filterField("field", "value") |
| runner().withListener(new LoggingScenarioListener()).build(); | runner().withDebugLogEnabled()                |
| flow(...).beforeFlow                                          | flow(...).withBefore (now accepts Test Steps) |
| withVUsers(...long code to determine DS size...)              | withVUsersAuto()                              |
|                                                               |                                               |

<a name="MigratingTestSteps"></a>
### Migrating Test Steps

1. Test step should not contain vUser specific logic
2. Usage of TestContext is deprecated. To put/get values from context use approach described in [Rx Scenario Context and Data Sources](https://confluence-nam.lmera.ericsson.se/display/TAF/Context+and+Data+Sources)
3. If test step produces any data (i.e. HttpTool, Browser, Node, Alarm), it should [return it](https://confluence-nam.lmera.ericsson.se/display/TAF/Context+and+Data+Sources#ContextandDataSources-Reuseobjectindifferentstepsinscopeofflow)

<a name="CheckDataSourcefilters"></a>
### Check Data Source filters    

* There should not be any Dynamic Fields i.e. TafTestContext, static variables, ThreadLocals etc.

<a name="operatorsReplacing_vUser_dependent_logic"></a>
### Replacing vUser dependent logic

Binding logic to vUser id is a bad practice, because vUser id may change in scope of flow reuse/refactoring and cause complex issues.
In Rx Scenarios, `TafTestContext.getContext().getVUser()` will throw exception. In most testwares vUser is needed for logging purposes. In Rx Scenarios its not actual, as logging contains vUser. Example logging message:

`2016-09-21 14:05:34,620 [scenarioName.flowName.subflowName.vUser-1] [INFO] [com.ericsson.cifwk.taf.scenario.impl.TafDataSourceDefinition] Log Test`
        
If vUser is used for other purposes, same functionality can be achieved using Data Source with explicit and predictable values:

```java
DataSourceDefinition<Integer> vUserEmulation = fromIterable("vUser", asList(1, 2)).shared().build();

flow("flow")
        .addTestStep(annotatedMethod(this, "VUSER_DEPENDENT_TEST_STEP"))
        .withDataSources(vUserEmulation)
        .withVUsers(2);

@TestStep(id = "VUSER_DEPENDENT_TEST_STEP")
public void vUserDependentTestStep(@Input("vUser") Integer vUser) {
}

```

### Migrating reusable flows

* Method should *not* return flow builder, but flows instead
* All customizable flow attributes (vUser, DataSources) should be passed as method arguments
* See example in [Pass Data Between Shared Flows](https://confluence-nam.lmera.ericsson.se/display/TAF/Context+and+Data+Sources#ContextandDataSources-PassDataBetweenFlows)

<a name="class_compatibility"></a>
### Class compatibility  

|                                     |      Scenarios      |                                   |    Rx Scenarios    |
|-------------------------------------|---------------------|-----------------------------------|--------------------|
| com.ericsson.cifwk.taf.scenario     | TestScenario        | com.ericsson.de.scenariorx.api | RxScenario         |
| com.ericsson.cifwk.taf.scenario     | TestScenarioRunner  | com.ericsson.de.scenariorx.api | RxScenarioRunner   |
| com.ericsson.cifwk.taf.scenario     | TestStepFlow        | com.ericsson.de.scenariorx.api | RxFlow             |
| com.ericsson.cifwk.taf.scenario.api | TestStepFlowBuilder | com.ericsson.de.scenariorx.api | Builder&lt;RxFlow> |

#### Legacy Scenarios:  

Usual imports are

```java
import com.ericsson.cifwk.taf.datasource.TestDataSource;
import com.ericsson.cifwk.taf.scenario.TestStepFlow;
import com.ericsson.cifwk.taf.scenario.TestScenario;

import static com.ericsson.cifwk.taf.scenario.TestScenarios.scenario;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.flow;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.annotatedMethod;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.dataSource;
```

#### Rx Scenarios:

Usual imports are

```java
import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.api.RxDataSourceDefinition;
import com.ericsson.de.scenariorx.api.RxFlow;

import static com.ericsson.de.scenariorx.api.TafRxScenarios.flow;
import static com.ericsson.de.scenariorx.api.TafRxScenarios.scenario;
import static com.ericsson.de.scenariorx.api.TafRxScenarios.annotatedMethod;
import static com.ericsson.de.scenariorx.api.TafRxScenarios.dataSource;
```

All builders are located in one class - `TafRxScenarios`, so when starting defining scenario it might make sense to static import all API methods (IDE should replace it with individual methods before commit):

```java
import static com.ericsson.de.scenariorx.api.*;
import static com.ericsson.de.scenariorx.api.TafRxScenarios.*;
```

<a name="ChangesinSharedDataSourceStrategyoperators"></a>
### Changes in Shared Data Source Strategy

* In Legacy Scenarios scope of Shared Data Source was scenario. I.e. any flow/vUser which took Data Records from Shared Data Source could deplete it. So some flows wouldn't execute at all. 
* It possible to switch DS state (shared⇄not shared) or reset it, which is very implicit and complex to understand.

In Rx Scenario Data Source is shared in the scope of one flow. "Shared" is not an attribute of Data Source, but a strategy of how it will be processed in Flow.

* No need for shareDataSource/reset/copy
* It is impossible to switch state of Data Source shared⇄not shared
* To make Data Source shared *in the scope of one flow* use `.shared()`

Difference will be explained in following examples. Given `SHARED_DATA_SOURCE` contains 2 Data Records: `1` and `2`. Test Step `"STEP"` outputs flow name and Data Record:

```java
@TestStep(id = "STEP")
public void step(@Input("name") String name, @Input("integer") Integer integer) {
    stack.push(name + ": Data Record " + integer);
}
```

#### Data Source depletion

In this example Data Source will be depleted after the first flow execution.

```java
// Legacy scenario
TestScenario scenario = scenario("Scenario1")
        .addFlow(
                flow("parent")
                        .addSubFlow(
                                flow("subFlow1")
                                        .addTestStep(
                                                annotatedMethod(this, "STEP")
                                                        .withParameter("name", "subFlow1")
                                        )
                                        .withVusers(2)
                                        .withDataSources(dataSource(SHARED_DATA_SOURCE))
                        )
                        // SubFlow2 will not be executed
                        .addSubFlow( //
                                flow("subFlow2")
                                        .addTestStep(
                                                annotatedMethod(this, "STEP")
                                                        .withParameter("name", "subFlow2")
                                        )
                                        .withVusers(2)
                                        .withDataSources(dataSource(SHARED_DATA_SOURCE))
                        )
        )
        .build();
```

Only first flow Test Steps will be executed: `[subFlow1: Data Record 1, subFlow1: Data Record 2]`.

In Rx Scenario each flow will have its own instance of Shared Data Source:

<!-- MACRO{snippet|id=SHARED_DATA_DEPLETION|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/RxSharedDataSourcesExample.java} -->

In this case both flows will be executed: `[subFlow1: Data Record 1, subFlow1: Data Record 2, subFlow2: Data Record 1, subFlow2: Data Record 2]`

#### Depletion difference

Because of backwards compatibility, Shared Data source behaves differently between flows and subflows. Between flows on scenario level Data Sources are reset.

In fist example, subFlow2 will not be executed.

```java
// Legacy scenario
 TestScenario scenario = scenario("Scenario1")
                .addFlow(
                        flow("parent")
                                .split(
                                        flow("subFlow1")
                                                .addTestStep(annotatedMethod(this, "STEP")
                                                        .withParameter("name", "subFlow1"))
                                                .withVusers(2)
                                                .withDataSources(dataSource(SHARED_DATA_SOURCE)))
                                .split(//this subFlow will not be executed
                                        flow("subFlow2")
                                                .addTestStep(annotatedMethod(this, "STEP")
                                                        .withParameter("name", "subFlow2"))
                                                .withVusers(2)
                                                .withDataSources(dataSource(SHARED_DATA_SOURCE))))
                .build();
```

Output: `[subFlow1: Data Record 1, subFlow1: Data Record 2]`

In the second example, Shared Data Source is reset between flows on a Scenario level:

```java
// Legacy scenario
TestScenario scenario2 = scenario("Scenario2")
        .addFlow(
                flow("flow1")
                        .addTestStep(annotatedMethod(this, "STEP")
                                .withParameter("name", "flow1"))
                        .withVusers(2)
                        .withDataSources(dataSource(SHARED_DATA_SOURCE)))
        .addFlow(//thiswillbeexecuted
                flow("flow2")
                        .addTestStep(annotatedMethod(this, "STEP")
                                .withParameter("name", "flow2"))
                        .withVusers(2)
                        .withDataSources(dataSource(SHARED_DATA_SOURCE))).build();
```

All flows will be executed: `[flow1: Data Record 1, flow1: Data Record 2, flow2: Data Record 1, flow2: Data Record 2]`

In Rx Scenarios all flows will be executed in both cases:

<!-- MACRO{snippet|id=SHARED_DATA_SOURCE_IN_MULTIPLE_SUBFLOWS|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/RxSharedDataSourcesExample.java} -->

<!-- MACRO{snippet|id=SHARED_DATA_SOURCE_IN_MULTIPLE_FLOWS|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/RxSharedDataSourcesExample.java} -->

Output in both cases `[subFlow1: Data Record 1, subFlow1: Data Record 2, subFlow2: Data Record 1, subFlow2: Data Record 2]`

#### Hierarchical Depletion

In the next example although `GLOBAL_DATA_SOURCE` contains multiple records, `subFlow1` will only execute for first one, since its shared Data Source will be depleted the first execution:

```java
// Legacy scenario
TestScenario scenario = scenario("Scenario1")
        .addFlow(
                flow("parent")
                        .addSubFlow(
                                flow("subFlow1")
                                        .addTestStep(annotatedMethod(this, "STEP")
                                                .withParameter("name", "flow1"))
                                        .withDataSources(dataSource(SHARED_DATA_SOURCE))
                        )
                        .withVusers(2)
                        .withDataSources(dataSource(GLOBAL_DATA_SOURCE)) // 2 Data Records
        )
        .build();

```

Output will be: `[flow1: Data Record 1, flow1: Data Record 1]`

In Rx Scenario each `subFlow1` execution will have its own instance of Shared Data Source:

<!-- MACRO{snippet|id=SHARED_DATA_SOURCE_IN_SUB_FLOWS_EXAMPLE|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/RxSharedDataSourcesExample.java} -->

Output will be: `[flow1: Data Record 1, flow1: Data Record 2, flow1: Data Record 1, flow1: Data Record 2]`

#### Parallel Depletion

Two parallel running Flows will both deplete SHARED data Source:

```java
// Legacy scenario
TestScenario scenario = scenario("Scenario1")
        .split(
                flow("parent")
                        .addSubFlow(
                                flow("parallelFlow1")
                                        .addTestStep(annotatedMethod(this, "STEP")
                                                .withParameter("name", "parallelFlow1"))
                                        .withVusers(2)
                                        .withDataSources(dataSource(SHARED_DATA_SOURCE))
                        )
                        .addSubFlow(
                                flow("parallelFlow2")
                                        .addTestStep(annotatedMethod(this, "STEP")
                                                .withParameter("name", "parallelFlow2"))
                                        .withVusers(2)
                                        .withDataSources(dataSource(SHARED_DATA_SOURCE))
                        )
        )
        .build();
```

In this case there will be race condition between both flows, and Data Record distribution is unpredictable.

In Rx Scenario each flow will have its own instance of Shared Data Source:

<!-- MACRO{snippet|id=SHARED_DATA_SOURCE_IN_MULTIPLE_FLOWS_RUNNING_TWO_FLOWS_IN_PARALLEL|file=scenario-rx-taf-adaptor/src/test/java/com/ericsson/de/scenariorx/examples/RxSharedDataSourcesExample.java} -->

Output will always be: `[parallelFlow1: Data Record 2, parallelFlow1: Data Record 1, parallelFlow2: Data Record 1, parallelFlow2: Data Record 2]`