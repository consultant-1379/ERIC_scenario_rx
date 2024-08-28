Feature: DocStrings

Scenario: Doc String Test
  When an empty stack
  Then I should receive an email with:
    """
    Dear,

    Please click this link to reset your password
    """
