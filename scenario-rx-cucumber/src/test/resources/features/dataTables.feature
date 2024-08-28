Feature: DataTables

Scenario: Push an item into a stack
  Given an empty stack
  And the following collections are created:
    | name         | createdBy         | sharingPermission |
    | Collection11 | Collections_User1 | Private           |
    | Collection9  | Collections_User1 | Private           |
    | Collection1  | Collections_User1 | Private           |
