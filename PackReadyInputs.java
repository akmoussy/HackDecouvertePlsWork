import java.math.BigDecimal;

/**
 * Plain Java input validation for PackReady's JavaFX controller.
 * Temporarily uses the default package until the team confirms its Java layout.
 */
public final class PackReadyInputs {
    private PackReadyInputs() {
        // Call the static methods directly; no instance is needed.
    }

    /**
     * Parses a nonnegative whole-number stock count, ignoring surrounding spaces.
     *
     * @return a count from 0 through Integer.MAX_VALUE
     * @throws IllegalArgumentException if input is missing, malformed, or too large
     */
    public static int parseStock(String input) {
        String text = requireText(input, "Stock");

        // Validate the text before conversion: digits only, with no sign or decimal.
        if (!text.matches("[0-9]+")) {
            throw new IllegalArgumentException("Stock must be a nonnegative whole number.");
        }

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Stock must be at most " + Integer.MAX_VALUE + ".", exception);
        }
    }

    /**
     * Parses dollars such as 12, 12.5, or 12.50 into exact integer cents.
     * Ignores surrounding spaces. Requires digits before the decimal point and,
     * when a decimal point is present, one or two digits after it.
     *
     * @param allowZero true for the budget; false for a unit price
     * @return cents from 0 (or 1 for a unit price) through Integer.MAX_VALUE
     * @throws IllegalArgumentException if input is missing, malformed, too large,
     *                                  or zero when allowZero is false
     */
    public static int parseMoneyCents(String input, boolean allowZero) {
        String field = allowZero ? "Budget" : "Unit price";
        String text = requireText(input, field);

        // Check precision first, so even 12.000 is rejected rather than rounded.
        if (!text.matches("[0-9]+(\\.[0-9]{1,2})?")) {
            throw new IllegalArgumentException(
                    field + " must be a nonnegative dollar amount with at most two decimal places."
                            + " Examples: 12, 12.5, 12.50.");
        }

        int cents;
        try {
            // Construct from text, avoiding floating-point rounding errors.
            cents = new BigDecimal(text).movePointRight(2).intValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    field + " must be at most 21474836.47 dollars.", exception);
        }

        if (!allowZero && cents == 0) {
            throw new IllegalArgumentException("Unit price must be greater than zero.");
        }

        return cents;
    }

    private static String requireText(String input, String field) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return input.trim();
    }
}
