#!/usr/bin/env bash
# Обязательный секрет-скан staged-изменений компонента.
set -euo pipefail

if ! command -v gitleaks >/dev/null 2>&1; then
  echo "[secrets] gitleaks не установлен. Поставь его (например: brew install gitleaks) — секрет-скан обязателен." >&2
  exit 1
fi

gitleaks protect --staged --redact --verbose
