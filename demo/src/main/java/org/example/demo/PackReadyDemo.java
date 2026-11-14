package org.example.demo;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/** Supplies fresh, valid demo values; fixed acceptance fixtures stay separate. */
public final class PackReadyDemo {
    private final Random random;
    private Map<String, String> previous = Map.of();

    public PackReadyDemo() {
        this(new Random());
    }

    PackReadyDemo(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    public Map<String, String> nextFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        add(fields, "backpack", 800, 1600);
        add(fields, "notebook", 50, 300);
        add(fields, "pencil-case", 100, 500);
        int budgetCents = (20 + random.nextInt(131)) * 100;
        fields.put(PackReadyForm.BUDGET, dollars(budgetCents));
        if (fields.equals(previous)) {
            budgetCents += budgetCents == 15000 ? -100 : 100;
            fields.put(PackReadyForm.BUDGET, dollars(budgetCents));
        }
        previous = Map.copyOf(fields);
        return fields;
    }

    private void add(Map<String, String> fields, String id, int minPrice, int maxPrice) {
        fields.put(id + "-required", Integer.toString(1 + random.nextInt(4)));
        fields.put(id + "-stock", Integer.toString(random.nextInt(46)));
        fields.put(id + "-price", dollars(minPrice + 25 * random.nextInt((maxPrice - minPrice) / 25 + 1)));
    }

    private static String dollars(int cents) {
        return BigDecimal.valueOf(cents, 2).toPlainString();
    }
}
