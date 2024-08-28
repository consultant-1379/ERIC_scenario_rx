package com.ericsson.de.scenariorx.cucumber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Named;
import java.util.Random;
import java.util.Stack;

import com.ericsson.de.scenariorx.api.RxBasicDataRecord;
import com.ericsson.de.scenariorx.api.RxDataRecord;
import com.ericsson.de.scenariorx.mocks.Alarm;
import com.ericsson.de.scenariorx.mocks.HttpTool;
import com.ericsson.de.scenariorx.mocks.Node;
import cucumber.api.DataTable;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class StepDefinitions {

    public static final String USERNAME = "username";
    private Stack myStack;
    private Object pushed;
    private Object popped;

    @Before
    public void setUp() {
        myStack = new Stack();
    }

    @When("^I pop from the stack$")
    public void popFromTheStack() {
        Object out = myStack.pop();
        popped = out;
    }

    @When("^I push another item into the stack$")
    public void pushAnotherItemIntoTheStack() {
        iPushAnItemIntoTheStack();
    }

    @Given("^an empty stack$")
    public void anEmptyStack() {
        myStack = new Stack();
    }

    @Then("^I get the same item back$")
    public void getTheSameItemBack() {
        assertEquals(pushed, popped);
    }

    @Then("^the stack contains (.*) item$")
    public void theStackContainsOneItem(Integer arg1) {
        assertEquals(arg1, (Integer) myStack.size());
    }

    @When("^I push an item into the stack$")
    public Object iPushAnItemIntoTheStack() {
        pushed = new Object();
        myStack.push(pushed);

        return RxBasicDataRecord.builder()
                .setField("newItem", pushed)
                .build();
    }


    @Then("^My Item is in a stack$")
    public void itemIsInAStack(@Named("newItem") Object arg1) {
        assertTrue(arg1 == myStack.peek());
    }

    @Given("^there are (\\d+) cucumbers$")
    public void thereAreCucumbers(Integer arg1) throws Throwable {
        assertThat(arg1).isNotNull();

    }

    @When("^I eat (\\d+) cucumbers$")
    public void iEatCucumbers(Integer arg1) throws Throwable {
        assertThat(arg1).isNotNull();
    }

    @Then("^I should have (\\d+) cucumbers$")
    public void iShouldHaveCucumbers(Integer arg1) throws Throwable {
        assertThat(arg1).isNotNull();
    }

    @Given("^the following collections are created:$")
    public void theFollowingCollectionsAreCreated(DataTable arg1) throws Throwable {
        assertThat(arg1).isNotNull();
        assertThat(arg1.getGherkinRows().size()).isGreaterThan(0);
    }

    @Then("^Cleanup nodes$")
    public void cleanupNode(@Named("node") Node node) throws Throwable {
        assertThat(node).isNotNull();
    }

    @Given("^Login$")
    public RxDataRecord login() throws Throwable {
        HttpTool httpTool = new HttpTool(USERNAME, "password");

        return RxBasicDataRecord.builder()
                .setField("session", httpTool)
                .build();
    }

    @Given("^Perform setup for enviroment$")
    public void performSetupForEnviroment(@Named("ds") Node node, @Named("session") HttpTool session) throws Throwable {
        assertThat(node).isNotNull();
        assertThat(session).isNotNull();

        System.out.println(
                "Setting up node " + node.getNetworkElementId()
                        + " by user " + session.getUsername());
    }

    @When("^Add alarm$")
    public RxDataRecord addAlarm() throws Throwable {
        RxDataRecord newAlarm = RxBasicDataRecord.builder()
                .setField("name", "alarm" + new Random().nextInt(30))
                .setField("status", "ON FIRE")
                .build();

        System.out.println("Create alarm " + newAlarm.getFieldValue("name"));

        return newAlarm;
    }

    @Then("^Validate alarm$")
    public void validateAlarm(@Named("Add alarm") Alarm alarm) throws Throwable {
        assertThat(alarm).isNotNull();
        System.out.println("Validation step" + alarm.getName());
    }


    @Then("^I should receive an email with:$")
    public void iShouldReceiveAnEmailWith(String docString) throws Throwable {
        assertThat(docString).isNotNull();
    }

    @When("^the user types \"([^\"]*)\" and presses Enter$")
    public void userTypesAndPressesEnter(@Named("session") HttpTool session, String command) {
        assertThat(session).isNotNull();
        assertThat(session.getUsername()).isEqualTo(USERNAME);
        assertThat(command).isNotEmpty().isNotNull();
    }

    @When("^assert node source$")
    public void assertNodeSource(
            @Named("ds") Node bean,
            @Named("networkElementId") String networkElementId) throws Throwable {
        assertThat(bean).isNotNull();
        assertThat(networkElementId).isNotNull();
    }

    @When("^assert network element id$")
    public void assertNodeSource(@Named("networkElementId") String networkElementId) throws Throwable {
        assertThat(networkElementId).isNotNull();
    }
}
