package org.example.demo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

/** Programmatic JavaFX layout, with the shared seven-field contract. */
public final class PackReadyView extends ScrollPane {
    private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");
    private static final PseudoClass NARROW = PseudoClass.getPseudoClass("narrow");
    private final Map<String, TextField> fields = new LinkedHashMap<>();
    private final Button calculate = new Button("Calculate plan  \u2192");
    private final Button demo = new Button("Load demo");
    private final Label error = label("", "form-error");
    private final PackReadyResultsPane results = new PackReadyResultsPane();
    private final VBox page = new VBox(28);
    private final GridPane columns = new GridPane();
    private boolean stacked;

    public PackReadyView() {
        setId("packready");
        getStyleClass().add("app-scroll");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        getStylesheets().add(Objects.requireNonNull(
                PackReadyView.class.getResource("packready.css")).toExternalForm());

        page.setId("page");
        page.getStyleClass().add("page");
        page.setMaxWidth(1180);
        page.getChildren().addAll(header(), introduction(), mainPanels(), footer());
        StackPane canvas = new StackPane(page);
        canvas.setAlignment(Pos.TOP_CENTER);
        canvas.getStyleClass().add("canvas");
        setContent(canvas);
        viewportBoundsProperty().addListener((observable, previous, bounds) -> {
            pseudoClassStateChanged(NARROW, bounds.getWidth() < 620);
            arrangePanels(bounds.getWidth() < 940);
        });
    }

    private Node header() {
        Label mark = label("P", "brand-mark");
        Label name = label("PackReady", "brand-name");
        Label tagline = label("A little planning. More complete kits.", "brand-tagline");
        VBox wordmark = new VBox(2, name, tagline);
        HBox identity = new HBox(12, mark, wordmark);
        identity.setAlignment(Pos.CENTER_LEFT);
        Label currency = label("CAD  /  SCHOOL KITS", "eyebrow");
        HBox header = new HBox(16, identity, spacer(), currency);
        header.setAlignment(Pos.CENTER_LEFT);
        // The long tagline and currency marker yield space to the form on small windows.
        widthProperty().addListener((observable, previous, width) -> {
            boolean wide = width.doubleValue() >= 620;
            currency.setVisible(wide);
            currency.setManaged(wide);
            tagline.setVisible(wide);
            tagline.setManaged(wide);
        });
        return header;
    }

    private Node introduction() {
        Label eyebrow = label("SMALL SUPPLIES. BIG POSSIBILITIES.", "eyebrow");
        Label title = label("Turn donated supplies\ninto complete school kits.", "hero-title");
        title.setWrapText(true);
        Label description = label("Count what you have, set your budget, and make a shopping plan that goes further.",
                "intro-copy");
        description.setWrapText(true);
        VBox intro = new VBox(9, eyebrow, title, description);
        intro.getStyleClass().add("intro");
        return intro;
    }

    private Node mainPanels() {
        columns.setHgap(24);
        columns.setVgap(24);
        columns.setId("planner-panels");
        VBox form = form();
        columns.add(form, 0, 0);
        columns.add(results, 1, 0);
        GridPane.setValignment(results, javafx.geometry.VPos.TOP);
        arrangePanels(false);
        return columns;
    }

    private void arrangePanels(boolean compact) {
        if (columns.getChildren().size() < 2) {
            return;
        }
        if (!columns.getColumnConstraints().isEmpty() && stacked == compact) {
            return;
        }
        stacked = compact;
        ColumnConstraints first = new ColumnConstraints();
        first.setPercentWidth(compact ? 100 : 57);
        if (compact) {
            columns.getColumnConstraints().setAll(first);
            GridPane.setColumnIndex(results, 0);
            GridPane.setRowIndex(results, 1);
        } else {
            ColumnConstraints second = new ColumnConstraints();
            second.setPercentWidth(43);
            columns.getColumnConstraints().setAll(first, second);
            GridPane.setColumnIndex(results, 1);
            GridPane.setRowIndex(results, 0);
        }
    }

    private VBox form() {
        Label step = label("01", "step-number");
        Label title = label("Build your plan", "panel-title");
        HBox heading = new HBox(10, step, title);
        heading.setAlignment(Pos.CENTER_LEFT);
        Label description = label("Start with the supplies already on your shelf.", "muted");
        description.setWrapText(true);
        VBox headingBlock = new VBox(7, heading, description);

        VBox rows = new VBox(0,
                new SupplyRow("Backpacks", "Carry the essentials", "bag",
                        PackReadyForm.BACKPACK_STOCK, PackReadyForm.BACKPACK_PRICE, "25", "12.00"),
                new SupplyRow("Notebooks", "Room for new ideas", "book",
                        PackReadyForm.NOTEBOOK_STOCK, PackReadyForm.NOTEBOOK_PRICE, "15", "1.00"),
                new SupplyRow("Pencil cases", "Keep the little things together", "case",
                        PackReadyForm.PENCIL_CASE_STOCK, PackReadyForm.PENCIL_CASE_PRICE, "8", "2.00"));

        Label budgetLabel = label("Budget (CAD)", "budget-title");
        Label budgetHelp = label("How much can you spend?", "muted");
        VBox budgetCopy = new VBox(4, budgetLabel, budgetHelp);
        TextField budgetField = field(PackReadyForm.BUDGET, "Budget in Canadian dollars", "30.00");
        budgetLabel.setLabelFor(budgetField);
        StackPane budgetInput = moneyInput(budgetField);
        budgetInput.setMinWidth(140);
        budgetInput.setMaxWidth(190);
        HBox budget = new HBox(16, budgetCopy, spacer(), budgetInput);
        budget.setAlignment(Pos.CENTER_LEFT);
        budget.getStyleClass().add("budget-box");
        budget.widthProperty().addListener((observable, previous, width) -> {
            boolean compact = width.doubleValue() < 380;
            budgetHelp.setVisible(!compact);
            budgetHelp.setManaged(!compact);
            budgetInput.setMaxWidth(compact ? 145 : 190);
        });

        error.setId("form-error");
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);
        calculate.setId("calculate");
        calculate.setDefaultButton(true);
        calculate.setMaxWidth(Double.MAX_VALUE);
        calculate.getStyleClass().add("primary-button");
        demo.setId("load-demo");
        demo.getStyleClass().add("secondary-button");
        HBox actions = new HBox(10, calculate, demo);
        HBox.setHgrow(calculate, Priority.ALWAYS);
        Label note = label("One backpack + one notebook + one pencil case = one kit.", "form-note");
        note.setWrapText(true);
        note.setMaxWidth(Double.MAX_VALUE);

        VBox form = new VBox(18, headingBlock, rows, budget, error, actions, note);
        form.setId("planner-form");
        form.getStyleClass().add("panel");
        form.setMinWidth(0);
        return form;
    }

    private Node footer() {
        Label footer = label("Thoughtful shopping. More students ready for what's next.", "footer-copy");
        footer.setWrapText(true);
        HBox box = new HBox(footer);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private TextField field(String name, String accessibleName, String prompt) {
        TextField field = new TextField();
        field.setId(name);
        field.setAccessibleText(accessibleName);
        field.setPromptText(prompt);
        field.setPrefColumnCount(6);
        field.setMinWidth(0);
        field.setMaxWidth(Double.MAX_VALUE);
        fields.put(name, field);
        return field;
    }

    private static StackPane moneyInput(TextField field) {
        field.getStyleClass().add("money-field");
        Label symbol = label("$", "money-symbol");
        symbol.setMouseTransparent(true);
        symbol.setPadding(new Insets(0, 0, 0, 13));
        StackPane input = new StackPane(field, symbol);
        StackPane.setAlignment(symbol, Pos.CENTER_LEFT);
        return input;
    }

    void clearError() {
        error.setText("");
        error.setVisible(false);
        error.setManaged(false);
        fields.values().forEach(field -> {
            field.pseudoClassStateChanged(INVALID, false);
            field.setAccessibleHelp(null);
        });
    }

    void showError(String name, String message) {
        results.showEmpty();
        error.setText(message);
        error.setVisible(true);
        error.setManaged(true);
        TextField field = fields.get(name);
        if (field != null) {
            field.pseudoClassStateChanged(INVALID, true);
            field.setAccessibleHelp(message);
            Platform.runLater(() -> {
                field.requestFocus();
                // Keep the focused input visible when the panels are stacked.
                if (stacked) {
                    applyCss();
                    layout();
                    Bounds bounds = getContent().sceneToLocal(field.localToScene(field.getBoundsInLocal()));
                    double overflow = getContent().getLayoutBounds().getHeight() - getViewportBounds().getHeight();
                    if (overflow > 0) {
                        double centered = bounds.getMinY() - getViewportBounds().getHeight() / 2;
                        setVvalue(Math.max(0, Math.min(1, centered / overflow)));
                    }
                }
            });
        }
    }

    Map<String, TextField> fields() { return Collections.unmodifiableMap(fields); }
    Button calculateButton() { return calculate; }
    Button demoButton() { return demo; }
    PackReadyResultsPane results() { return results; }

    static Label label(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        return label;
    }

    static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private final class SupplyRow extends GridPane {
        private final HBox identity;
        private final VBox stock;
        private final VBox price;
        private Boolean compact;

        SupplyRow(String name, String help, String icon, String stockName, String priceName,
                  String stockPrompt, String pricePrompt) {
            getStyleClass().add("supply-row");
            setHgap(14);
            setVgap(12);
            Label title = label(name, "supply-name");
            Label hint = label(help, "supply-help");
            hint.setWrapText(true);
            VBox copy = new VBox(4, title, hint);
            identity = new HBox(10, icon(icon), copy);
            identity.setAlignment(Pos.CENTER_LEFT);
            identity.setMinWidth(0);
            HBox.setHgrow(copy, Priority.ALWAYS);
            TextField stockField = field(stockName, name + " in stock", stockPrompt);
            Label stockLabel = label("In stock", "field-label");
            stockLabel.setLabelFor(stockField);
            stock = new VBox(7, stockLabel, stockField);
            TextField priceField = field(priceName, name + " unit price in Canadian dollars", pricePrompt);
            Label priceLabel = label("Unit price (CAD)", "field-label");
            priceLabel.setLabelFor(priceField);
            price = new VBox(7, priceLabel, moneyInput(priceField));
            getChildren().addAll(identity, stock, price);
            arrange(false);
            widthProperty().addListener((observable, previous, width) -> arrange(width.doubleValue() < 510));
        }

        private void arrange(boolean small) {
            if (compact != null && compact == small) { return; }
            compact = small;
            ColumnConstraints first = new ColumnConstraints();
            ColumnConstraints second = new ColumnConstraints();
            first.setPercentWidth(small ? 50 : 42);
            second.setPercentWidth(small ? 50 : 25);
            if (small) {
                getColumnConstraints().setAll(first, second);
            } else {
                ColumnConstraints third = new ColumnConstraints();
                third.setPercentWidth(33);
                getColumnConstraints().setAll(first, second, third);
            }
            GridPane.setConstraints(identity, 0, 0, small ? 2 : 1, 1);
            GridPane.setConstraints(stock, small ? 0 : 1, small ? 1 : 0);
            GridPane.setConstraints(price, small ? 1 : 2, small ? 1 : 0);
        }
    }

    static StackPane icon(String kind) {
        String path = switch (kind) {
            case "book" -> "M5 3 H18 V21 H5 Q2 21 2 18 V6 Q2 3 5 3 M5 3 V21 M9 8 H15 M9 12 H15";
            case "case" -> "M3 7 H20 V19 H3 Z M7 7 V4 H16 V7 M3 11 H20 M15 11 V14";
            default -> "M7 7 V5 Q7 2 11 2 Q15 2 15 5 V7 M4 7 H18 L20 21 H2 Z M8 13 H14 V18 H8 Z";
        };
        SVGPath shape = new SVGPath();
        shape.setContent(path);
        shape.getStyleClass().add("supply-icon-path");
        StackPane icon = new StackPane(shape);
        icon.getStyleClass().add("supply-icon");
        icon.setMinSize(38, 38);
        icon.setPrefSize(38, 38);
        icon.setMaxSize(38, 38);
        return icon;
    }
}
