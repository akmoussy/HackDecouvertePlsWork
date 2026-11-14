package org.example.demo;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Results display for Prompt 5. Formats the planner's values without calculating
 * purchases and without depending on JavaFX.
 */
public final class PackReadyResults {
    private PackReadyResults() {
    }

    /**
     * Replaces a text control's content with one complete result.
     *
     * @param setText a replacement setter, for example resultsArea::setText
     * @param plan the result returned by Planner.planPurchase
     * @throws NullPointerException if the setter or plan is null
     */
    public static void renderResults(Consumer<String> setText, Planner.Plan plan) {
        Objects.requireNonNull(setText, "setText");
        // Format before changing the display; each successful render calls the setter once.
        String text = format(plan);
        setText.accept(text);
    }

    /**
     * Builds the complete plain-text display: kit counts, shopping list, then totals.
     * The plan and its purchase list are left untouched.
     *
     * @param plan the result returned by Planner.planPurchase
     * @return display text with explicit CAD amounts and two decimal places
     * @throws NullPointerException if the plan is null
     */
    public static String format(Planner.Plan plan) {
        Objects.requireNonNull(plan, "plan");

        StringBuilder text = new StringBuilder();
        text.append(kits(plan.currentKits())).append(" ready now \u2192 ")
                .append(kits(plan.plannedKits())).append(" after shopping\n\n");
        text.append("Shopping list\n");

        if (plan.purchases().isEmpty()) {
            text.append("This budget can\u2019t complete another kit.\n");
        } else {
            for (Planner.Purchase purchase : plan.purchases()) {
                text.append("- ").append(purchase.quantity()).append(" ")
                        .append(purchase.name()).append(": ")
                        .append(formatCad(purchase.subtotalCents())).append('\n');
            }
        }

        text.append("\nTotal spent: ").append(formatCad(plan.totalCents()));
        text.append("\nBudget left: ").append(formatCad(plan.remainingCents()));
        return text.toString();
    }

    private static String kits(int count) {
        return count + (count == 1 ? " kit" : " kits");
    }

    /** Formats integer cents as an exact CAD amount for text and JavaFX results. */
    public static String formatCad(long cents) {
        // Exact decimal formatting, including values too large for an exact double.
        return "CAD $" + BigDecimal.valueOf(cents, 2).toPlainString();
    }
}
