import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Checks the complete form without requiring JavaFX or a planner implementation. */
public final class PackReadyFormTest {
    private static int checks;

    public static void main(String[] args) {
        Map<String, String> fields = PackReadyForm.demoFields();
        Map<String, String> original = new LinkedHashMap<>(fields);
        PackReadyForm.Request request = PackReadyForm.parse(fields);

        expectEquals("seven demo fields", 7, fields.size());
        expectEquals("three items", 3, request.getItems().size());
        expectItem(request.getItems().get(0), "Backpacks", 25, 1200);
        expectItem(request.getItems().get(1), "Notebooks", 15, 100);
        expectItem(request.getItems().get(2), "Pencil cases", 8, 200);
        expectEquals("demo budget in cents", 3000, request.getBudgetCents());
        expectEquals("parsing preserves raw input", original, fields);
        fields.put(PackReadyForm.BACKPACK_STOCK, "99");
        expectEquals("validated values survive later form edits", 25,
                request.getItems().get(0).getStock());
        fields.put(PackReadyForm.BACKPACK_STOCK, "25");

        for (String budget : new String[] {"0", "30", "32"}) {
            fields.put(PackReadyForm.BUDGET, budget);
            expectEquals("budget " + budget, Integer.parseInt(budget) * 100,
                    PackReadyForm.parse(fields).getBudgetCents());
        }

        fields.put(PackReadyForm.BACKPACK_PRICE, "12.50");
        expectEquals("form preserves exact price cents", 1250,
                PackReadyForm.parse(fields).getItems().get(0).getUnitPriceCents());
        for (String stockField : new String[] {
                PackReadyForm.BACKPACK_STOCK, PackReadyForm.NOTEBOOK_STOCK, PackReadyForm.PENCIL_CASE_STOCK
        }) {
            fields.put(stockField, "0");
        }
        request = PackReadyForm.parse(fields);
        for (PackReadyForm.Item item : request.getItems()) {
            expectEquals(item.getName() + " zero stock", 0, item.getStock());
        }

        // A blank or missing field must identify its exact field, not become zero.
        for (String field : original.keySet()) {
            fields = PackReadyForm.demoFields();
            fields.put(field, " ");
            expectInvalidField("blank " + field, fields, field, "required");
            fields.remove(field);
            expectInvalidField("missing " + field, fields, field, "required");
        }

        for (String field : new String[] {
                PackReadyForm.BACKPACK_PRICE, PackReadyForm.NOTEBOOK_PRICE, PackReadyForm.PENCIL_CASE_PRICE
        }) {
            fields = PackReadyForm.demoFields();
            fields.put(field, "0");
            expectInvalidField("zero " + field, fields, field, "greater than zero");
            fields.put(field, "12.001");
            expectInvalidField("extra decimals " + field, fields, field, "two decimal places");
        }
        fields = PackReadyForm.demoFields();
        fields.put(PackReadyForm.BUDGET, "12.001");
        expectInvalidField("extra decimals in budget", fields, PackReadyForm.BUDGET, "two decimal places");

        // Fail at the first invalid field in display order, regardless of map order.
        fields = new LinkedHashMap<>();
        fields.put(PackReadyForm.BUDGET, "bad");
        fields.put(PackReadyForm.BACKPACK_STOCK, "bad");
        expectInvalidField("first of several invalid fields", fields,
                PackReadyForm.BACKPACK_STOCK, "Backpacks");
        fields = PackReadyForm.demoFields();
        fields.put(PackReadyForm.BACKPACK_PRICE, "");
        fields.put(PackReadyForm.NOTEBOOK_STOCK, "");
        expectInvalidField("price before next item's stock", fields,
                PackReadyForm.BACKPACK_PRICE, "Backpacks");
        fields = PackReadyForm.demoFields();
        fields.put(PackReadyForm.NOTEBOOK_STOCK, "-1");
        expectInvalidField("specific notebook error", fields, PackReadyForm.NOTEBOOK_STOCK, "Notebooks");
        fields = PackReadyForm.demoFields();
        fields.put(PackReadyForm.PENCIL_CASE_PRICE, "abc");
        expectInvalidField("specific pencil case error", fields, PackReadyForm.PENCIL_CASE_PRICE, "Pencil cases");

        // Reloading demo data restores all seven fields without sharing mutable state.
        fields.clear();
        expectEquals("demo reload restores defaults", original, PackReadyForm.demoFields());
        request = PackReadyForm.parse(PackReadyForm.demoFields());
        try {
            request.getItems().clear();
            throw new AssertionError("Validated items should be immutable");
        } catch (UnsupportedOperationException expected) {
            checks++;
            System.out.println("PASS validated items are immutable");
        }

        System.out.println("All " + checks + " form checks passed.");
    }

    private static void expectItem(PackReadyForm.Item item, String name, int stock, int priceCents) {
        expectEquals(name + " display name", name, item.getName());
        expectEquals(name + " stock", stock, item.getStock());
        expectEquals(name + " price cents", priceCents, item.getUnitPriceCents());
    }

    private static void expectEquals(String description, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(description + ": expected " + expected + ", got " + actual);
        }
        checks++;
        System.out.println("PASS " + description);
    }

    private static void expectInvalidField(String description, Map<String, String> fields,
                                          String expectedField, String messagePart) {
        Map<String, String> original = new LinkedHashMap<>(fields);
        try {
            PackReadyForm.parse(fields);
        } catch (PackReadyForm.FieldValidationException expected) {
            if (!expectedField.equals(expected.getFieldName())) {
                throw new AssertionError(description + ": wrong field " + expected.getFieldName());
            }
            if (!expected.getMessage().contains(messagePart)) {
                throw new AssertionError(description + ": wrong message " + expected.getMessage());
            }
            if (!original.equals(fields)) {
                throw new AssertionError(description + ": parsing changed the raw fields");
            }
            checks++;
            System.out.println("PASS " + description + " rejected: " + expected.getMessage());
            return;
        }
        throw new AssertionError(description + ": expected FieldValidationException");
    }
}
