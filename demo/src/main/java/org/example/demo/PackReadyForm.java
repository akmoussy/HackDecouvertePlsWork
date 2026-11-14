package org.example.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Plain Java form reading, independent of JavaFX and the team's planner. */
public final class PackReadyForm {
    public static final String BACKPACK_STOCK = "backpack-stock";
    public static final String BACKPACK_PRICE = "backpack-price";
    public static final String NOTEBOOK_STOCK = "notebook-stock";
    public static final String NOTEBOOK_PRICE = "notebook-price";
    public static final String PENCIL_CASE_STOCK = "pencil-case-stock";
    public static final String PENCIL_CASE_PRICE = "pencil-case-price";
    public static final String BUDGET = "budget";

    private PackReadyForm() {
    }

    /**
     * Reads the seven raw text fields in display order. Missing fields are
     * invalid, just like blank fields. The caller's map is never changed.
     *
     * @throws FieldValidationException for the first invalid field
     * @throws IllegalArgumentException if the entire form map is null
     */
    public static Request parse(Map<String, String> fields) {
        if (fields == null) {
            throw new IllegalArgumentException("Form fields are required.");
        }

        List<Item> items = new ArrayList<>();
        items.add(readItem(fields, "Backpacks", BACKPACK_STOCK, BACKPACK_PRICE));
        items.add(readItem(fields, "Notebooks", NOTEBOOK_STOCK, NOTEBOOK_PRICE));
        items.add(readItem(fields, "Pencil cases", PENCIL_CASE_STOCK, PENCIL_CASE_PRICE));
        int budgetCents = readMoney(fields, BUDGET, null, true);

        // Return a complete request only after every field has passed validation.
        return new Request(items, budgetCents);
    }

    /** Fresh, editable demo text for the controller to put in its seven fields. */
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

        private Item(String name, int stock, int unitPriceCents) {
            this.name = name;
            this.stock = stock;
            this.unitPriceCents = unitPriceCents;
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
    }

    /** Carries the field key so the JavaFX controller can focus that field. */
    public static final class FieldValidationException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
        private final String fieldName;

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
