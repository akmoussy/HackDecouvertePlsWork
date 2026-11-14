package org.example.demo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Connects the JavaFX form to the independently tested Java logic. */
final class PackReadyController {
    private final PackReadyView view;

    PackReadyController(PackReadyView view) {
        this.view = view;
        view.calculateButton().setOnAction(event -> calculate());
        view.demoButton().setOnAction(event -> loadDemo());
        view.fields().values().forEach(field -> field.setOnAction(event -> calculate()));
    }

    private void loadDemo() {
        PackReadyForm.demoFields().forEach((name, text) -> view.fields().get(name).setText(text));
        calculate();
    }

    private void calculate() {
        view.clearError();
        Map<String, String> raw = new LinkedHashMap<>();
        view.fields().forEach((name, field) -> raw.put(name, field.getText()));

        try {
            PackReadyForm.Request request = PackReadyForm.parse(raw);
            List<Planner.Item> items = new ArrayList<>();
            for (PackReadyForm.Item item : request.getItems()) {
                items.add(new Planner.Item(item.getName(), item.getStock(), item.getUnitPriceCents()));
            }
            Planner.Plan plan = Planner.planPurchase(items, request.getBudgetCents());
            // Do not display a negative kit count from the known upstream int overflow.
            if (plan.plannedKits() < plan.currentKits()
                    || plan.totalCents() < 0 || plan.remainingCents() < 0) {
                view.showError(PackReadyForm.BACKPACK_STOCK,
                        "These values are too large to calculate safely. Try smaller stock counts or a smaller budget.");
                return;
            }
            view.results().showPlan(plan);
        } catch (PackReadyForm.FieldValidationException exception) {
            view.showError(exception.getFieldName(), exception.getMessage());
        }
    }
}
