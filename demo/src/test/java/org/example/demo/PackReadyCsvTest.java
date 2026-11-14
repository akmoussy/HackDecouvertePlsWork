package org.example.demo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Checks CSV contents and actual UTF-8 file output. */
public final class PackReadyCsvTest {
    public static void main(String[] args) throws Exception {
        Planner.Plan plan = new Planner.Plan(8, 20, List.of(
                new Planner.Purchase("Notebooks", 5, 500),
                new Planner.Purchase("Pencil cases", 12, 2400)), 2900, 100);
        String expected = "\"Section\",\"Item\",\"Quantity\",\"Amount (CAD)\"\r\n"
                + "\"Summary\",\"Kits ready now\",\"8\",\"\"\r\n"
                + "\"Summary\",\"Kits after shopping\",\"20\",\"\"\r\n"
                + "\"Purchase\",\"Notebooks\",\"5\",\"5.00\"\r\n"
                + "\"Purchase\",\"Pencil cases\",\"12\",\"24.00\"\r\n"
                + "\"Summary\",\"Total spent\",\"\",\"29.00\"\r\n"
                + "\"Summary\",\"Budget left\",\"\",\"1.00\"\r\n";
        check("full plan CSV", expected.equals(PackReadyCsv.format(plan)));
        String empty = PackReadyCsv.format(new Planner.Plan(8, 8, List.of(), 0, 0));
        check("empty shopping list keeps counts and totals", empty.lines().count() == 5
                && !empty.contains("Purchase") && empty.contains("\"0.00\""));
        String special = PackReadyCsv.format(new Planner.Plan(0, 1, List.of(
                new Planner.Purchase("Étuis, \"bleus\"\nextra", 2, 1)), 1, 9));
        check("quotes, commas, Unicode and newlines are escaped", special.contains("\"Étuis, \"\"bleus\"\"\nextra\""));
        check("cents stay exact", special.contains("\"0.01\"") && special.contains("\"0.09\""));
        check("spreadsheet formula-like names remain text", PackReadyCsv.format(new Planner.Plan(0, 1,
                List.of(new Planner.Purchase("=1+1", 1, 100)), 100, 0)).contains("\"'=1+1\""));
        Path file = Files.createTempFile("packready-csv-test-", ".csv");
        PackReadyCsv.write(file, plan);
        check("saved UTF-8 file contains exact results", Files.readString(file, StandardCharsets.UTF_8).equals("\uFEFF" + expected));
        System.out.println("All 6 CSV checks passed.");
    }

    private static void check(String label, boolean success) {
        if (!success) { throw new AssertionError(label); }
        System.out.println("PASS " + label);
    }
}
