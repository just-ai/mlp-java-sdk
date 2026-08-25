#!/usr/bin/env bash
# Сборка и тесты JVM SDK до коммита.
#
# Jenkins собирает весь reactor. В POM нет закреплённой современной версии
# Surefire, поэтому Maven по умолчанию может не увидеть JUnit 5. Проверка сначала
# пакетирует reactor, затем явно запускает тесты mlp-sdk через JUnit Platform.
set -euo pipefail

repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"

if git diff --cached --quiet -- \
  '*.java' '*.kt' '*.kts' '*pom.xml' '*src/*' \
  'Jenkinsfile' 'checks/check-build.sh'; then
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
mvn -B -pl mlp-sdk test-compile org.apache.maven.plugins:maven-surefire-plugin:3.2.5:test
