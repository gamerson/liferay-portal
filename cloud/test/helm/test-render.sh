#!/usr/bin/env bash
set -o errexit
set -o nounset
set -o pipefail

UPDATE=0

if [[ "${1:-}" == "--update" ]]; then
	UPDATE=1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLOUD_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
CASES_FILE="$SCRIPT_DIR/cases.yaml"
EXPECTED_DIR="$SCRIPT_DIR/expected"

mkdir -p "$EXPECTED_DIR"

for tool in helm yq diff; do
	if ! command -v "$tool" >/dev/null; then
		echo "ERROR: required tool '$tool' not found on PATH" >&2
		exit 2
	fi
done

api_args=()

while IFS= read -r v; do
	api_args+=(--api-versions "$v")
done < <(yq '.defaults.api_versions[]' "$CASES_FILE")

charts_with_deps=()

while IFS= read -r chart; do
	if [[ -f "$CLOUD_DIR/$chart/Chart.yaml" ]] && grep -q "^dependencies:" "$CLOUD_DIR/$chart/Chart.yaml"; then
		if grep -q "repository: file://\.\." "$CLOUD_DIR/$chart/Chart.yaml"; then
			charts_with_deps+=("$chart")
		fi
	fi
done < <(yq -r '.cases[].chart' "$CASES_FILE" | sort -u)

for chart in "${charts_with_deps[@]}"; do
	echo "==> helm dependency update $chart"
	helm dependency update "$CLOUD_DIR/$chart" >/dev/null
done

n_cases=$(yq '.cases | length' "$CASES_FILE")
fail=0

for i in $(seq 0 $((n_cases - 1))); do
	name=$(yq -r ".cases[$i].name" "$CASES_FILE")
	chart=$(yq -r ".cases[$i].chart" "$CASES_FILE")
	release=$(yq -r ".cases[$i].release_name" "$CASES_FILE")
	namespace=$(yq -r ".cases[$i].namespace" "$CASES_FILE")

	value_args=()

	while IFS= read -r vf; do
		value_args+=(-f "$CLOUD_DIR/$vf")
	done < <(yq -r ".cases[$i].value_files[]" "$CASES_FILE")

	set_args=()

	while IFS= read -r entry; do
		set_args+=(--set-string "$entry")
	done < <(yq -r ".cases[$i].helm_params | to_entries | .[] | .key + \"=\" + (.value|tostring)" "$CASES_FILE")

	expected_file="$EXPECTED_DIR/$name.yaml"

	rendered=$(helm template "$release" "$CLOUD_DIR/$chart" \
		--namespace "$namespace" \
		"${api_args[@]}" \
		"${value_args[@]}" \
		"${set_args[@]}" \
		| sed -E 's|^([[:space:]]*LIFERAY_DEFAULT_PERIOD_ADMIN_PERIOD_PASSWORD: ").*(")$|\1<REDACTED>\2|')

	if (( UPDATE )); then
		printf '%s\n' "$rendered" > "$expected_file"
		echo "updated $name"
	else
		if [[ ! -f "$expected_file" ]]; then
			echo "FAIL  $name (no expected file at $expected_file; run with --update)"
			fail=1
			continue
		fi

		if diff -u "$expected_file" <(printf '%s\n' "$rendered") >/dev/null; then
			echo "ok    $name"
		else
			echo "FAIL  $name"
			diff -u "$expected_file" <(printf '%s\n' "$rendered") | head -80
			fail=1
		fi
	fi
done

exit "$fail"
