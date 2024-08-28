Feature: Performance Test

Scenario: Concurrent Data Source processing
  Given performance test
    And rampup during 30 seconds
    And number of users equals 3
    And repeating from file data/performance.csv
    And repeat for 1 minutes
  When I push an item from datasource into the context
  Then My Item is in a context
