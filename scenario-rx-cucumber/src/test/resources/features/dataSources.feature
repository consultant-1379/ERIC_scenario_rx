Feature: Data Sources

Scenario: Load from file
  Given repeating from file data/node.csv
  When assert node source
