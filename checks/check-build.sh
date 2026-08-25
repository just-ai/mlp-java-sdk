#!/usr/bin/env bash
# Сборка и тесты legacy SDK до коммита.
#
# Jenkins собирает весь reactor и запускает Maven Surefire 2.12.4. В ветке
# CAILA-5168 есть JUnit5-тесты без JUnit Platform provider: современный локальный
# Maven подхватывает их другим Surefire и получает известные baseline-падения, а
# Jenkins исполняет JUnit4-контур. Поэтому проверка явно фиксирует серверный
# runner: сначала пакетирует весь reactor, затем запускает все обнаруживаемые
# JUnit4-тесты mlp-sdk на Surefire 2.12.4.
set -euo pipefail

repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"

if git diff --cached --quiet -- +  '*.java' '*.kt' '*.kts' '*pom.xml' '*src/*' +  'Jenkinsfile' 'checks/check-build.sh'; then
  exit 0
fi

if ! command -v java >/dev/null 2>&1; then
  echo "[build] не найден JDK: поставь JDK или задай JAVA_HOME — иначе сборку не проверить." >&2
  exit 1
fi
if ! command -v mvn >/dev/null 2>&1; then
  echo "[build] не найден Maven: поставь Maven — в репозитории нет mvnw." >&2
  exit 1
fi

mvn -B -DskipTests package
mvn -B -pl mlp-sdk test-compile org.apache.maven.plugins:maven-surefire-plugin:2.12.4:test
