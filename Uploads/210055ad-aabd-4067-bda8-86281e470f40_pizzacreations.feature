Feature: Creation of custom Pizza with selected toppings
    @CORP-43
    Scenario Outline: I want to create my own pizza with choice of toppings
        Given I want to create a unique pizza with detail "<pizzaName>" with available toppings "<toppings>"
        Then I expect to get a status code response of "<expectedResponse>"

        Examples:
            | pizzaName          | toppings | expectedResponse |
            | Anchoviesonax:1.99 | Bacon    | Pizza has been saved!                |
            | Veggieonax: 0.59   |          | Pizza has been saved!                |
            | Apineappleonax:099 | Peppers  | Pizza has been saved!                |

    @CORP-48
    Scenario Outline: I want to select from a list of available toppings and add or remove them as desired
        Given I want to select toppings "<toppingsForAdding>"
        When I deselect toppings "<toppingsForRemoval>"
        Then I expect a remaining toppings selection count of "<countOfToppingsExpected>"

        Examples:
            | toppingsForAdding                 | toppingsForRemoval | countOfToppingsExpected |
            | spinach,Peppers,pineapple,Bacon   | Bacon,spinach      | 2                       |
            | tomatoes,cucmbers,Sausage         |                    | 3                       |
            | Sausage,Peppers                   | Peppers,Sausage    | 0                       |