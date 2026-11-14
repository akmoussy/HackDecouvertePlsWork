#!/bin/sh
set -eu

packready_repo_dir=$(CDPATH= cd "$(dirname "$0")/.." && pwd)
packready_test_dir=$(mktemp -d /tmp/packready-tests.XXXXXX)

printf 'PackReady test classes: %s\n' "$packready_test_dir"

# Compile the plain Java code explicitly, without starting JavaFX or Gradle.
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d "$packready_test_dir" \
  "$packready_repo_dir/demo/src/main/java/org/example/demo/PackReadyInputs.java" \
  "$packready_repo_dir/demo/src/main/java/org/example/demo/PackReadyForm.java" \
  "$packready_repo_dir/demo/src/main/java/org/example/demo/Planner.java" \
  "$packready_repo_dir/demo/src/main/java/org/example/demo/PackReadyResults.java" \
  "$packready_repo_dir/demo/src/test/java/org/example/demo/PackReadyInputsTest.java" \
  "$packready_repo_dir/demo/src/test/java/org/example/demo/PackReadyFormTest.java" \
  "$packready_repo_dir/demo/src/test/java/org/example/demo/PlannerTest.java" \
  "$packready_repo_dir/demo/src/test/java/org/example/demo/PackReadyResultsTest.java" \
  "$packready_repo_dir/demo/src/test/java/org/example/demo/PackReadyFlowTest.java" \
  "$packready_repo_dir/demo/src/test/java/org/example/demo/PackReadyCategoriesTest.java"

for packready_test in PackReadyInputsTest PackReadyFormTest PlannerTest PackReadyResultsTest PackReadyFlowTest PackReadyCategoriesTest
do
  java -cp "$packready_test_dir" "org.example.demo.$packready_test"
done
