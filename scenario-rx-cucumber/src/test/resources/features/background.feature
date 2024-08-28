Feature: Cucumber Background Test

Background:
  Given an empty stack
  And I push an item into the stack

Scenario: First check
  Then the stack contains 1 item

Scenario: Second check
  Then the stack contains 1 item