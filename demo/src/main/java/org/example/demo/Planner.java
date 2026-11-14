package org.example.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * PackReady purchase planner (Part 2).
 *
 * Pure logic only: no JavaFX, no demo data, no currency formatting.
 * All money is whole cents (long), so there are no floating-point rounding errors.
 * The app/controller supplies validated inputs; results view formats the output.
 */
public final class Planner {

    private Planner() {}

    /** One component: stock >= 0, price > 0, requiredQuantity >= 1 per kit. */
    public record Item(String name, int stock, long unitPriceCents, int requiredQuantity) {
        public Item(String name, int stock, long unitPriceCents) {
            this(name, stock, unitPriceCents, 1);
        }
    }

    /** One line of the shopping list. Only items that need buying appear. */
    public record Purchase(String name, long quantity, long subtotalCents) {}

    /** What the planner returns to the app/results code. */
    public record Plan(
            int currentKits,
            int plannedKits,
            List<Purchase> purchases,
            long totalCents,
            long remainingCents) {}

    /** Exact cents needed for target kits; throws if an unrepresentable cost is requested. */
    static long costToReach(List<Item> items, int target) {
        long totalCents = 0;
        for (Item item : items) {
            long missing = missingUnits(item, target);
            totalCents = Math.addExact(totalCents, Math.multiplyExact(missing, item.unitPriceCents()));
        }
        return totalCents;
    }

    /**
     * Decide what to buy to complete the most kits within the budget.
     * Does not modify the items list.
     */
    public static Plan planPurchase(List<Item> items, long budgetCents) {
        Objects.requireNonNull(items, "items");
        if (budgetCents < 0) {
            throw new IllegalArgumentException("Budget cannot be negative.");
        }
        if (items.isEmpty()) {
            return new Plan(0, 0, List.of(), 0, budgetCents);
        }

        // The scarcest item limits how many complete kits exist right now.
        int currentKits = Integer.MAX_VALUE;
        for (Item item : items) {
            if (item == null || item.stock() < 0 || item.unitPriceCents() <= 0 || item.requiredQuantity() <= 0) {
                throw new IllegalArgumentException("Stock must be nonnegative; unit price and required quantity must be positive.");
            }
            currentKits = Math.min(currentKits, item.stock() / item.requiredQuantity());
        }

        // Costs only increase with the target. Binary search also keeps large
        // quantities fast and never steps beyond the int kit-count limit.
        int plannedKits = currentKits;
        int upper = Integer.MAX_VALUE;
        while (plannedKits < upper) {
            int target = (int) (plannedKits + ((long) upper - plannedKits + 1) / 2);
            if (canAfford(items, target, budgetCents)) {
                plannedKits = target;
            } else {
                upper = target - 1;
            }
        }

        // Shopping list for plannedKits.
        List<Purchase> purchases = new ArrayList<>();
        long totalCents = 0;
        for (Item item : items) {
            long quantity = missingUnits(item, plannedKits);
            if (quantity > 0) {
                long subtotal = Math.multiplyExact(quantity, item.unitPriceCents());
                purchases.add(new Purchase(item.name(), quantity, subtotal));
                totalCents = Math.addExact(totalCents, subtotal);
            }
        }

        return new Plan(currentKits, plannedKits, List.copyOf(purchases),
                totalCents, budgetCents - totalCents);
    }

    private static long missingUnits(Item item, int kits) {
        return Math.max(0L, (long) kits * item.requiredQuantity() - item.stock());
    }

    private static boolean canAfford(List<Item> items, int kits, long budgetCents) {
        long remaining = budgetCents;
        for (Item item : items) {
            long quantity = missingUnits(item, kits);
            // Divide first: an unaffordable multiplication must not overflow.
            if (quantity > remaining / item.unitPriceCents()) {
                return false;
            }
            remaining -= quantity * item.unitPriceCents();
        }
        return true;
    }
}
