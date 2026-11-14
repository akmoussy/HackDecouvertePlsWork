> Historical notes from the original results branch (5e1bdd2).
> Paths and commands below describe that checkout. Use the root README for this repository's current layout.

# HackDecouvert2026WWYJ
Premier Hackaton

## Prompt 5 results display

This branch implements the results portion of PackReady for the team's JavaFX
application. The validated form code remains on the `app` branch.

`PackReadyResults` lives in `demo/src/main/java/org/example/demo`, alongside the
team's `Planner.java`, and uses the same `org.example.demo` package. It accepts
the actual `Planner.Plan` record, including its `long` cents values. No planner
calculations or JavaFX dependencies are added to the results class.

### How the display works

1. `format(plan)` starts with the kit counts, then lists each purchase's supplied
   quantity, name, and subtotal. Total spent and budget left come from the plan.
2. Money is explicitly labeled CAD and always has two decimal places.
   `BigDecimal.valueOf(cents, 2)` preserves exact cents, including large `long`
   values, without converting through `double`.
3. An empty shopping list displays “This budget can’t complete another kit.”
   Kit counts and both totals remain visible.
4. `renderResults(setText, plan)` sends one complete formatted result to a text
   setter. Each call replaces the previous output. Item names stay plain text.

The $30 demo displays:

```text
8 kits ready now → 20 kits after shopping

Shopping list
- 5 Notebooks: CAD $5.00
- 12 Pencil cases: CAD $24.00

Total spent: CAD $29.00
Budget left: CAD $1.00
```

### Run the checks in VS Code

From the repository root, using the installed JDK:

```sh
mkdir -p /tmp/packready-results-tests
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d /tmp/packready-results-tests \
  demo/src/main/java/org/example/demo/Planner.java \
  demo/src/main/java/org/example/demo/PackReadyResults.java \
  demo/src/test/java/org/example/demo/PackReadyResultsTest.java
java -cp /tmp/packready-results-tests org.example.demo.PackReadyResultsTest
```

The tests cover a supplied plan, actual planner output for budgets of $0, $30,
$31.99, and $32, an unaffordable $1 budget, repeated replacement of the same
display, exact cents and large values, literal item names, and unchanged input.
They require no JavaFX toolkit or external test library. A failed check throws
`AssertionError` and returns a failure status. Compiled classes stay in `/tmp`.
A successful run ends with `All 18 results checks passed.`

### Connect the JavaFX controller

Use a plain-text JavaFX control such as a `TextArea` for the complete result.
With the real control and a `Planner.Plan` in the controller:

```java
resultsArea.setEditable(false);
resultsArea.setWrapText(true);
PackReadyResults.renderResults(resultsArea::setText, plan);
```

`resultsArea` is a placeholder for the team's actual field name. The method
reference passes the control's existing `setText` method to the renderer. Use
that replacement setter so the $0 result clears purchases from a previous $30
result. Call it on the JavaFX application thread, for example in the Calculate
event handler. On invalid form input, the controller can clear stale results
with `resultsArea.clear()`.

The current upstream project contains the planner but no JavaFX screen,
controller, or build wrapper. The tests verify the text and replacement calls;
the live control's layout, wrapping, and accessibility still need verification
when the team pushes its UI. The result area should have a visible label and an
accessible name, and remain readable and scrollable for longer shopping lists.

### Review handoff

The review covered the `app` validation code and this branch's results code:
74 parser checks, 66 form checks, and 18 results checks all passed with compiler
warnings treated as errors. Six additional checks passed raw form text through
validation, adaptation to `Planner.Item`, the actual planner, and the result
setter. They covered $0, $30, $31.99, $32, zero stock, and exact decimal prices.
These are Java checks; live JavaFX event handling and layout remain unverified.

### Planner boundary follow-up

One additional extreme-input check found a defect in the planner. Set all three
stock counts to `2147483647`, keep the demo unit prices of $12, $1, and $2, and
use a $15 budget. The form accepts these inputs, but `Planner.planPurchase`
returns `plannedKits = -2147483648` because `plannedKits + 1` overflows an `int`.

Request for the planner owner: guard the next target before adding one. The
minimal guard for the current `int` kit-count model is:

```java
while (plannedKits < Integer.MAX_VALUE
        && costToReach(items, plannedKits + 1) <= budgetCents) {
    plannedKits++;
}
```

This caps the count at the largest value the current model can represent. If
larger kit counts must be supported, the planner owner should widen the kit-count
types instead. The guard passed the boundary check, existing planner/results
tests, and six combined-flow checks in a temporary copy. The team's committed
`Planner.java` is unchanged; this follow-up still needs the planner owner's fix.
