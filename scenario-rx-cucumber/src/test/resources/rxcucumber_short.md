class: middle, center
# Cucumber⇄Rx Scenario prototype

.left.footer[Keith Moore]

---

# Executive Summary

* Gherkin compatible
--

* Fits in to existing Test Execution infrastructure (TMS, TE)
--

* External Data Sources, parameterized by Test Phase
--

* Framework level standardized approach
  * Passing data between steps
  * Loading external Data Source
--

* Reuse of Test Steps between Test Phases
--

* Integration with Graphite/Grafana
--

* Integration with Taurus
--

* Basic Performance features
--

* Allure reporting


This is proved to be working in PoC

---

# What is Rx Scenario?

* An advanced way to define and schedule test execution using Java API
* Complete rewrite of TAF Scenario
    - More robust concurrency model (RxJava)
    - Improved API based on user feedback
* Internal development - pending open source approval
* No dependencies on TAF
    - Independent implementation
    - Adapter modules for different frameworks:
        + TAF
        + Generic
        + ...

---

# What is Rx Scenario?

* An advanced way to define and schedule test execution using Java API
* Complete rewrite of TAF Scenario
    - More robust concurrency model (RxJava)
    - Improved API based on user feedback
* Internal development - pending open source approval
* No dependencies on TAF
    - Independent implementation
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

--

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

# Supported Gherkin Features

* Running Features
* DocStrings
* DataTables
* Outlines
* Tags


* (Cucumber) Hooks
* (Cucumber) Listeners

---

# Additional Features

* Use Gherkin step definitions to use extended Rx Scenario features.
* 2 Approaches:


```gherkin
  Scenario: Some determinable business situation
*   Given repeating from file data/node.csv
*   Given number of users equals 3
    Given Login
    Given Perform setup for enviroment
     When Add alarm
     Then Validate alarm
```

```gherkin
* @DataSource("data/node.csv")
* @VUsers(3)
  Scenario: Some determinable business situation
    Given Login
    Given Perform setup for enviroment
     When Add alarm
     Then Validate alarm
```

---

# External Data Sources

* Possibility to load Data Source:
  - From file
  - Test Data Management system
* Standardized
* Framework level (vs need to implement Java using Cucumber)
* Works on scenario (vs Test Step when using Cucumber)
* Explicit

---

# Parameterization

* Run same Test Cases written in Gherkin
* With different data depending on Test Phase:
  - Feature/Acceptance
  - Real Node
  - RFA250
  - Client
  - ...

--


* Parameters (file location/Test Data Management id) could be set:
  - Local execution
  - Test Executor
  - Fallback if not defined

---

# Data Flow

* *Standardized* way to pass data between steps
* Injection by name
* Explicit outputs & inputs for each step
  - Readability

---

# vUsers

* Run Scenario in *n* parallel threads
  - *Speed up* Data Source processing
  - Test if system behaves correctly when accessed concurrently

---

# Additional Features

* Basic performance features:
  - Rampup/duration
* Svg/Allure reports
* Graphite/Grafana Reporting

<img src="/home/edmnovi/safe/docs/cucumber/performance1.png"/>

---

# Taurus

* Taurus does not support granular reporting with Cucumber by default
* With Rx Scenario runner it's possible:
  * Show metrics by Test Step in Dashboard (realtime)
  * Set threshold for Test Step execution time
* When using HttpTool its possible to get request times

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

# Possible next steps

* Just by mapping Gherkin to existing Rx Scenario features:
  - Combination of Data Sources
  - Data Source filtering
  - Always Run - cleanup

* Integration with Arquillian
* `.toJavaApi()` - convert Gherkin Feature to Rx Scenario
    - For more advanced features like subflows

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

# Feedback

### Team America Collection Search Plugin

* Use of Allure reports for existing Cucumber testware 
  - Done
* One testsuite for both integration and acceptance tests

> "This will reduce the projects we have to maintain and the context switching between integration technologies and TAF technologies"

---

# Feedback

* Discussion with Mayke Nespoli
  - Taurus reporting granularity
  - Data Source parameterization - fallback

* Discussion with Biagio Laneve
  - Technically - fine with solution
    +  Strategy - internal vs external
  - Pilot with teams/get evaluation of benefits

---

# Next Steps

* Documentation
* Migrate more Testware

---

# Links

* [Rx Scenario documentation](http://taf.lmera.ericsson.se/taflanding/scenarios-rx/)
* [Team America Collection Search Plugin](https://gerrit.ericsson.se/#/c/2671121/)
* [Prototype source - ERIC_scenario_rx](https://gerrit.ericsson.se/#/admin/projects/OSS/com.ericsson.cifwk/ERIC_scenario_rx), ⌥branch `dCucumber`

---

class: middle, center
# Q&A

