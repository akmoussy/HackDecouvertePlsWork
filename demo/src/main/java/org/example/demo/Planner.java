package org.example.demo;

import java.util.ArrayList;
import java.util.List;

/**
 * PackReady purchase planner (Part 2).
 *
 * Pure logic only: no JavaFX, no demo data, no currency formatting.
 * All money is whole cents (long), so there are no floating-point rounding errors.
 * The app/controller supplies validated inputs; results view formats the output.
 */
public final class Planner {

    private Planner() {}

    /** One kit component. stock >= 0, unitPriceCents > 0. */
    public record Item(String name, int stock, long unitPriceCents) {}

    /** One line of the shopping list. Only items that need buying appear. */
    public record Purchase(String name, int quantity, long subtotalCents) {}

    /** What the planner returns to the app/results code. */
    public record Plan(
            int currentKits,
            int plannedKits,
            List<Purchase> purchases,
            long totalCents,
            long remainingCents) {}

    /** Cents needed to bring every item up to `target` units. */
    static long costToReach(List<Item> items, int target) {
        long totalCents = 0;
        for (Item item : items) {
            int missing = Math.max(0, target - item.stock());
            totalCents += missing * item.unitPriceCents();
        }
        return totalCents;
    }

    /**
     * Decide what to buy to complete the most kits within the budget.
     * Does not modify the items list.
     */
    public static Plan planPurchase(List<Item> items, long budgetCents) {
        if (items.isEmpty()) {
            return new Plan(0, 0, List.of(), 0, budgetCents);
        }

        // The scarcest item limits how many complete kits exist right now.
        int currentKits = Integer.MAX_VALUE;
        for (Item item : items) {
            currentKits = Math.min(currentKits, item.stock());
        }

        // Try one more kit at a time. Stop at the first unaffordable target;
        // the previous target is the answer.
        int plannedKits = currentKits;
        while (costToReach(items, plannedKits + 1) <= budgetCents) {
            plannedKits++;
        }

        // Shopping list for plannedKits.
        List<Purchase> purchases = new ArrayList<>();
        long totalCents = 0;
        for (Item item : items) {
            int quantity = Math.max(0, plannedKits - item.stock());
            if (quantity > 0) {
                long subtotal = quantity * item.unitPriceCents();
                purchases.add(new Purchase(item.name(), quantity, subtotal));
                totalCents += subtotal;
            }
        }

        return new Plan(currentKits, plannedKits, List.copyOf(purchases),
                totalCents, budgetCents - totalCents);
    }
}
