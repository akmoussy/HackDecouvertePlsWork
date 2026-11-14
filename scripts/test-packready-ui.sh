#!/bin/sh
set -eu
. "$(dirname "$0")/javafx-common.sh"
javac --release 17 -encoding UTF-8 -Xlint:all -Werror \
  --module-path "$packready_fx_cache" --add-modules javafx.controls,javafx.fxml \
  -cp "$packready_ui_build" -d "$packready_ui_build" \
  "$packready_repo_dir/demo/src/uiTest/java/org/example/demo/PackReadyUiTest.java"
exec java --enable-native-access=javafx.graphics \
  --module-path "$packready_fx_cache" --add-modules javafx.controls,javafx.fxml \
  -Dpackready.screenshots=/tmp/packready-ui-shots \
  -cp "$packready_ui_build" org.example.demo.PackReadyUiTest
