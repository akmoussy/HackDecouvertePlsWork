package org.example.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

/** JavaFX layout with editable kit categories and the original three-category demo. */
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
    private final VBox rows = new VBox(0);
    private final List<SupplyRow> categories = new ArrayList<>();
    private final Button addCategory = new Button("+ Add category");
    private final TextField categoryName = new TextField();
    private final Button confirmCategory = new Button("Add");
    private final Button cancelCategory = new Button("Cancel");
    private final Label categoryError = label("", "form-error");
    private final VBox categoryEditor = new VBox(9);
    private final Label removedMessage = label("", "muted");
    private final Button undoRemoval = new Button("Undo");
    private final HBox undoNotice = new HBox(10);
    private SupplyRow lastRemoved;
    private int lastRemovedIndex;
    private int nextCategoryId = 1;
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
        addCategory.setId("add-category");
        addCategory.getStyleClass().addAll("secondary-button", "add-category-button");
        addCategory.setOnAction(event -> showCategoryEditor());
        HBox heading = new HBox(10, step, title, spacer(), addCategory);
        heading.setAlignment(Pos.CENTER_LEFT);
        Label description = label("Start with the supplies already on your shelf.", "muted");
        description.setWrapText(true);
        VBox headingBlock = new VBox(7, heading, description);

        addDefaultRows();
        prepareCategoryEditor();
        removedMessage.setWrapText(true);
        HBox.setHgrow(removedMessage, Priority.ALWAYS);
        undoRemoval.setId("undo-remove-category");
        undoRemoval.setAccessibleText("Undo the last category removal");
        undoRemoval.getStyleClass().addAll("secondary-button", "add-category-button");
        undoRemoval.setOnAction(event -> undoCategoryRemoval());
        undoNotice.getChildren().setAll(removedMessage, spacer(), undoRemoval);
        undoNotice.setAlignment(Pos.CENTER_LEFT);
        undoNotice.getStyleClass().add("undo-notice");
        setShown(undoNotice, false);

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
        demo.setTooltip(new Tooltip("Generate fresh per-kit quantities, stock, prices, and a budget for the three demo categories."));
        demo.getStyleClass().add("secondary-button");
        HBox actions = new HBox(10, calculate, demo);
        HBox.setHgrow(calculate, Priority.ALWAYS);
        Label note = label("Each kit uses the required quantity from every category.", "form-note");
        note.setWrapText(true);
        note.setMaxWidth(Double.MAX_VALUE);

        VBox form = new VBox(18, headingBlock, categoryEditor, undoNotice, rows, budget, error, actions, note);
        form.setId("planner-form");
        form.getStyleClass().add("panel");
        form.setMinWidth(0);
        return form;
    }

    private void addDefaultRows() {
        categories.add(new SupplyRow("Backpacks", "Carry the essentials", "bag",
                PackReadyForm.BACKPACK_STOCK, PackReadyForm.BACKPACK_PRICE, "25", "12.00"));
        categories.add(new SupplyRow("Notebooks", "Room for new ideas", "book",
                PackReadyForm.NOTEBOOK_STOCK, PackReadyForm.NOTEBOOK_PRICE, "15", "1.00"));
        categories.add(new SupplyRow("Pencil cases", "Keep the little things together", "case",
                PackReadyForm.PENCIL_CASE_STOCK, PackReadyForm.PENCIL_CASE_PRICE, "8", "2.00"));
        updateRows();
    }

    private void prepareCategoryEditor() {
        categoryEditor.setId("category-editor");
        categoryEditor.getStyleClass().add("category-editor");
        Label nameLabel = label("New category name", "field-label");
        categoryName.setId("new-category-name");
        categoryName.setPromptText("e.g. Water bottles");
        categoryName.setAccessibleText("New category name");
        categoryName.setMinWidth(0);
        nameLabel.setLabelFor(categoryName);
        confirmCategory.setId("confirm-add-category");
        confirmCategory.getStyleClass().addAll("primary-button", "add-category-button");
        cancelCategory.setId("cancel-add-category");
        cancelCategory.getStyleClass().addAll("secondary-button", "add-category-button");
        cancelCategory.setOnAction(event -> {
            hideCategoryEditor();
            addCategory.requestFocus();
        });
        categoryName.setOnAction(event -> confirmCategory.fire());
        categoryName.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                cancelCategory.fire();
                event.consume();
            }
        });
        HBox entry = new HBox(8, categoryName, confirmCategory, cancelCategory);
        entry.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(categoryName, Priority.ALWAYS);
        categoryError.setId("category-error");
        categoryError.setWrapText(true);
        setShown(categoryError, false);
        categoryEditor.getChildren().setAll(nameLabel, entry, categoryError);
        setShown(categoryEditor, false);
    }

    private void showCategoryEditor() {
        setShown(categoryEditor, true);
        focusAndReveal(categoryName);
    }

    private void hideCategoryEditor() {
        categoryName.clear();
        categoryName.pseudoClassStateChanged(INVALID, false);
        categoryName.setAccessibleHelp(null);
        setShown(categoryError, false);
        setShown(categoryEditor, false);
    }

    void showCategoryError(String message) {
        categoryError.setText(message);
        setShown(categoryError, true);
        categoryName.pseudoClassStateChanged(INVALID, true);
        categoryName.setAccessibleHelp(message);
        focusAndReveal(categoryName);
    }

    void addCategory(String name) {
        String id = "custom-" + nextCategoryId++;
        SupplyRow row = new SupplyRow(name, "Custom supply", "item", id + "-stock", id + "-price", "0", "1.00");
        row.stockField.setText("0");
        categories.add(row);
        clearUndo();
        hideCategoryEditor();
        categoriesChanged();
        focusAndReveal(row.priceField);
    }

    private void removeCategory(SupplyRow row) {
        int index = categories.indexOf(row);
        if (index < 0) { return; }
        lastRemovedIndex = index;
        lastRemoved = row;
        categories.remove(row);
        fields.remove(row.categoryId + "-stock");
        fields.remove(row.categoryId + "-price");
        fields.remove(row.categoryId + "-required");
        categoriesChanged();
        removedMessage.setText("Removed " + row.categoryName + ".");
        setShown(undoNotice, true);
        focusAndReveal(undoRemoval);
    }

    private void undoCategoryRemoval() {
        if (lastRemoved == null) { return; }
        SupplyRow row = lastRemoved;
        categories.add(Math.min(lastRemovedIndex, categories.size()), row);
        fields.put(row.categoryId + "-stock", row.stockField);
        fields.put(row.categoryId + "-price", row.priceField);
        fields.put(row.categoryId + "-required", row.requiredField);
        clearUndo();
        categoriesChanged();
        focusAndReveal(row.stockField);
    }

    private void clearUndo() {
        lastRemoved = null;
        setShown(undoNotice, false);
    }

    private void categoriesChanged() {
        clearError();
        results.showEmpty();
        updateRows();
    }

    private void updateRows() {
        if (categories.isEmpty()) {
            Label empty = label("Add a category to start your kit.", "no-categories");
            empty.setWrapText(true);
            rows.getChildren().setAll(empty);
        } else {
            rows.getChildren().setAll(categories);
        }
    }

    void loadDemoCategories(Map<String, String> values) {
        for (SupplyRow row : categories) {
            fields.remove(row.categoryId + "-stock");
            fields.remove(row.categoryId + "-price");
            fields.remove(row.categoryId + "-required");
        }
        categories.clear();
        clearUndo();
        hideCategoryEditor();
        addDefaultRows();
        values.forEach((name, value) -> {
            TextField field = fields.get(name);
            if (field != null) { field.setText(value); }
        });
        categoriesChanged();
    }

    List<PackReadyForm.CategoryInput> categoryInputs() {
        return categories.stream().map(row -> new PackReadyForm.CategoryInput(
                row.categoryId, row.categoryName, row.stockField.getText(), row.priceField.getText(),
                row.requiredField.getText())).toList();
    }

    List<String> categoryNames() { return categories.stream().map(row -> row.categoryName).toList(); }
    Button confirmCategoryButton() { return confirmCategory; }
    TextField categoryNameInput() { return categoryName; }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
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
        field.setOnAction(event -> calculate.fire());
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
            focusAndReveal(field);
        } else if (PackReadyForm.CATEGORIES.equals(name)) {
            showCategoryEditor();
            categoryName.setAccessibleHelp(message);
        }
    }

    private void focusAndReveal(Node node) {
        Platform.runLater(() -> {
            if (node.getScene() == null) { return; }
            node.requestFocus();
            applyCss();
            layout();
            Bounds visible = localToScene(getBoundsInLocal());
            Bounds field = node.localToScene(node.getBoundsInLocal());
            if (field.getMinY() < visible.getMinY() || field.getMaxY() > visible.getMaxY()) {
                Bounds content = getContent().sceneToLocal(field);
                double overflow = getContent().getLayoutBounds().getHeight() - getViewportBounds().getHeight();
                if (overflow > 0) {
                    double centered = content.getMinY() - getViewportBounds().getHeight() / 2;
                    setVvalue(Math.max(0, Math.min(1, centered / overflow)));
                }
            }
        });
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
        private final String categoryId;
        private final String categoryName;
        private final TextField stockField;
        private final TextField priceField;
        private final TextField requiredField;
        private final Button remove = new Button("\u00d7");
        private final HBox identity;
        private final VBox stock;
        private final VBox price;
        private final VBox required;
        private Boolean compact;

        SupplyRow(String name, String help, String icon, String stockName, String priceName,
                  String stockPrompt, String pricePrompt) {
            categoryId = stockName.substring(0, stockName.length() - "-stock".length());
            categoryName = name;
            setId("category-" + categoryId);
            getStyleClass().add("supply-row");
            setHgap(14);
            setVgap(12);
            Label title = label(name, "supply-name");
            title.setWrapText(true);
            Label hint = label(help, "supply-help");
            hint.setWrapText(true);
            VBox copy = new VBox(4, title, hint);
            copy.setMinWidth(0);
            identity = new HBox(10, icon(icon), copy);
            identity.setAlignment(Pos.CENTER_LEFT);
            identity.setMinWidth(0);
            HBox.setHgrow(copy, Priority.ALWAYS);
            requiredField = field(categoryId + "-required", name + " required quantity per kit", "1");
            requiredField.setText("1");
            Label requiredLabel = label("Per kit", "field-label");
            requiredLabel.setLabelFor(requiredField);
            required = new VBox(7, requiredLabel, requiredField);
            stockField = field(stockName, name + " in stock", stockPrompt);
            Label stockLabel = label("In stock", "field-label");
            stockLabel.setLabelFor(stockField);
            stock = new VBox(7, stockLabel, stockField);
            priceField = field(priceName, name + " unit price in Canadian dollars", pricePrompt);
            Label priceLabel = label("Unit price (CAD)", "field-label");
            priceLabel.setLabelFor(priceField);
            price = new VBox(7, priceLabel, moneyInput(priceField));
            remove.setId("remove-" + categoryId);
            remove.setAccessibleText("Remove " + name + " category");
            remove.setTooltip(new Tooltip("Remove " + name));
            remove.getStyleClass().add("remove-category-button");
            remove.setOnAction(event -> removeCategory(this));
            GridPane.setFillWidth(remove, false);
            GridPane.setHalignment(remove, javafx.geometry.HPos.RIGHT);
            getChildren().addAll(identity, required, stock, price, remove);
            arrange(false);
            widthProperty().addListener((observable, previous, width) -> arrange(width.doubleValue() < 510));
        }

        private void arrange(boolean small) {
            if (compact != null && compact == small) { return; }
            compact = small;
            ColumnConstraints first = new ColumnConstraints();
            ColumnConstraints second = new ColumnConstraints();
            first.setPercentWidth(28);
            second.setPercentWidth(small ? 28 : 16);
            ColumnConstraints third = new ColumnConstraints();
            third.setPercentWidth(small ? 44 : 16);
            if (small) {
                getColumnConstraints().setAll(first, second, third);
            } else {
                ColumnConstraints fourth = new ColumnConstraints();
                fourth.setPercentWidth(32);
                ColumnConstraints fifth = new ColumnConstraints();
                fifth.setPercentWidth(8);
                getColumnConstraints().setAll(first, second, third, fourth, fifth);
            }
            identity.setPadding(new Insets(0, small ? 40 : 0, 0, 0));
            GridPane.setConstraints(identity, 0, 0, small ? 3 : 1, 1);
            GridPane.setConstraints(required, small ? 0 : 1, small ? 1 : 0);
            GridPane.setConstraints(stock, small ? 1 : 2, small ? 1 : 0);
            GridPane.setConstraints(price, small ? 2 : 3, small ? 1 : 0);
            GridPane.setConstraints(remove, small ? 2 : 4, 0);
        }
    }

    static StackPane icon(String kind) {
        String path = switch (kind) {
            case "item" -> "M3 6 H19 V20 H3 Z M3 6 L11 2 L19 6 M11 6 V20 M7 10 H14";
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
