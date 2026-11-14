package org.example.demo;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/** Live JavaFX acceptance checks. Opens a window briefly and saves QA screenshots. */
public final class PackReadyUiTest {
    private static volatile Throwable failure;

    public static void main(String[] args) {
        Application.launch(UiApplication.class, args);
        if (failure != null) {
            throw new AssertionError("JavaFX acceptance checks failed", failure);
        }
    }

    public static final class UiApplication extends Application {
        private final List<Runnable> steps = new ArrayList<>();
        private int index;
        private int checks;
        private Stage stage;
        private PackReadyView view;
        private String customId;

        @Override
        public void start(Stage primaryStage) {
            stage = primaryStage;
            view = new PackReadyView();
            new PackReadyController(view);
            stage.setTitle("PackReady UI checks");
            stage.setScene(new Scene(view, 1120, 820));
            stage.setMinWidth(420);
            stage.setMinHeight(600);
            stage.show();

            steps.add(() -> {
                check("all seven raw fields are present", view.fields().size() == 7);
                check("Calculate is the default button", view.calculateButton().isDefaultButton());
                check("empty form starts with guidance", view.lookup("#current-kits") == null);
                for (TextField field : view.fields().values()) {
                    boolean labeled = view.lookupAll(".label").stream()
                            .filter(node -> node instanceof Label)
                            .map(node -> (Label) node).anyMatch(label -> label.getLabelFor() == field);
                    check(field.getId() + " has a visible label and accessible name",
                            labeled && field.getAccessibleText() != null);
                }
                screenshot("initial-desktop");
                view.demoButton().fire();
            });
            steps.add(() -> {
                expectPlan("$30 demo", "8", "20", "CAD $29.00", "CAD $1.00");
                check("shopping quantities are visible", view.results().getAccessibleText().contains("5 Notebooks")
                        && view.results().getAccessibleText().contains("12 Pencil cases"));
                screenshot("desktop-plan");
                input("budget").setText("32");
                // TextField's Enter action uses this same ActionEvent handler.
                input("budget").fireEvent(new ActionEvent());
            });
            steps.add(() -> {
                expectPlan("Enter action with $32", "8", "21", "CAD $32.00", "CAD $0.00");
                input("budget").setText("0");
                view.calculateButton().fire();
            });
            steps.add(() -> {
                expectPlan("$0 calculation", "8", "8", "CAD $0.00", "CAD $0.00");
                check("empty result has no stale purchases", view.results().getAccessibleText().contains("can\u2019t complete")
                        && !view.results().getAccessibleText().contains("Notebooks:"));
                view.demoButton().fire();
                input("notebook-stock").clear();
                view.calculateButton().fire();
            });
            steps.add(() -> {
                expectError("blank stock", "notebook-stock", "required");
                view.demoButton().fire();
                input("backpack-price").setText("12.001");
                view.calculateButton().fire();
            });
            steps.add(() -> {
                expectError("extra decimal place", "backpack-price", "two decimal places");
                view.demoButton().fire();
                input("pencil-case-price").setText("0");
                view.calculateButton().fire();
            });
            steps.add(() -> {
                expectError("zero unit price", "pencil-case-price", "greater than zero");
                view.demoButton().fire();
                input("backpack-stock").setText("2147483647");
                input("notebook-stock").setText("2147483647");
                input("pencil-case-stock").setText("2147483647");
                input("budget").setText("15");
                view.calculateButton().fire();
            });
            steps.add(() -> {
                expectError("upstream overflow never displays negative kits", "backpack-stock", "too large");
                stage.setWidth(600);
                stage.setHeight(850);
                view.demoButton().fire();
                view.setVvalue(0);
            });
            steps.add(() -> {
                check("narrow window stacks the results below the form", Integer.valueOf(1)
                        .equals(GridPane.getRowIndex(view.results())));
                Bounds form = view.lookup("#planner-form").localToScene(
                        view.lookup("#planner-form").getBoundsInLocal());
                for (TextField field : view.fields().values()) {
                    Bounds bounds = field.localToScene(field.getBoundsInLocal());
                    check(field.getId() + " fits the narrow form", bounds.getMinX() >= form.getMinX()
                            && bounds.getMaxX() <= form.getMaxX());
                }
                screenshot("narrow-plan");
                input("budget").clear();
                view.calculateButton().fire();
            });
            steps.add(() -> {
                expectError("narrow budget error", "budget", "required");
                Bounds field = input("budget").localToScene(input("budget").getBoundsInLocal());
                Bounds viewport = view.localToScene(view.getBoundsInLocal());
                check("narrow view scrolls the invalid field into view",
                        field.getMinY() >= viewport.getMinY() && field.getMaxY() <= viewport.getMaxY());
                screenshot("narrow-error");
                stage.setWidth(420);
                stage.setHeight(850);
                view.demoButton().fire();
                view.setVvalue(0);
            });
            steps.add(() -> {
                Bounds form = view.lookup("#planner-form").localToScene(
                        view.lookup("#planner-form").getBoundsInLocal());
                for (TextField field : view.fields().values()) {
                    Bounds bounds = field.localToScene(field.getBoundsInLocal());
                    check(field.getId() + " fits the 420px minimum window",
                            bounds.getMinX() >= form.getMinX() && bounds.getMaxX() <= form.getMaxX());
                }
                screenshot("minimum-window");
                stage.setWidth(1120);
                stage.setHeight(850);
                view.demoButton().fire();
                input("backpack-stock").setText("0");
                view.calculateButton().fire();
            });
            steps.add(() -> {
                expectPlan("zero stock", "0", "2", "CAD $24.00", "CAD $6.00");
                check("success clears validation errors", !view.lookup("#form-error").isVisible());
                view.demoButton().fire();
                view.setVvalue(0);
            });
            steps.add(() -> {
                fire("add-category");
                view.confirmCategoryButton().fire();
            });
            steps.add(() -> {
                check("blank category name is rejected", ((Label) view.lookup("#category-error")).getText().contains("required"));
                check("invalid add preserves the existing categories", view.categoryInputs().size() == 3);
                view.categoryNameInput().setText(" Water bottles ");
                view.categoryNameInput().fireEvent(new ActionEvent());
            });
            steps.add(() -> {
                check("Enter adds a named category", view.categoryInputs().size() == 4
                        && view.categoryNames().get(3).equals("Water bottles"));
                customId = view.categoryInputs().get(3).id();
                check("added category gets stock and price controls", view.fields().size() == 9
                        && input(customId + "-stock").getText().equals("0"));
                check("adding a category clears the previous result", view.lookup("#current-kits") == null);
                check("adding focuses its required price", view.getScene().getFocusOwner() == input(customId + "-price"));
                input(customId + "-price").setText("0.50");
                input(customId + "-price").fireEvent(new ActionEvent());
            });
            steps.add(() -> {
                expectPlan("four categories participate in the kit recipe", "0", "17", "CAD $28.50", "CAD $1.50");
                check("custom purchase is displayed", view.results().getAccessibleText().contains("17 Water bottles"));
                screenshot("categories-desktop");
                fire("add-category");
                view.categoryNameInput().setText("water bottles");
                view.confirmCategoryButton().fire();
            });
            steps.add(() -> {
                check("duplicate category names are rejected", view.categoryInputs().size() == 4
                        && ((Label) view.lookup("#category-error")).getText().contains("already exists"));
                check("name errors are accessible", view.categoryNameInput().getAccessibleHelp().contains("already exists"));
                fire("cancel-add-category");
                Button remove = (Button) view.lookup("#remove-" + customId);
                remove.fire();
                // A queued second click must not corrupt the saved Undo position.
                remove.fire();
            });
            steps.add(() -> {
                check("remove deletes only that category and its fields", view.categoryInputs().size() == 3
                        && !view.fields().containsKey(customId + "-stock") && view.fields().size() == 7);
                check("remove clears stale results", view.lookup("#current-kits") == null);
                check("remove offers Undo", view.lookup("#undo-remove-category").isVisible());
                fire("undo-remove-category");
            });
            steps.add(() -> {
                check("Undo restores category order", view.categoryNames().get(3).equals("Water bottles"));
                check("Undo restores entered values", input(customId + "-stock").getText().equals("0")
                        && input(customId + "-price").getText().equals("0.50") && input("budget").getText().equals("30"));
                view.calculateButton().fire();
            });
            steps.add(() -> {
                expectPlan("Undo restores the original four-category calculation", "0", "17", "CAD $28.50", "CAD $1.50");
                input(customId + "-price").clear();
                fire("remove-" + customId);
                view.calculateButton().fire();
            });
            steps.add(() -> {
                expectPlan("removed invalid fields no longer block calculation", "8", "20", "CAD $29.00", "CAD $1.00");
                fire("remove-pencil-case");
                view.calculateButton().fire();
            });
            steps.add(() -> {
                expectPlan("default categories can be removed too", "15", "26", "CAD $23.00", "CAD $7.00");
                fire("undo-remove-category");
                view.calculateButton().fire();
            });
            steps.add(() -> {
                expectPlan("Undo restores a default category", "8", "20", "CAD $29.00", "CAD $1.00");
                for (PackReadyForm.CategoryInput category : view.categoryInputs()) {
                    fire("remove-" + category.id());
                }
                view.calculateButton().fire();
            });
            steps.add(() -> {
                check("empty category list gets a helpful validation message", view.categoryInputs().isEmpty()
                        && ((Label) view.lookup("#form-error")).getText().contains("at least one"));
                check("empty category list focuses the add editor", view.getScene().getFocusOwner() == view.categoryNameInput());
                check("no orphaned fields remain after removing all categories", view.fields().size() == 1);
                fire("undo-remove-category");
                view.calculateButton().fire();
            });
            steps.add(() -> {
                expectPlan("a single restored category can form kits", "8", "23", "CAD $30.00", "CAD $0.00");
                view.demoButton().fire();
            });
            steps.add(() -> {
                expectPlan("Load demo resets categories and runs the shared example", "8", "20", "CAD $29.00", "CAD $1.00");
                check("demo restores exactly the three original categories", view.categoryInputs().size() == 3 && view.fields().size() == 7);
                fire("add-category");
                view.categoryNameInput().setText("Water bottles");
                view.confirmCategoryButton().fire();
                customId = view.categoryInputs().get(3).id();
                input(customId + "-price").setText("0.50");
                view.calculateButton().fire();
                stage.setWidth(420);
                view.setVvalue(0);
            });
            steps.add(() -> {
                Bounds form = view.lookup("#planner-form").localToScene(view.lookup("#planner-form").getBoundsInLocal());
                for (TextField field : view.fields().values()) {
                    Bounds bounds = field.localToScene(field.getBoundsInLocal());
                    check(field.getId() + " fits the editable-category minimum layout",
                            bounds.getMinX() >= form.getMinX() && bounds.getMaxX() <= form.getMaxX());
                }
                screenshot("categories-minimum");
                fire("add-category");
            });
            steps.add(() -> {
                Bounds form = view.lookup("#planner-form").localToScene(view.lookup("#planner-form").getBoundsInLocal());
                Bounds entry = view.categoryNameInput().localToScene(view.categoryNameInput().getBoundsInLocal());
                check("add editor fits the minimum window", entry.getMinX() >= form.getMinX() && entry.getMaxX() <= form.getMaxX());
                screenshot("category-editor-minimum");
                fire("cancel-add-category");
                stage.setWidth(1120);
                view.setVvalue(0);
            });
            steps.add(() -> screenshot("final-desktop"));
            next();
        }

        private void next() {
            PauseTransition pause = new PauseTransition(Duration.millis(220));
            pause.setOnFinished(event -> {
                try {
                    if (index == steps.size()) {
                        System.out.println("All " + checks + " JavaFX checks passed.");
                        stage.close();
                        Platform.exit();
                        return;
                    }
                    steps.get(index++).run();
                    next();
                } catch (Throwable exception) {
                    failure = exception;
                    exception.printStackTrace();
                    stage.close();
                    Platform.exit();
                }
            });
            pause.play();
        }

        private TextField input(String id) { return view.fields().get(id); }
        private void fire(String id) { ((Button) view.lookup("#" + id)).fire(); }

        private void expectPlan(String label, String current, String planned, String total, String left) {
            check(label, current.equals(((Label) view.lookup("#current-kits")).getText())
                    && planned.equals(((Label) view.lookup("#planned-kits")).getText())
                    && total.equals(((Label) view.lookup("#total-spent")).getText())
                    && left.equals(((Label) view.lookup("#budget-left")).getText()));
        }

        private void expectError(String label, String field, String message) {
            Label error = (Label) view.lookup("#form-error");
            check(label + " shows a specific error", error.isVisible() && error.getText().contains(message));
            check(label + " focuses the first invalid field", view.getScene().getFocusOwner() == input(field));
            check(label + " associates the error with its field for accessibility",
                    input(field).getAccessibleHelp().contains(message));
            check(label + " clears old results", view.lookup("#current-kits") == null);
        }

        private void check(String label, boolean passed) {
            if (!passed) { throw new AssertionError(label); }
            checks++;
            System.out.println("PASS " + label);
        }

        private void screenshot(String name) {
            try {
                Path folder = Path.of(System.getProperty("packready.screenshots", "/tmp/packready-ui-shots"));
                Files.createDirectories(folder);
                Node canvas = view.getContent();
                WritableImage image = canvas.snapshot(null, null);
                int width = (int) image.getWidth();
                int height = (int) image.getHeight();
                int[] pixels = new int[width * height];
                image.getPixelReader().getPixels(0, 0, width, height,
                        PixelFormat.getIntArgbInstance(), pixels, 0, width);
                BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                output.setRGB(0, 0, width, height, pixels, 0, width);
                ImageIO.write(output, "png", folder.resolve(name + ".png").toFile());
                System.out.println("SCREENSHOT " + folder.resolve(name + ".png"));
            } catch (IOException exception) {
                throw new IllegalStateException("Could not save UI screenshot", exception);
            }
        }
    }
}
