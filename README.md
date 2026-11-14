# HackDecouvert2026WWYJ
Premier Hackaton

## Local Java input validation

PackReady now targets JavaFX. `PackReadyInputs.java` contains plain Java validation
that a JavaFX controller can call without depending on JavaFX itself. The class
and its standalone test runner temporarily live at the repository root with no
package declaration, pending the team's source folder and package decision.

### Read the implementation

1. `parseStock` trims surrounding spaces, rejects missing input, checks for digits
   only, then converts to an `int`. This accepts `0` and `25`, but rejects blank
   input, `-1`, and `2.5`.
2. `parseMoneyCents` checks the text format before conversion. It accepts `12`,
   `12.5`, and `12.50`. `BigDecimal` shifts the decimal point two places and
   converts exactly to cents: `12.50` becomes `1250`. Extra decimal places,
   including `12.001` and `12.000`, are rejected without rounding. Use `true` for
   the budget (zero allowed) and `false` for a unit price (must be positive).

Both methods throw `IllegalArgumentException` with a readable message when the
input is invalid. Surrounding spaces are allowed; currency symbols, commas,
signs, and scientific notation are rejected. Money requires digits before the
decimal point and one or two digits after it when present, so `.50` and `12.` are
rejected. Stock is limited to `2147483647`, and dollar amounts to `21474836.47`,
because the methods return integer counts and integer cents.

### Run the tests in the VS Code terminal

From the repository root, with a JDK installed:

```sh
mkdir -p /tmp/packready-input-tests
javac -d /tmp/packready-input-tests PackReadyInputs.java PackReadyInputsTest.java
java -cp /tmp/packready-input-tests PackReadyInputsTest
```

The runner prints each accepted value or expected rejection and ends with
`All 48 checks passed.` A failed check throws `AssertionError` and exits with a
failure status. Compiled classes stay outside the repository.

### Call from the JavaFX controller

```java
try {
    int stock = PackReadyInputs.parseStock(stockField.getText());
    int unitPriceCents = PackReadyInputs.parseMoneyCents(priceField.getText(), false);
    int budgetCents = PackReadyInputs.parseMoneyCents(budgetField.getText(), true);
    // Pass validated values to the team's planner.
} catch (IllegalArgumentException exception) {
    // Display exception.getMessage(), clear stale results, and focus the invalid field.
}
```

The field names above are placeholders. When the team confirms its layout, move
the validator into that source folder and add the agreed `package` declaration.
The test runner must use the same package (or import the validator). A controller
in a named package cannot import a class from the default package. The controller
will handle field-specific messages and focus, demo loading, planner calls, and
result display; this first step covers the two parsing methods only.
