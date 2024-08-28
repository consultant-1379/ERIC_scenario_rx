Feature: Stack Test

Scenario: Push an item into a stack
  Given an empty stack
  When I push an item into the stack
  Then the stack contains 1 item

Scenario: Pop a given item from the stack
  Given an empty stack
  When I push an item into the stack
  And I pop from the stack
  Then I get the same item back

Scenario: Push two items into a stack
  Given an empty stack
  When I push an item into the stack
  And I push another item into the stack
  Then the stack contains 2 item

Scenario: Check empty stack
  Then the stack contains 0 item
