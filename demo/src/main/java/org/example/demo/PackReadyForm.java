package org.example.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Plain Java form reading, independent of JavaFX and the team's planner. */
public final class PackReadyForm {
    public static final String BACKPACK_STOCK = "backpack-stock";
    public static final String BACKPACK_PRICE = "backpack-price";
    public static final String NOTEBOOK_STOCK = "notebook-stock";
    public static final String NOTEBOOK_PRICE = "notebook-price";
    public static final String PENCIL_CASE_STOCK = "pencil-case-stock";
    public static final String PENCIL_CASE_PRICE = "pencil-case-price";
    public static final String BUDGET = "budget";
    public static final String CATEGORIES = "categories";

    private PackReadyForm() {
    }

    /**
     * Reads the original three categories in display order. Optional -required
     * fields default to 1 for legacy callers. The caller's map is never changed.
     *
     * @throws FieldValidationException for the first invalid field
     * @throws IllegalArgumentException if the entire form map is null
     */
    public static Request parse(Map<String, String> fields) {
        if (fields == null) {
            throw new IllegalArgumentException("Form fields are required.");
        }

        return parseCategories(List.of(
                new CategoryInput("backpack", "Backpacks", fields.get(BACKPACK_STOCK), fields.get(BACKPACK_PRICE),
                        fields.getOrDefault("backpack-required", "1")),
                new CategoryInput("notebook", "Notebooks", fields.get(NOTEBOOK_STOCK), fields.get(NOTEBOOK_PRICE),
                        fields.getOrDefault("notebook-required", "1")),
                new CategoryInput("pencil-case", "Pencil cases", fields.get(PENCIL_CASE_STOCK), fields.get(PENCIL_CASE_PRICE),
                        fields.getOrDefault("pencil-case-required", "1"))), fields.get(BUDGET));
    }

    /** Raw text for one active category. Its stable id identifies invalid fields. */
    public record CategoryInput(String id, String name, String stock, String unitPrice, String requiredQuantity) {
        /** Existing callers keep the original one-item-per-kit recipe. */
        public CategoryInput(String id, String name, String stock, String unitPrice) {
            this(id, name, stock, unitPrice, "1");
        }
    }

    /** Reads a nonempty recipe, including its positive required quantities. */
    public static Request parseCategories(List<CategoryInput> categories, String budget) {
        if (categories == null || categories.isEmpty()) {
            throw new FieldValidationException(CATEGORIES, "Add at least one category before calculating.");
        }
        List<Item> items = new ArrayList<>();
        List<String> names = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (CategoryInput category : categories) {
            if (category == null || category.id() == null
                    || !category.id().matches("[A-Za-z][A-Za-z0-9-]*") || !ids.add(category.id())) {
                throw new FieldValidationException(CATEGORIES, "Each category must have a unique identifier.");
            }
            String name;
            try {
                name = parseCategoryName(category.name(), names);
            } catch (IllegalArgumentException exception) {
                throw new FieldValidationException(category.id() + "-name", exception.getMessage(), exception);
            }
            names.add(name);
            int required;
            try {
                required = PackReadyInputs.parseRequiredQuantity(category.requiredQuantity());
            } catch (IllegalArgumentException exception) {
                throw new FieldValidationException(category.id() + "-required", name + ": " + exception.getMessage(), exception);
            }
            String stockField = category.id() + "-stock";
            String priceField = category.id() + "-price";
            Map<String, String> raw = new HashMap<>();
            raw.put(stockField, category.stock());
            raw.put(priceField, category.unitPrice());
            Item item = readItem(raw, name, stockField, priceField);
            items.add(new Item(name, item.getStock(), item.getUnitPriceCents(), required));
        }
        Map<String, String> rawBudget = new HashMap<>();
        rawBudget.put(BUDGET, budget);
        return new Request(items, readMoney(rawBudget, BUDGET, null, true));
    }

    /** Trims a category name and rejects missing, duplicate, or unreadable names. */
    public static String parseCategoryName(String input, Iterable<String> existingNames) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Category name is required.");
        }
        String name = input.strip();
        if (name.length() > 60) {
            throw new IllegalArgumentException("Keep category names to 60 characters or fewer.");
        }
        if (name.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Use a single line of text for the category name.");
        }
        for (String existing : existingNames) {
            if (name.equalsIgnoreCase(existing)) {
                throw new IllegalArgumentException("A category with this name already exists.");
            }
        }
        return name;
    }

    /** Fixed reference values for acceptance tests; the UI demo is randomized. */
    public static Map<String, String> demoFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(BACKPACK_STOCK, "25");
        fields.put(BACKPACK_PRICE, "12");
        fields.put(NOTEBOOK_STOCK, "15");
        fields.put(NOTEBOOK_PRICE, "1");
        fields.put(PENCIL_CASE_STOCK, "8");
        fields.put(PENCIL_CASE_PRICE, "2");
        fields.put(BUDGET, "30");
        return fields;
    }

    private static Item readItem(Map<String, String> fields, String name,
                                 String stockField, String priceField) {
        int stock;
        try {
            stock = PackReadyInputs.parseStock(fields.get(stockField));
        } catch (IllegalArgumentException exception) {
            throw new FieldValidationException(stockField, name + ": " + exception.getMessage(),
                    exception);
        }

        int priceCents = readMoney(fields, priceField, name, false);
        return new Item(name, stock, priceCents);
    }

    private static int readMoney(Map<String, String> fields, String field,
                                 String itemName, boolean allowZero) {
        try {
            return PackReadyInputs.parseMoneyCents(fields.get(field), allowZero);
        } catch (IllegalArgumentException exception) {
            String message = itemName == null
                    ? exception.getMessage()
                    : itemName + ": " + exception.getMessage();
            throw new FieldValidationException(field, message, exception);
        }
    }

    /** Validated values for the controller to adapt to the team's planner API. */
    public static final class Request {
        private final List<Item> items;
        private final int budgetCents;

        private Request(List<Item> items, int budgetCents) {
            this.items = Collections.unmodifiableList(new ArrayList<>(items));
            this.budgetCents = budgetCents;
        }

        public List<Item> getItems() {
            return items;
        }

        public int getBudgetCents() {
            return budgetCents;
        }
    }

    /** Immutable stock and price values; money is always integer cents. */
    public static final class Item {
        private final String name;
        private final int stock;
        private final int unitPriceCents;
        private final int requiredQuantity;

        private Item(String name, int stock, int unitPriceCents) {
            this(name, stock, unitPriceCents, 1);
        }

        private Item(String name, int stock, int unitPriceCents, int requiredQuantity) {
            this.name = name;
            this.stock = stock;
            this.unitPriceCents = unitPriceCents;
            this.requiredQuantity = requiredQuantity;
        }

        public String getName() {
            return name;
        }

        public int getStock() {
            return stock;
        }

        public int getUnitPriceCents() {
            return unitPriceCents;
        }

        public int getRequiredQuantity() {
            return requiredQuantity;
        }
    }

    /** Carries the field key so the JavaFX controller can focus that field. */
    public static final class FieldValidationException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
        private final String fieldName;

        private FieldValidationException(String fieldName, String message) {
            super(message);
            this.fieldName = fieldName;
        }

        private FieldValidationException(String fieldName, String message,
                                         IllegalArgumentException cause) {
            super(message, cause);
            this.fieldName = fieldName;
        }

        public String getFieldName() {
            return fieldName;
        }
    }
}
