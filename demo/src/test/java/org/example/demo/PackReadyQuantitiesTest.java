package org.example.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Per-kit quantity, overflow, optimality, and random demo regression checks. */
public final class PackReadyQuantitiesTest {
    private static int checks;

    public static void main(String[] args) {
        check("required quantity accepts 1", PackReadyInputs.parseRequiredQuantity("1") == 1);
        check("required quantity trims spaces", PackReadyInputs.parseRequiredQuantity(" 3 ") == 3);
        check("largest required quantity is representable", PackReadyInputs.parseRequiredQuantity("2147483647") == Integer.MAX_VALUE);
        for (String input : new String[] {"", " ", null, "0", "-1", "2.5", "2147483648", "abc"}) {
            expectInvalid("reject required quantity [" + input + "]", () -> PackReadyInputs.parseRequiredQuantity(input));
        }

        List<Planner.Item> weighted = List.of(
                new Planner.Item("Backpacks", 25, 1200, 2),
                new Planner.Item("Notebooks", 15, 100, 3),
                new Planner.Item("Pencil cases", 8, 200, 1));
        check("required quantities affect both ready kits and shopping units", Planner.planPurchase(weighted, 3000).equals(
                new Planner.Plan(5, 12, List.of(new Planner.Purchase("Notebooks", 21, 2100),
                        new Planner.Purchase("Pencil cases", 4, 800)), 2900, 100)));
        check("zero budget buys nothing with a weighted recipe", Planner.planPurchase(weighted, 0).equals(
                new Planner.Plan(5, 5, List.of(), 0, 0)));
        check("partial quantities are rounded down for current kits", Planner.planPurchase(List.of(
                new Planner.Item("Backpacks", 5, 1200, 2),
                new Planner.Item("Notebooks", 9, 100, 3),
                new Planner.Item("Pencil cases", 3, 200, 2)), 200).equals(
                new Planner.Plan(1, 2, List.of(new Planner.Purchase("Pencil cases", 1, 200)), 200, 0)));

        List<PackReadyForm.CategoryInput> raw = List.of(
                new PackReadyForm.CategoryInput("backpack", "Backpacks", "25", "12", "2"),
                new PackReadyForm.CategoryInput("notebook", "Notebooks", "15", "1", "3"),
                new PackReadyForm.CategoryInput("pencil-case", "Pencil cases", "8", "2", "1"));
        PackReadyForm.Request request = PackReadyForm.parseCategories(raw, "30");
        check("form preserves the entered per-kit quantity", request.getItems().get(0).getRequiredQuantity() == 2
                && request.getItems().get(1).getRequiredQuantity() == 3);
        check("full form-to-planner adapter preserves quantities", plan(request).equals(Planner.planPurchase(weighted, 3000)));
        Map<String, String> mapped = PackReadyForm.demoFields();
        mapped.put("backpack-required", "2");
        mapped.put("notebook-required", "3");
        check("map-based form callers also retain required quantities", plan(PackReadyForm.parse(mapped))
                .equals(Planner.planPurchase(weighted, 3000)));
        try {
            PackReadyForm.parseCategories(List.of(new PackReadyForm.CategoryInput("soap", "Soap", "0", "1", "0")), "30");
            throw new AssertionError("Zero required quantity should be rejected");
        } catch (PackReadyForm.FieldValidationException expected) {
            check("quantity errors identify the exact field", expected.getFieldName().equals("soap-required")
                    && expected.getMessage().contains("at least 1"));
        }
        check("legacy item callers still require one per kit", new Planner.Item("Soap", 3, 100).requiredQuantity() == 1);
        check("legacy form callers still require one per kit", PackReadyForm.parseCategories(List.of(
                new PackReadyForm.CategoryInput("soap", "Soap", "3", "1")), "0")
                .getItems().get(0).getRequiredQuantity() == 1);
        expectInvalid("planner rejects zero required quantities", () -> Planner.planPurchase(List.of(
                new Planner.Item("Soap", 1, 100, 0)), 100));

        check("maximum stock no longer wraps into negative kits", Planner.planPurchase(List.of(
                new Planner.Item("Backpacks", Integer.MAX_VALUE, 1200),
                new Planner.Item("Notebooks", Integer.MAX_VALUE, 100),
                new Planner.Item("Pencil cases", Integer.MAX_VALUE, 200)), 1500).equals(
                new Planner.Plan(Integer.MAX_VALUE, Integer.MAX_VALUE, List.of(), 0, 1500)));
        long units = (long) Integer.MAX_VALUE * Integer.MAX_VALUE - Integer.MAX_VALUE;
        check("large required quantities and purchase counts use long arithmetic", Planner.planPurchase(List.of(
                new Planner.Item("Soap", Integer.MAX_VALUE, 1, Integer.MAX_VALUE)), Long.MAX_VALUE).equals(
                new Planner.Plan(1, Integer.MAX_VALUE,
                        List.of(new Planner.Purchase("Soap", units, units)), units, Long.MAX_VALUE - units)));
        check("an overflowing aggregate cost is treated as unaffordable", Planner.planPurchase(List.of(
                new Planner.Item("A", 0, Long.MAX_VALUE), new Planner.Item("B", 0, 1)), Long.MAX_VALUE).equals(
                new Planner.Plan(0, 0, List.of(), 0, Long.MAX_VALUE)));
        check("huge quantity times huge price never overflows affordability", Planner.planPurchase(List.of(
                new Planner.Item("A", 0, Long.MAX_VALUE, Integer.MAX_VALUE)), Long.MAX_VALUE).equals(
                new Planner.Plan(0, 0, List.of(), 0, Long.MAX_VALUE)));
        check("weighted recipes match exhaustive search on 1000 cases", matchesBruteForce());

        PackReadyDemo first = new PackReadyDemo(new Random(42));
        PackReadyDemo second = new PackReadyDemo(new Random(42));
        check("demo generator can be tested reproducibly", first.nextFields().equals(second.nextFields()));
        PackReadyDemo repeated = new PackReadyDemo(new ZeroRandom());
        Map<String, String> editable = repeated.nextFields();
        editable.clear();
        check("even an identical draw changes the next demo and is defensively saved",
                repeated.nextFields().get("budget").equals("21.00"));
        check("500 demos are fresh, valid, and budget-safe", validRandomDemos());
        System.out.println("All " + checks + " required-quantity and demo checks passed.");
    }

    private static Planner.Plan plan(PackReadyForm.Request request) {
        return Planner.planPurchase(request.getItems().stream().map(item -> new Planner.Item(
                item.getName(), item.getStock(), item.getUnitPriceCents(), item.getRequiredQuantity())).toList(),
                request.getBudgetCents());
    }

    private static boolean validRandomDemos() {
        PackReadyDemo generator = new PackReadyDemo(new Random(73));
        Map<String, String> previous = Map.of();
        for (int n = 0; n < 500; n++) {
            Map<String, String> fields = generator.nextFields();
            if (fields.equals(previous) || fields.size() != 10) { return false; }
            List<PackReadyForm.CategoryInput> rows = new ArrayList<>();
            for (String id : List.of("backpack", "notebook", "pencil-case")) {
                rows.add(new PackReadyForm.CategoryInput(id, id, fields.get(id + "-stock"),
                        fields.get(id + "-price"), fields.get(id + "-required")));
            }
            PackReadyForm.Request request = PackReadyForm.parseCategories(rows, fields.get("budget"));
            Planner.Plan result = plan(request);
            if (request.getBudgetCents() < 2000 || request.getBudgetCents() > 15000
                    || result.plannedKits() < result.currentKits() || result.remainingCents() < 0
                    || result.totalCents() + result.remainingCents() != request.getBudgetCents()) { return false; }
            for (PackReadyForm.Item item : request.getItems()) {
                if (item.getRequiredQuantity() < 1 || item.getRequiredQuantity() > 4
                        || item.getStock() < 0 || item.getStock() > 45 || item.getUnitPriceCents() <= 0) { return false; }
                long purchased = result.purchases().stream().filter(p -> p.name().equals(item.getName()))
                        .mapToLong(Planner.Purchase::quantity).sum();
                if ((long) item.getStock() + purchased < (long) result.plannedKits() * item.getRequiredQuantity()) { return false; }
            }
            previous = Map.copyOf(fields);
        }
        return true;
    }

    private static boolean matchesBruteForce() {
        Random random = new Random(89);
        for (int n = 0; n < 1000; n++) {
            List<Planner.Item> items = new ArrayList<>();
            int current = Integer.MAX_VALUE;
            for (int i = 0; i < 3; i++) {
                Planner.Item item = new Planner.Item("Item " + i, random.nextInt(21),
                        1 + random.nextInt(50), 1 + random.nextInt(5));
                items.add(item);
                current = Math.min(current, item.stock() / item.requiredQuantity());
            }
            int budget = random.nextInt(501);
            int best = current;
            for (int target = current; target <= current + budget + 1; target++) {
                if (cost(items, target) <= budget) { best = target; }
            }
            Planner.Plan actual = Planner.planPurchase(items, budget);
            if (actual.currentKits() != current || actual.plannedKits() != best
                    || actual.totalCents() != cost(items, best) || actual.remainingCents() != budget - cost(items, best)) { return false; }
        }
        return true;
    }

    private static long cost(List<Planner.Item> items, int kits) {
        long cost = 0;
        for (Planner.Item item : items) {
            cost += Math.max(0L, (long) kits * item.requiredQuantity() - item.stock()) * item.unitPriceCents();
        }
        return cost;
    }

    private static void expectInvalid(String label, Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { check(label, true); return; }
        throw new AssertionError(label + " should be rejected");
    }

    private static void check(String label, boolean passed) {
        if (!passed) { throw new AssertionError(label); }
        checks++;
        System.out.println("PASS " + label);
    }

    private static final class ZeroRandom extends Random {
        private static final long serialVersionUID = 1L;
        @Override public int nextInt(int bound) { return 0; }
    }
}
