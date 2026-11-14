package org.example.demo;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Simple CSV export of the displayed plan; no spreadsheet library required. */
public final class PackReadyCsv {
    private PackReadyCsv() {}

    public static String format(Planner.Plan plan) {
        Objects.requireNonNull(plan, "plan");
        StringBuilder csv = new StringBuilder();
        row(csv, "Section", "Item", "Quantity", "Amount (CAD)");
        row(csv, "Summary", "Kits ready now", Integer.toString(plan.currentKits()), "");
        row(csv, "Summary", "Kits after shopping", Integer.toString(plan.plannedKits()), "");
        for (Planner.Purchase purchase : plan.purchases()) {
            row(csv, "Purchase", spreadsheetText(purchase.name()), Long.toString(purchase.quantity()),
                    dollars(purchase.subtotalCents()));
        }
        row(csv, "Summary", "Total spent", "", dollars(plan.totalCents()));
        row(csv, "Summary", "Budget left", "", dollars(plan.remainingCents()));
        return csv.toString();
    }

    public static void write(Path file, Planner.Plan plan) throws IOException {
        // UTF-8 BOM helps spreadsheet apps recognize accented category names.
        Files.writeString(file, "\uFEFF" + format(plan), StandardCharsets.UTF_8);
    }

    private static String dollars(long cents) {
        return BigDecimal.valueOf(cents, 2).toPlainString();
    }

    private static void row(StringBuilder csv, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) { csv.append(','); }
            csv.append('"').append(cells[i].replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }

    private static String spreadsheetText(String text) {
        String leading = text.stripLeading();
        if (!leading.isEmpty() && "=+-@".indexOf(leading.charAt(0)) >= 0) {
            return "'" + text;
        }
        return text;
    }
}
