package com.ericsson.de.scenariorx.cucumber;

import static junit.framework.Assert.assertTrue;

import javax.inject.Named;
import java.util.Random;

import com.ericsson.de.scenariorx.api.RxBasicDataRecord;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class PerformanceStepDefinitions {
    @When("^I push an item from datasource into the context$")
    public Object toContext(@Named("item") String item) {
        sleep();

        return RxBasicDataRecord.builder()
                .setField("newItem", item)
                .build();
    }

    @Then("^My Item is in a context$")
    public void inContext(@Named("newItem") Object arg1, @Named("item") String item) {
        assertTrue(arg1 == item);
    }

    private void sleep() {
        try {
            System.out.println("Starting  sleep " + getTime());
            Thread.sleep(new Random().nextInt(3000));
            System.out.println("Wake from sleep " + getTime());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getTime() {
        long l = System.currentTimeMillis() / 1000;
        return "" + l + " " + Thread.currentThread().getName() + " ";
    }
}
