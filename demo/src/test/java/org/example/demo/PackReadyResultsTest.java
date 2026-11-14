package org.example.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Standalone results checks; no JavaFX toolkit or test library is required. */
public final class PackReadyResultsTest {
    private static int checks;

    private static final String THIRTY_DOLLARS =
            "8 kits ready now \u2192 20 kits after shopping\n\n"
            + "Shopping list\n"
            + "- 5 Notebooks: CAD $5.00\n"
            + "- 12 Pencil cases: CAD $24.00\n"
            + "\nTotal spent: CAD $29.00\n"
            + "Budget left: CAD $1.00";

    private static final String ZERO_DOLLARS =
            "8 kits ready now \u2192 8 kits after shopping\n\n"
            + "Shopping list\n"
            + "This budget can\u2019t complete another kit.\n"
            + "\nTotal spent: CAD $0.00\n"
            + "Budget left: CAD $0.00";

    private static final String THIRTY_TWO_DOLLARS =
            "8 kits ready now \u2192 21 kits after shopping\n\n"
            + "Shopping list\n"
            + "- 6 Notebooks: CAD $6.00\n"
            + "- 13 Pencil cases: CAD $26.00\n"
            + "\nTotal spent: CAD $32.00\n"
            + "Budget left: CAD $0.00";

    public static void main(String[] args) {
        // First test a supplied plan independently of the purchase algorithm.
        Planner.Plan sample = new Planner.Plan(8, 20, List.of(
                new Planner.Purchase("Notebooks", 5, 500),
                new Planner.Purchase("Pencil cases", 12, 2400)), 2900, 100);
        expectEquals("sample $30 plan", THIRTY_DOLLARS, PackReadyResults.format(sample));

        // Then check the team's actual planner-to-results contract.
        List<Planner.Item> items = List.of(
                new Planner.Item("Backpacks", 25, 1200),
                new Planner.Item("Notebooks", 15, 100),
                new Planner.Item("Pencil cases", 8, 200));
        Planner.Plan thirty = Planner.planPurchase(items, 3000);
        Planner.Plan zero = Planner.planPurchase(items, 0);
        Planner.Plan thirtyTwo = Planner.planPurchase(items, 3200);
        expectEquals("planner $30 output", THIRTY_DOLLARS, PackReadyResults.format(thirty));
        expectEquals("planner $0 output", ZERO_DOLLARS, PackReadyResults.format(zero));
        expectEquals("planner $32 output", THIRTY_TWO_DOLLARS, PackReadyResults.format(thirtyTwo));
        expectEquals("planner $31.99 leaves exact cents",
                THIRTY_DOLLARS.replace("Budget left: CAD $1.00", "Budget left: CAD $2.99"),
                PackReadyResults.format(Planner.planPurchase(items, 3199)));
        expectEquals("an unused budget stays visible with an empty list",
                ZERO_DOLLARS.replace("Budget left: CAD $0.00", "Budget left: CAD $1.00"),
                PackReadyResults.format(Planner.planPurchase(items, 100)));

        // The same setter stands in for a JavaFX TextArea.setText method.
        AtomicReference<String> display = new AtomicReference<>("Previous result");
        PackReadyResults.renderResults(display::set, thirty);
        expectEquals("render replaces the previous content", THIRTY_DOLLARS, display.get());
        PackReadyResults.renderResults(display::set, zero);
        expectEquals("$0 replaces $30 without stale purchases", ZERO_DOLLARS, display.get());
        PackReadyResults.renderResults(display::set, thirtyTwo);
        expectEquals("$32 replaces the empty result", THIRTY_TWO_DOLLARS, display.get());

        // Supplied values are displayed as-is; the renderer does not recompute totals.
        Planner.Plan supplied = new Planner.Plan(1, 2,
                List.of(new Planner.Purchase("<b>Notebooks</b> & pens", 3, 123)), 456, 789);
        expectEquals("provided counts, names, subtotals, and totals are preserved",
                "1 kit ready now \u2192 2 kits after shopping\n\n"
                + "Shopping list\n"
                + "- 3 <b>Notebooks</b> & pens: CAD $1.23\n"
                + "\nTotal spent: CAD $4.56\n"
                + "Budget left: CAD $7.89",
                PackReadyResults.format(supplied));
        expectEquals("zero and one kits use the right wording",
                "0 kits ready now \u2192 1 kit after shopping",
                PackReadyResults.format(new Planner.Plan(0, 1,
                        List.of(new Planner.Purchase("Backpacks", 1, 1)), 1, 0))
                        .split("\n", 2)[0]);
        expectContains("single cents keep two decimal places", "Total spent: CAD $0.01",
                PackReadyResults.format(new Planner.Plan(0, 1,
                        List.of(new Planner.Purchase("Backpacks", 1, 1)), 1, 0)));
        expectContains("large long cents do not lose precision", "CAD $90071992547409.93",
                PackReadyResults.format(new Planner.Plan(0, 0, List.of(), 0, 9007199254740993L)));
        expectContains("maximum long cents format without overflow", "CAD $92233720368547758.07",
                PackReadyResults.format(new Planner.Plan(0, 0, List.of(), 0, Long.MAX_VALUE)));

        List<Planner.Purchase> mutablePurchases = new ArrayList<>(sample.purchases());
        List<Planner.Purchase> original = List.copyOf(mutablePurchases);
        PackReadyResults.format(new Planner.Plan(8, 20, mutablePurchases, 2900, 100));
        expectEquals("formatting leaves the purchase list untouched", original, mutablePurchases);

        expectNullRejected("null plan is rejected", () -> PackReadyResults.renderResults(display::set, null));
        expectEquals("a failed render leaves the displayed result intact", THIRTY_TWO_DOLLARS, display.get());
        expectNullRejected("null setter is rejected", () -> PackReadyResults.renderResults(null, sample));

        System.out.println("All " + checks + " results checks passed.");
    }

    private static void expectEquals(String description, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(description + "\nExpected: " + expected + "\nActual: " + actual);
        }
        checks++;
        System.out.println("PASS " + description);
    }

    private static void expectContains(String description, String expectedPart, String actual) {
        if (!actual.contains(expectedPart)) {
            throw new AssertionError(description + ": missing " + expectedPart + " in " + actual);
        }
        checks++;
        System.out.println("PASS " + description);
    }

    private static void expectNullRejected(String description, Runnable action) {
        try {
            action.run();
        } catch (NullPointerException expected) {
            checks++;
            System.out.println("PASS " + description);
            return;
        }
        throw new AssertionError(description + ": expected NullPointerException");
    }
}
