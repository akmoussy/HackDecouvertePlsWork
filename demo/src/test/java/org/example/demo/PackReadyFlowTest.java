package org.example.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises raw form text through the actual planner and results setter. */
public final class PackReadyFlowTest {
    private static final AtomicReference<String> display = new AtomicReference<>("OLD RESULT");
    private static int checks;

    public static void main(String[] args) {
        Map<String, String> fields = PackReadyForm.demoFields();
        Planner.Plan thirty = new Planner.Plan(8, 20, List.of(
                new Planner.Purchase("Notebooks", 5, 500),
                new Planner.Purchase("Pencil cases", 12, 2400)), 2900, 100);
        checkFlow("raw demo -> $30 plan -> display", fields, thirty,
                "8 kits ready now \u2192 20 kits after shopping",
                "- 5 Notebooks: CAD $5.00", "- 12 Pencil cases: CAD $24.00",
                "Total spent: CAD $29.00", "Budget left: CAD $1.00");

        fields.put("budget", "0");
        checkFlow("raw $0 budget -> empty result replaces purchases", fields,
                new Planner.Plan(8, 8, List.of(), 0, 0),
                "8 kits ready now \u2192 8 kits after shopping",
                "This budget can\u2019t complete another kit.",
                "Total spent: CAD $0.00", "Budget left: CAD $0.00");

        fields.put("budget", "32");
        checkFlow("raw $32 budget -> 21 kits -> display", fields,
                new Planner.Plan(8, 21, List.of(
                        new Planner.Purchase("Notebooks", 6, 600),
                        new Planner.Purchase("Pencil cases", 13, 2600)), 3200, 0),
                "8 kits ready now \u2192 21 kits after shopping",
                "- 6 Notebooks: CAD $6.00", "- 13 Pencil cases: CAD $26.00",
                "Total spent: CAD $32.00", "Budget left: CAD $0.00");

        fields.put("budget", "31.99");
        checkFlow("raw $31.99 budget preserves cents throughout", fields,
                new Planner.Plan(8, 20, thirty.purchases(), 2900, 299),
                "8 kits ready now \u2192 20 kits after shopping", "Budget left: CAD $2.99");

        fields.put("backpack-stock", "0");
        fields.put("budget", "30");
        checkFlow("zero stock survives validation, planning, and display", fields,
                new Planner.Plan(0, 2, List.of(new Planner.Purchase("Backpacks", 2, 2400)), 2400, 600),
                "0 kits ready now \u2192 2 kits after shopping",
                "- 2 Backpacks: CAD $24.00", "Budget left: CAD $6.00");

        fields.put("notebook-stock", "0");
        fields.put("pencil-case-stock", "0");
        fields.put("backpack-price", "0.29");
        fields.put("notebook-price", "0.01");
        fields.put("pencil-case-price", "0.01");
        fields.put("budget", "0.93");
        checkFlow("fractional dollar prices stay exact across the full flow", fields,
                new Planner.Plan(0, 3, List.of(
                        new Planner.Purchase("Backpacks", 3, 87),
                        new Planner.Purchase("Notebooks", 3, 3),
                        new Planner.Purchase("Pencil cases", 3, 3)), 93, 0),
                "0 kits ready now \u2192 3 kits after shopping",
                "- 3 Backpacks: CAD $0.87", "Total spent: CAD $0.93", "Budget left: CAD $0.00");
        System.out.println("All " + checks + " combined flow checks passed.");
    }

    static Planner.Plan calculate(Map<String, String> fields) {
        PackReadyForm.Request request = PackReadyForm.parse(fields);
        List<Planner.Item> items = new ArrayList<>();
        for (PackReadyForm.Item item : request.getItems()) {
            items.add(new Planner.Item(item.getName(), item.getStock(), item.getUnitPriceCents()));
        }
        return Planner.planPurchase(items, request.getBudgetCents());
    }

    private static void checkFlow(String label, Map<String, String> fields,
                                  Planner.Plan expected, String... expectedLines) {
        Planner.Plan actual = calculate(fields);
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
        PackReadyResults.renderResults(display::set, actual);
        for (String line : expectedLines) {
            if (!display.get().contains(line)) {
                throw new AssertionError(label + ": display is missing " + line);
            }
        }
        if (actual.purchases().isEmpty() && display.get().contains("\n- ")) {
            throw new AssertionError(label + ": stale shopping lines remain");
        }
        checks++;
        System.out.println("PASS " + label);
    }
}
