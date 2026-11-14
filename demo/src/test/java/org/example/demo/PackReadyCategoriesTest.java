package org.example.demo;

import java.util.ArrayList;
import java.util.List;

/** Checks editable-category validation and its actual planner contract. */
public final class PackReadyCategoriesTest {
    private static int checks;

    public static void main(String[] args) {
        check("category names are trimmed", "Water bottles".equals(
                PackReadyForm.parseCategoryName("  Water bottles  ", List.of("Notebooks"))));
        check("60-character names are accepted", PackReadyForm.parseCategoryName("a".repeat(60), List.of()).length() == 60);
        invalidName("blank name", " ", List.of(), "required");
        invalidName("missing name", null, List.of(), "required");
        invalidName("long name", "a".repeat(61), List.of(), "60 characters");
        invalidName("multiline name", "Water\nbottles", List.of(), "single line");
        invalidName("duplicate name ignores case and surrounding spaces", " notebooks ", List.of("Notebooks"), "already exists");

        List<PackReadyForm.CategoryInput> defaults = List.of(
                category("backpack", "Backpacks", "25", "12"),
                category("notebook", "Notebooks", "15", "1"),
                category("pencil-case", "Pencil cases", "8", "2"));
        Planner.Plan normal = new Planner.Plan(8, 20, List.of(
                new Planner.Purchase("Notebooks", 5, 500),
                new Planner.Purchase("Pencil cases", 12, 2400)), 2900, 100);
        check("default categories keep the shared $30 result", normal.equals(plan(defaults, "30")));

        List<PackReadyForm.CategoryInput> four = new ArrayList<>(defaults);
        four.add(category("water", "Water bottles", "0", "0.50"));
        List<PackReadyForm.CategoryInput> original = List.copyOf(four);
        PackReadyForm.Request request = PackReadyForm.parseCategories(four, "30");
        check("added category is included", request.getItems().size() == 4
                && request.getItems().get(3).getName().equals("Water bottles")
                && request.getItems().get(3).getUnitPriceCents() == 50);
        check("adding a category changes the actual kit calculation", plan(four, "30").equals(
                new Planner.Plan(0, 17, List.of(
                        new Planner.Purchase("Notebooks", 2, 200),
                        new Planner.Purchase("Pencil cases", 9, 1800),
                        new Planner.Purchase("Water bottles", 17, 850)), 2850, 150)));
        check("category input is not mutated", original.equals(four));
        check("zero budget is valid with custom categories", plan(four, "0").equals(new Planner.Plan(0, 0, List.of(), 0, 0)));
        check("deleting a default category changes the recipe", plan(defaults.subList(0, 2), "30").equals(
                new Planner.Plan(15, 26, List.of(
                        new Planner.Purchase("Backpacks", 1, 1200),
                        new Planner.Purchase("Notebooks", 11, 1100)), 2300, 700)));
        check("a single category is supported", plan(List.of(defaults.get(1)), "30").equals(
                new Planner.Plan(15, 45, List.of(new Planner.Purchase("Notebooks", 30, 3000)), 3000, 0)));

        invalidForm("no categories", List.of(), "30", PackReadyForm.CATEGORIES, "at least one");
        invalidForm("null category list", null, "30", PackReadyForm.CATEGORIES, "at least one");
        invalidForm("duplicate ids", List.of(defaults.get(0), defaults.get(0)), "30", PackReadyForm.CATEGORIES, "unique identifier");
        invalidForm("missing id", List.of(category(null, "Soap", "0", "1")), "30", PackReadyForm.CATEGORIES, "unique identifier");
        invalidForm("blank category name", List.of(category("soap", " ", "0", "1")), "30", "soap-name", "required");
        invalidForm("duplicate names", List.of(defaults.get(0), category("other", "backpacks", "0", "1")), "30", "other-name", "already exists");
        invalidForm("custom stock is required", List.of(category("soap", "Soap", null, "1")), "30", "soap-stock", "required");
        invalidForm("custom stock is whole", List.of(category("soap", "Soap", "2.5", "1")), "30", "soap-stock", "whole number");
        invalidForm("custom price is required", List.of(category("soap", "Soap", "0", "")), "30", "soap-price", "required");
        invalidForm("custom unit price cannot be zero", List.of(category("soap", "Soap", "0", "0")), "0", "soap-price", "greater than zero");
        invalidForm("custom price precision", List.of(category("soap", "Soap", "0", "12.001")), "30", "soap-price", "two decimal places");
        invalidForm("first invalid category precedes the budget", List.of(category("soap", "Soap", "-1", "1")), "", "soap-stock", "whole number");
        invalidForm("budget remains required", defaults, "", PackReadyForm.BUDGET, "required");
        System.out.println("All " + checks + " category checks passed.");
    }

    private static PackReadyForm.CategoryInput category(String id, String name, String stock, String price) {
        return new PackReadyForm.CategoryInput(id, name, stock, price);
    }

    private static Planner.Plan plan(List<PackReadyForm.CategoryInput> raw, String budget) {
        PackReadyForm.Request request = PackReadyForm.parseCategories(raw, budget);
        List<Planner.Item> items = request.getItems().stream().map(
                item -> new Planner.Item(item.getName(), item.getStock(), item.getUnitPriceCents())).toList();
        return Planner.planPurchase(items, request.getBudgetCents());
    }

    private static void invalidName(String label, String input, List<String> existing, String message) {
        try {
            PackReadyForm.parseCategoryName(input, existing);
        } catch (IllegalArgumentException expected) {
            check(label, expected.getMessage().contains(message));
            return;
        }
        throw new AssertionError(label + " should be rejected");
    }

    private static void invalidForm(String label, List<PackReadyForm.CategoryInput> raw, String budget,
                                    String field, String message) {
        try {
            PackReadyForm.parseCategories(raw, budget);
        } catch (PackReadyForm.FieldValidationException expected) {
            check(label, expected.getFieldName().equals(field) && expected.getMessage().contains(message));
            return;
        }
        throw new AssertionError(label + " should be rejected");
    }

    private static void check(String label, boolean success) {
        if (!success) { throw new AssertionError(label); }
        checks++;
        System.out.println("PASS " + label);
    }
}
