package org.example.demo;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Standalone checks for Planner.planPurchase(). No JavaFX, no test framework.
 *
 * Run from the folder containing Planner.java and PlannerTest.java:
 *   javac Planner.java PlannerTest.java
 *   java PlannerTest
 */
public class PlannerTest {

    private static int failures = 0;

    private static void check(String label, Object actual, Object expected) {
        // Records compare every field in equals(), so one call checks the whole plan.
        if (actual.equals(expected)) {
            System.out.println("PASS  " + label);
        } else {
            failures++;
            System.out.println("FAIL  " + label);
            System.out.println("  expected: " + expected);
            System.out.println("  actual:   " + actual);
        }
    }

    // Shared acceptance data: 25 backpacks @ $12, 15 notebooks @ $1, 8 pencil cases @ $2.
    private static List<Planner.Item> demoItems() {
        return List.of(
                new Planner.Item("Backpacks", 25, 1200),
                new Planner.Item("Notebooks", 15, 100),
                new Planner.Item("Pencil cases", 8, 200));
    }

    private static Planner.Purchase buy(String name, int quantity, long subtotalCents) {
        return new Planner.Purchase(name, quantity, subtotalCents);
    }

    public static void main(String[] args) {
        // $0: nothing to buy, list empty, counts still shown.
        check("$0 budget",
                Planner.planPurchase(demoItems(), 0),
                new Planner.Plan(8, 8, List.of(), 0, 0));

        // $30: 20 kits, $29 spent, $1 left.
        check("$30 budget",
                Planner.planPurchase(demoItems(), 3000),
                new Planner.Plan(8, 20, List.of(
                        buy("Notebooks", 5, 500),
                        buy("Pencil cases", 12, 2400)), 2900, 100));

        // $32: the judging moment, the extra $2 completes kit 21.
        check("$32 budget",
                Planner.planPurchase(demoItems(), 3200),
                new Planner.Plan(8, 21, List.of(
                        buy("Notebooks", 6, 600),
                        buy("Pencil cases", 13, 2600)), 3200, 0));

        // $31.99: one cent short of kit 21, so still 20.
        check("$31.99 budget",
                Planner.planPurchase(demoItems(), 3199),
                new Planner.Plan(8, 20, List.of(
                        buy("Notebooks", 5, 500),
                        buy("Pencil cases", 12, 2400)), 2900, 299));

        // Zero stock: 0 backpacks means 0 kits now; $30 buys 2 backpacks ($24), 3 would be $36.
        List<Planner.Item> zeroBackpacks = List.of(
                new Planner.Item("Backpacks", 0, 1200),
                new Planner.Item("Notebooks", 15, 100),
                new Planner.Item("Pencil cases", 8, 200));
        check("zero backpack stock, $30 budget",
                Planner.planPurchase(zeroBackpacks, 3000),
                new Planner.Plan(0, 2, List.of(buy("Backpacks", 2, 2400)), 2400, 600));

        // Input untouched: pass a modifiable list and confirm it's identical afterwards.
        List<Planner.Item> original = new ArrayList<>(demoItems());
        List<Planner.Item> snapshot = List.copyOf(original);
        Planner.planPurchase(original, 3200);
        check("items list is left untouched", original, snapshot);

        // Cross-check: compare against a slow brute-force search on random inputs.
        check("matches brute force on 2,000 random cases", bruteForceMismatches(2000), 0);

        if (failures == 0) {
            System.out.println("\nAll planner checks passed.");
        } else {
            System.out.println("\n" + failures + " planner check(s) failed.");
            System.exit(1);
        }
    }

    /**
     * Brute force: try EVERY target up to a generous ceiling and keep the largest
     * affordable one. It never stops early, so if it agrees with the planner,
     * the planner's "stop at the first unaffordable target" shortcut is safe.
     */
    private static int bruteForceMismatches(int cases) {
        Random random = new Random(42); // fixed seed: same cases every run
        int mismatches = 0;
        for (int i = 0; i < cases; i++) {
            List<Planner.Item> items = List.of(
                    new Planner.Item("Backpacks", random.nextInt(30), 1 + random.nextInt(2000)),
                    new Planner.Item("Notebooks", random.nextInt(30), 1 + random.nextInt(500)),
                    new Planner.Item("Pencil cases", random.nextInt(30), 1 + random.nextInt(800)));
            long budget = random.nextInt(20000);

            int currentKits = items.stream().mapToInt(Planner.Item::stock).min().orElse(0);
            int best = currentKits;
            for (int target = currentKits; target <= currentKits + 25000; target++) {
                if (Planner.costToReach(items, target) <= budget) {
                    best = target;
                }
            }

            Planner.Plan plan = Planner.planPurchase(items, budget);
            boolean ok = plan.plannedKits() == best
                    && plan.totalCents() == Planner.costToReach(items, best)
                    && plan.remainingCents() == budget - plan.totalCents()
                    && plan.remainingCents() >= 0;
            if (!ok) {
                mismatches++;
                if (mismatches <= 3) {
                    System.out.println("  mismatch: budget=" + budget + " items=" + items
                            + " planner=" + plan.plannedKits() + " brute=" + best);
                }
            }
        }
        return mismatches;
    }
}
