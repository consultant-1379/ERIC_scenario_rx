Feature: Data Sources

Scenario: Load from file
  Given repeating from file data/node.csv
  When assert node source

Scenario: Load nodes 1
  Given repeating from Data Source named nodes1
  When assert network element id

Scenario: Load nodes 3 (fallback to file)
  Given repeating from Data Source with name nodes3 or file data/node.csv
  When assert network element id