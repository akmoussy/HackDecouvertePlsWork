/** Standalone checks: run with javac and java, with no test library required. */
public final class PackReadyInputsTest {
    private static int checks;

    public static void main(String[] args) {
        // Stock: zero is valid, and blank is different from zero.
        expectEquals("stock 0", 0, PackReadyInputs.parseStock("0"));
        expectEquals("stock 25", 25, PackReadyInputs.parseStock("25"));
        expectEquals("stock with surrounding spaces", 25, PackReadyInputs.parseStock(" 25 "));
        for (String input : new String[] {"", " \t ", null, "-1", "2.5", "abc", "+1"}) {
            expectInvalid("stock [" + input + "]", () -> PackReadyInputs.parseStock(input));
        }

        // All three dollar formats from Prompt 4 convert to exact cents.
        expectEquals("unit price 12", 1200, PackReadyInputs.parseMoneyCents("12", false));
        expectEquals("unit price 12.5", 1250, PackReadyInputs.parseMoneyCents("12.5", false));
        expectEquals("unit price 12.50", 1250, PackReadyInputs.parseMoneyCents("12.50", false));
        expectEquals("budget 0", 0, PackReadyInputs.parseMoneyCents("0", true));
        expectEquals("budget 0.00", 0, PackReadyInputs.parseMoneyCents("0.00", true));
        expectInvalid("unit price 0", () -> PackReadyInputs.parseMoneyCents("0", false));
        expectInvalid("unit price 0.00", () -> PackReadyInputs.parseMoneyCents("0.00", false));
        expectEquals("smallest unit price", 1, PackReadyInputs.parseMoneyCents("0.01", false));
        expectEquals("exact cents for 0.29", 29, PackReadyInputs.parseMoneyCents("0.29", false));
        expectEquals("money with surrounding spaces", 1250,
                PackReadyInputs.parseMoneyCents(" 12.50 ", true));

        // Malformed amounts must fail for both budgets and unit prices.
        for (String input : new String[] {
                "12.001", "12.000", "", " \t ", null, "-1", "abc", "1e2", "$12", "12,50", "12.", ".50"
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
        expectInvalid("money overflow", () -> PackReadyInputs.parseMoneyCents("21474836.48", true));

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
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            checks++;
            System.out.println("PASS " + description + " rejected: " + expected.getMessage());
            return;
        }
        throw new AssertionError(description + ": expected IllegalArgumentException");
    }
}
