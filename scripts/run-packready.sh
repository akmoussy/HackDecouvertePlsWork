#!/bin/sh
set -eu
. "$(dirname "$0")/javafx-common.sh"
exec java --enable-native-access=javafx.graphics \
  --module-path "$packready_fx_cache" --add-modules javafx.controls,javafx.fxml \
  -cp "$packready_ui_build" org.example.demo.Launcher
