# HackDecouvert2026WWYJ
Premier Hackaton

## Local Java input validation

PackReady now targets JavaFX. `PackReadyInputs.java` validates individual fields;
`PackReadyForm.java` reads all seven fields and returns validated values for the
controller. Both are plain Java with no JavaFX dependency. The classes and their
standalone test runners temporarily live at the repository root with no package
declaration, pending the team's source folder and package decision.

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
input is invalid. Surrounding spaces and leading zeros are allowed; currency
symbols, commas, signs, and scientific notation are rejected. Money requires
digits before the decimal point and one or two digits after it when present, so
`.50` and `12.` are rejected. Stock is limited to `2147483647`, and dollar amounts
to `21474836.47`, because the methods return integer counts and integer cents.

### Run the tests in the VS Code terminal

From the repository root, with a JDK installed:

```sh
mkdir -p /tmp/packready-input-tests
javac -d /tmp/packready-input-tests PackReadyInputs.java PackReadyInputsTest.java
java -cp /tmp/packready-input-tests PackReadyInputsTest
```

The runner prints each accepted value or expected rejection and ends with
`All 74 checks passed.` A failed check throws `AssertionError` and exits with a
failure status. Compiled classes stay outside the repository.

### Validate the complete form

`PackReadyForm.parse` takes a `Map<String, String>` containing the raw text of the
seven fields. Its keys are `backpack-stock`, `backpack-price`, `notebook-stock`,
`notebook-price`, `pencil-case-stock`, `pencil-case-price`, and `budget`. Public
constants such as `PackReadyForm.BACKPACK_STOCK` provide these keys without
repeating string literals in controller code.

The method validates in display order: backpack stock and price, notebook stock
and price, pencil case stock and price, then budget. It returns a `Request` only
when all seven fields are valid. That request contains an immutable list of
items with `getName()`, `getStock()`, and `getUnitPriceCents()`, plus
`getBudgetCents()`. Display names are Backpacks, Notebooks, and Pencil cases.

On failure it throws `PackReadyForm.FieldValidationException`:

- `getMessage()` gives a specific error, such as
  `Notebooks: Stock is required.`
- `getFieldName()` identifies the first invalid field, such as `notebook-stock`,
  so the controller can focus it.

Missing fields are rejected, and parsing never changes the input map.
`PackReadyForm.demoFields()` supplies a fresh map with stock `25`, `15`, and `8`,
prices `12`, `1`, and `2` dollars, and a `30` dollar budget.

```java
Map<String, String> rawText = PackReadyForm.demoFields();
PackReadyForm.Request request = PackReadyForm.parse(rawText);
// request.getBudgetCents() == 3000
// request.getItems().get(0).getUnitPriceCents() == 1200
```

Run the form tests from the repository root:

```sh
mkdir -p /tmp/packready-input-tests
javac -d /tmp/packready-input-tests PackReadyInputs.java PackReadyForm.java PackReadyFormTest.java
java -cp /tmp/packready-input-tests PackReadyFormTest
```

These checks cover the demo values, budgets of `0`, `30`, and `32`, all blank or
missing fields, zero unit prices, excess decimal places, the first invalid field,
and preservation of input and validated values. They also independently check
the seven agreed field names, validation order even when map order is reversed,
overflow errors for every field, and parsing of a read-only map. The expected
final line is `All 66 form checks passed.`

### Connect the JavaFX controller when it is available

The controller will collect each `TextField.getText()` into the map, then call:

```java
try {
    PackReadyForm.Request request = PackReadyForm.parse(rawText);
    // Clear the error, adapt the values to the team's planner, and display its result.
} catch (PackReadyForm.FieldValidationException exception) {
    // Show exception.getMessage() and clear stale results.
    // Focus the TextField mapped to exception.getFieldName().
}
```

For Load demo, copy `demoFields()` into the seven text fields and invoke the same
Calculate handler. The controller should connect Enter to that handler as well.
Demo values follow the same validation path as manually typed values.

The JavaFX controller/FXML, Java package, and planner API have not been pushed to
the available branches yet. `Request` and `Item` are app-side data containers;
they do not prescribe the team's planner signature. No purchase algorithm or
result renderer is implemented here. The `0`, `30`, and `32` form tests validate
inputs only; the shared kit counts, shopping lists, and totals need the planner
before they can be checked end to end. Field focus, stale-result clearing, Enter,
and actual button actions still need verification in the JavaFX application.

When the layout is confirmed, move both production classes into the agreed
source folder and add its `package` declaration. The test runners must use the
same package (or import those classes). A controller in a named package cannot
import a class from the default package.
