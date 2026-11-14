#!/bin/sh
# Sourced by the launch and UI-check scripts; never changes JAVA_HOME.
set -eu

packready_repo_dir=$(CDPATH= cd "$(dirname "$0")/.." && pwd)
packready_fx_version=21.0.6
case "$(uname -s)/$(uname -m)" in
  Darwin/arm64) packready_fx_platform=mac-aarch64 ;;
  Darwin/x86_64) packready_fx_platform=mac ;;
  Linux/aarch64) packready_fx_platform=linux-aarch64 ;;
  Linux/x86_64) packready_fx_platform=linux ;;
  MINGW*/x86_64|MSYS*/x86_64) packready_fx_platform=win ;;
  *) printf 'Use the Gradle project for this platform.\n' >&2; exit 1 ;;
esac
packready_fx_cache="/tmp/packready-javafx-$packready_fx_version-$packready_fx_platform"
mkdir -p "$packready_fx_cache"

for packready_fx_module in javafx-base javafx-graphics javafx-controls javafx-fxml
do
  packready_fx_name="$packready_fx_module-$packready_fx_version-$packready_fx_platform.jar"
  packready_fx_jar="$packready_fx_cache/$packready_fx_name"
  if [ ! -s "$packready_fx_jar" ]; then
    packready_maven_jar="$HOME/.m2/repository/org/openjfx/$packready_fx_module/$packready_fx_version/$packready_fx_name"
    if [ -s "$packready_maven_jar" ]; then
      cp "$packready_maven_jar" "$packready_fx_jar"
    else
      printf 'Downloading %s\n' "$packready_fx_name"
      curl --fail --location --silent --show-error \
        --output "$packready_fx_jar.part" \
        "https://repo.maven.apache.org/maven2/org/openjfx/$packready_fx_module/$packready_fx_version/$packready_fx_name"
      mv "$packready_fx_jar.part" "$packready_fx_jar"
    fi
  fi
done

packready_ui_build=$(mktemp -d /tmp/packready-ui.XXXXXX)
javac --release 17 -encoding UTF-8 -Xlint:all -Werror \
  --module-path "$packready_fx_cache" --add-modules javafx.controls,javafx.fxml \
  -d "$packready_ui_build" \
  "$packready_repo_dir"/demo/src/main/java/org/example/demo/*.java
cp -R "$packready_repo_dir/demo/src/main/resources/." "$packready_ui_build/"
printf 'JavaFX classes: %s\n' "$packready_ui_build"
