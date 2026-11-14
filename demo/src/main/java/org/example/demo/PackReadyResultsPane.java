package org.example.demo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** Structured JavaFX presentation of the planner's supplied values. */
final class PackReadyResultsPane extends VBox {
    private final Button export = new Button("Export CSV");
    private final Label exportStatus = PackReadyView.label("", "muted");
    private Planner.Plan displayedPlan;

    PackReadyResultsPane() {
        super(22);
        setId("results");
        getStyleClass().addAll("panel", "results-panel");
        setMinWidth(0);
        export.setId("export-csv");
        export.getStyleClass().add("secondary-button");
        export.setMaxWidth(Double.MAX_VALUE);
        export.setOnAction(event -> chooseCsvFile());
        exportStatus.setId("export-status");
        exportStatus.setWrapText(true);
        showEmpty();
    }

    void showEmpty() {
        displayedPlan = null;
        export.setDisable(true);
        clearExportStatus();
        Label heading = PackReadyView.label("Your shopping plan", "panel-title");
        Label step = PackReadyView.label("02", "step-number");
        HBox header = new HBox(10, step, heading);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = PackReadyView.label("Good things start\nwith a little planning.", "empty-title");
        title.setWrapText(true);
        Label help = PackReadyView.label(
                "Add your supplies and budget, then calculate to see how many complete kits you can make.", "muted");
        help.setWrapText(true);
        VBox empty = new VBox(20, PackReadyView.icon("bag"), title, help);
        empty.getStyleClass().add("result-empty");
        empty.setAlignment(Pos.CENTER);
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);
        help.setAlignment(Pos.CENTER);
        help.setMaxWidth(270);
        Label kit = PackReadyView.label("Every kit includes the required quantity from each category.", "kit-recipe");
        kit.setWrapText(true);
        kit.setMaxWidth(Double.MAX_VALUE);
        getChildren().setAll(header, empty, kit, export, exportStatus);
        setAccessibleText("Your shopping plan. Enter supplies and a budget to calculate.");
    }

    void showPlan(Planner.Plan plan) {
        displayedPlan = plan;
        export.setDisable(false);
        clearExportStatus();
        Label heading = PackReadyView.label("Your shopping plan", "panel-title");
        HBox header = new HBox(10, PackReadyView.label("02", "step-number"), heading);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox current = stat("READY NOW", Integer.toString(plan.currentKits()), "current-kits", false);
        VBox planned = stat("AFTER SHOPPING", Integer.toString(plan.plannedKits()), "planned-kits", true);
        HBox summary = new HBox(12, current, planned);
        summary.getStyleClass().add("result-summary");
        HBox.setHgrow(current, Priority.ALWAYS);
        HBox.setHgrow(planned, Priority.ALWAYS);

        Label shoppingTitle = PackReadyView.label("What to buy", "section-title");
        VBox shopping = new VBox(0);
        shopping.setId("shopping-list");
        shopping.getStyleClass().add("shopping-list");
        if (plan.purchases().isEmpty()) {
            Label empty = PackReadyView.label("This budget can\u2019t complete another kit.", "empty-shopping");
            empty.setWrapText(true);
            shopping.getChildren().add(empty);
        } else {
            for (Planner.Purchase purchase : plan.purchases()) {
                Label name = PackReadyView.label(purchase.name(), "purchase-name");
                name.setWrapText(true);
                name.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(name, Priority.ALWAYS);
                Label quantity = PackReadyView.label("\u00d7 " + purchase.quantity(), "purchase-quantity");
                quantity.setMinWidth(Label.USE_PREF_SIZE);
                Label subtotal = PackReadyView.label(PackReadyResults.formatCad(purchase.subtotalCents()), "purchase-cost");
                subtotal.setMinWidth(Label.USE_PREF_SIZE);
                HBox row = new HBox(10, name, quantity, subtotal);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("purchase-row");
                shopping.getChildren().add(row);
            }
        }

        VBox spent = money("Total spent", plan.totalCents(), "total-spent");
        VBox left = money("Budget left", plan.remainingCents(), "budget-left");
        left.getStyleClass().add("remaining-money");
        HBox totals = new HBox(18, spent, left);
        totals.getStyleClass().add("totals");
        HBox.setHgrow(spent, Priority.ALWAYS);
        HBox.setHgrow(left, Priority.ALWAYS);
        Label note = PackReadyView.label("A complete kit. A ready-to-learn student.", "result-note");
        note.setWrapText(true);
        getChildren().setAll(header, summary, shoppingTitle, shopping, totals, note, export, exportStatus);
        setAccessibleText(PackReadyResults.format(plan));
    }

    private void chooseCsvFile() {
        if (displayedPlan == null) { return; }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export shopping plan");
        chooser.setInitialFileName("packready-results.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file != null) { exportDisplayedPlan(file.toPath()); }
    }

    /** Also lets the acceptance checks exercise saving without a native dialog. */
    boolean exportDisplayedPlan(Path file) {
        if (displayedPlan == null) { return false; }
        try {
            PackReadyCsv.write(file, displayedPlan);
            exportStatus.getStyleClass().setAll("label", "muted");
            exportStatus.setText("Saved " + file.getFileName() + ".");
        } catch (IOException | SecurityException exception) {
            exportStatus.getStyleClass().setAll("label", "form-error");
            exportStatus.setText("Could not save the CSV. Choose a writable location and try again.");
            exportStatus.setVisible(true);
            exportStatus.setManaged(true);
            return false;
        }
        exportStatus.setVisible(true);
        exportStatus.setManaged(true);
        return true;
    }

    private void clearExportStatus() {
        exportStatus.setText("");
        exportStatus.setVisible(false);
        exportStatus.setManaged(false);
    }

    private static VBox stat(String caption, String value, String id, boolean accent) {
        Label label = PackReadyView.label(caption, "stat-caption");
        Label count = PackReadyView.label(value, "stat-count");
        count.setId(id);
        if (value.length() > 4) {
            int size = value.length() > 8 ? 20 : value.length() > 6 ? 26 : 34;
            count.setStyle("-fx-font-size: " + size + "px;");
        }
        Label kits = PackReadyView.label("complete kits", "stat-unit");
        VBox box = new VBox(6, label, count, kits);
        box.setMinWidth(0);
        box.setMaxWidth(Double.MAX_VALUE);
        box.getStyleClass().add("result-stat");
        if (accent) { box.getStyleClass().add("planned-stat"); }
        return box;
    }

    private static VBox money(String caption, long cents, String id) {
        Label value = PackReadyView.label(PackReadyResults.formatCad(cents), "money-value");
        value.setId(id);
        value.setWrapText(true);
        value.setMinWidth(0);
        VBox box = new VBox(7, PackReadyView.label(caption, "muted"), value);
        box.setMinWidth(0);
        return box;
    }
}
