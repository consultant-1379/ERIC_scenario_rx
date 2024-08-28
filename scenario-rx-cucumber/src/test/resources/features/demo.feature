Feature: Demo

  Scenario: Data Sources vUsers
    Given repeating from file data/node.csv
    Given number of users equals 3
    Given Login
    Given Perform setup for enviroment
     When Add alarm
     Then Validate alarm