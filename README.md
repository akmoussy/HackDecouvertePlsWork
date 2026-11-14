# HackDecouvertePlsWork

PackReady helps donation-drive volunteers decide what to buy to complete the
most school kits within a budget. This transfer brings the completed input
validation and results code into this repository's existing Java project.

## Source layout

All production classes use `package org.example.demo;` in
`demo/src/main/java/org/example/demo`:

| Class | Purpose |
| --- | --- |
| `PackReadyInputs` | Parses stock counts and exact integer cents; rejects invalid text. |
| `PackReadyForm` | Validates all seven fields, identifies the first invalid field, and supplies demo values. |
| `Planner` | The team's purchase planner and its `Item`, `Purchase`, and `Plan` records. |
| `PackReadyResults` | Formats the supplied plan in CAD and replaces the previous text result. |

Standalone checks live in `demo/src/test/java/org/example/demo`. The validation
classes have been moved out of the default package so the JavaFX controller can
call them directly.

## Run the Java checks

From the repository root, with JDK 17 or newer on the terminal's path:

```sh
sh scripts/test-packready.sh
```

This compiles with warnings treated as errors and runs the parser, form,
planner, results, and combined-flow checks. The checks use core Java and need
no JavaFX toolkit or external test library. The script reports the temporary
directory containing the compiled classes; it writes no classes into the repo.

The parser, form, results, and flow suites have 164 checks in total. The planner
suite also includes its existing comparison with 2,000 generated cases. The
combined-flow checks pass raw form text through validation, adaptation to
`Planner.Item`, the actual planner, and the results setter. They cover $0, $30,
$31.99, $32, zero stock, and exact decimal prices.

These checks have standalone `main` methods. Run the script to execute them;
the existing Gradle `test` task uses JUnit discovery instead.

## Controller handoff

After collecting the seven text fields into a `Map<String, String>`, the
controller can use the validated values like this:

```java
PackReadyForm.Request request = PackReadyForm.parse(rawText);
List<Planner.Item> items = new ArrayList<>();
for (PackReadyForm.Item item : request.getItems()) {
    items.add(new Planner.Item(item.getName(), item.getStock(), item.getUnitPriceCents()));
}
Planner.Plan plan = Planner.planPurchase(items, request.getBudgetCents());
PackReadyResults.renderResults(resultsArea::setText, plan);
```

`rawText` and `resultsArea` are placeholders for the team's actual fields.
The controller will need `java.util.List` and `java.util.ArrayList` imports.
The validated `int` cents widen exactly to the planner's `long` cents.

Catch `PackReadyForm.FieldValidationException` to display `getMessage()`, clear
stale results, and focus the field named by `getFieldName()`. Load demo should
populate all seven fields from `PackReadyForm.demoFields()` and run the same
Calculate handler. Enter should use that handler as well. Use a plain-text
JavaFX control such as a noneditable, wrapping `TextArea` for the result and call
its setter on the JavaFX application thread.

The existing JavaFX entry point is still the team's starter screen; the actual
PackReady fields and event handlers need to be connected. The existing Gradle
configuration requests JDK 23 and JavaFX 21.0.6. The core-check script above
does not build or launch that JavaFX application.

## Review notes and original history

- [Original app notes](docs/packready-app-notes.md) preserve the validation
  walkthrough and handoff review.
- [Original results notes](docs/packready-results-notes.md) preserve the results
  walkthrough and test evidence.
- The original branches and full commit history are preserved as
  `archive/packready-app` and `archive/packready-results`.

The source history is from `wazz64/HackDecouvert2026WWYJ`; this combined transfer
is on `packready-transfer`, based on this repository's own `main`.

There is one confirmed planner follow-up: at maximum `int` stock counts, its
next target can overflow into a negative kit count. The reproduction and tested
guard are in the [planner boundary notes](docs/packready-results-notes.md#planner-boundary-follow-up).
The planner was copied with that finding documented for its owner; the guard
has not been applied to its production code.
