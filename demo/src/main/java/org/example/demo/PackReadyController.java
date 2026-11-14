package org.example.demo;

import java.util.ArrayList;
import java.util.List;

/** Connects the JavaFX form to the independently tested Java logic. */
final class PackReadyController {
    private final PackReadyView view;

    PackReadyController(PackReadyView view) {
        this.view = view;
        view.calculateButton().setOnAction(event -> calculate());
        view.demoButton().setOnAction(event -> loadDemo());
        view.confirmCategoryButton().setOnAction(event -> addCategory());
    }

    private void loadDemo() {
        view.loadDemoCategories();
        calculate();
    }

    private void addCategory() {
        try {
            String name = PackReadyForm.parseCategoryName(view.categoryNameInput().getText(), view.categoryNames());
            view.addCategory(name);
        } catch (IllegalArgumentException exception) {
            view.showCategoryError(exception.getMessage());
        }
    }

    private void calculate() {
        view.clearError();
        try {
            PackReadyForm.Request request = PackReadyForm.parseCategories(
                    view.categoryInputs(), view.fields().get(PackReadyForm.BUDGET).getText());
            List<Planner.Item> items = new ArrayList<>();
            for (PackReadyForm.Item item : request.getItems()) {
                items.add(new Planner.Item(item.getName(), item.getStock(), item.getUnitPriceCents()));
            }
            Planner.Plan plan = Planner.planPurchase(items, request.getBudgetCents());
            // Do not display a negative kit count from the known upstream int overflow.
            if (plan.plannedKits() < plan.currentKits()
                    || plan.totalCents() < 0 || plan.remainingCents() < 0) {
                view.showError(view.categoryInputs().get(0).id() + "-stock",
                        "These values are too large to calculate safely. Try smaller stock counts or a smaller budget.");
                return;
            }
            view.results().showPlan(plan);
        } catch (PackReadyForm.FieldValidationException exception) {
            view.showError(exception.getFieldName(), exception.getMessage());
        }
    }
}
