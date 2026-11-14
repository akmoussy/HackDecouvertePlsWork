package org.example.demo;

/** Standalone checks: run with javac and java, with no test library required. */
public final class PackReadyInputsTest {
    private static int checks;

    public static void main(String[] args) {
        // Stock: zero is valid, and blank is different from zero.
        expectEquals("stock 0", 0, PackReadyInputs.parseStock("0"));
        expectEquals("stock 25", 25, PackReadyInputs.parseStock("25"));
        expectEquals("stock with leading zeros", 25, PackReadyInputs.parseStock("00025"));
        expectEquals("stock with surrounding spaces", 25, PackReadyInputs.parseStock(" 25 "));
        for (String input : new String[] {
                "", " \t ", null, "-1", "2.5", "abc", "+1", "1e2", "1_000", "2 5"
        }) {
            expectInvalid("stock [" + input + "]", () -> PackReadyInputs.parseStock(input));
        }

        // All three dollar formats from Prompt 4 convert to exact cents.
        expectEquals("unit price 12", 1200, PackReadyInputs.parseMoneyCents("12", false));
        expectEquals("unit price 12.5", 1250, PackReadyInputs.parseMoneyCents("12.5", false));
        expectEquals("unit price 12.50", 1250, PackReadyInputs.parseMoneyCents("12.50", false));
        expectEquals("unit price 12.0", 1200, PackReadyInputs.parseMoneyCents("12.0", false));
        expectEquals("unit price 12.01", 1201, PackReadyInputs.parseMoneyCents("12.01", false));
        expectEquals("money with leading zeros", 1250, PackReadyInputs.parseMoneyCents("00012.50", false));
        expectEquals("budget 31.99", 3199, PackReadyInputs.parseMoneyCents("31.99", true));
        expectEquals("budget 0", 0, PackReadyInputs.parseMoneyCents("0", true));
        expectEquals("budget 0.00", 0, PackReadyInputs.parseMoneyCents("0.00", true));
        expectEquals("budget with leading zeros", 0, PackReadyInputs.parseMoneyCents("00.0", true));
        expectInvalid("unit price 0", () -> PackReadyInputs.parseMoneyCents("0", false));
        expectInvalid("unit price 0.0", () -> PackReadyInputs.parseMoneyCents("0.0", false));
        expectInvalid("unit price 0.00", () -> PackReadyInputs.parseMoneyCents("0.00", false));
        expectInvalid("unit price message requires a positive amount", "positive dollar amount",
                () -> PackReadyInputs.parseMoneyCents("-1", false));
        expectInvalid("budget message allows a nonnegative amount", "nonnegative dollar amount",
                () -> PackReadyInputs.parseMoneyCents("-1", true));
        expectEquals("smallest unit price", 1, PackReadyInputs.parseMoneyCents("0.01", false));
        expectEquals("exact cents for 0.29", 29, PackReadyInputs.parseMoneyCents("0.29", false));
        expectEquals("money with surrounding spaces", 1250,
                PackReadyInputs.parseMoneyCents(" 12.50 ", true));

        // Malformed amounts must fail for both budgets and unit prices.
        for (String input : new String[] {
                "12.001", "12.000", "", " \t ", null, "-1", "abc", "1e2", "$12", "12,50", "12.", ".50",
                "-0", "+12.50", "NaN", "Infinity", "12.5.0", "1 2.50"
        }) {
            expectInvalid("budget [" + input + "]",
                    () -> PackReadyInputs.parseMoneyCents(input, true));
            expectInvalid("unit price [" + input + "]",
                    () -> PackReadyInputs.parseMoneyCents(input, false));
        }

        // Valid text must also fit in an int, without wrapping around.
        expectEquals("largest stock", Integer.MAX_VALUE, PackReadyInputs.parseStock("2147483647"));
        expectInvalid("stock overflow", () -> PackReadyInputs.parseStock("2147483648"));
        expectEquals("largest money amount", Integer.MAX_VALUE,
                PackReadyInputs.parseMoneyCents("21474836.47", true));
        expectEquals("largest unit price", Integer.MAX_VALUE,
                PackReadyInputs.parseMoneyCents("21474836.47", false));
        expectInvalid("money overflow", () -> PackReadyInputs.parseMoneyCents("21474836.48", true));
        expectInvalid("unit price overflow", () -> PackReadyInputs.parseMoneyCents("21474836.48", false));

        System.out.println("All " + checks + " checks passed.");
    }

    private static void expectEquals(String description, int expected, int actual) {
        if (actual != expected) {
            throw new AssertionError(description + ": expected " + expected + ", got " + actual);
        }
        checks++;
        System.out.println("PASS " + description + " -> " + actual);
    }

    private static void expectInvalid(String description, Runnable action) {
        expectInvalid(description, null, action);
    }

    private static void expectInvalid(String description, String messagePart, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            if (messagePart != null && !expected.getMessage().contains(messagePart)) {
                throw new AssertionError(description + ": wrong message " + expected.getMessage());
            }
            checks++;
            System.out.println("PASS " + description + " rejected: " + expected.getMessage());
            return;
        }
        throw new AssertionError(description + ": expected IllegalArgumentException");
    }
}
