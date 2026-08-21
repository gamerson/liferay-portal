#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

function main {
	local fail=0
	local pass=0

	local script

	script="$(cd "$(dirname "${0}")/.." && pwd)/setup_k3d.sh"

	_run_test "${script}" _test_aborts_with_missing_required_utility
	_run_test "${script}" _test_aborts_with_no_arguments
	_run_test "${script}" _test_aborts_with_unknown_command

	echo ""
	echo "Results: ${pass} passed, ${fail} failed."

	if [[ ${fail} -eq 0 ]]
	then
		return 0
	fi

	return 1
}

function _assert_aborts_with {
	local expected=${1}

	shift

	local exit_code
	local output
	local result

	result=$(_run_setup_k3d_test "${@}")
	exit_code=$(echo "${result}" | head -n 1)
	output=$(echo "${result}" | tail -n +2)

	if [[ "${exit_code}" -ne 0 ]] && [[ ${output} == *"${expected}"* ]]
	then
		return 0
	fi

	echo "Expected the output to contain ${expected}, got ${output}" >&2

	return 1
}

function _make_stub_path {
	local stub_dir

	stub_dir=$(mktemp --directory)

	for util in docker git go helm java jq k3d kubectl
	do
		cat > "${stub_dir}/${util}" << 'EOF'
#!/usr/bin/env bash
exit 0
EOF

		chmod +x "${stub_dir}/${util}"
	done

	for util in basename bash cat dirname env
	do
		local real_path

		real_path=$(command -v "${util}" 2> /dev/null || true)

		if [[ -n ${real_path} ]]
		then
			ln --symbolic "${real_path}" "${stub_dir}/${util}"
		fi
	done

	echo "${stub_dir}"
}

function _run_setup_k3d_test {
	local script=${1}
	local utility_to_remove=${2}

	shift 2

	local stub_dir

	stub_dir=$(_make_stub_path)

	if [[ -n ${utility_to_remove} ]]
	then
		rm "${stub_dir}/${utility_to_remove}"
	fi

	local exit_code=0
	local output

	output=$(PATH="${stub_dir}" bash "${script}" "${@}" 2>&1) || exit_code="${?}"

	rm --force --recursive "${stub_dir}"

	echo "${exit_code}"
	echo "${output}"
}

function _run_test {
	local script=${1}
	local test_function=${2}

	local script_name

	script_name=$(basename "${script}")

	local description

	description=$(echo "${test_function}" | sed --expression "s/^_test_//" --expression "s/_/ /g")

	local exit_code=0

	"${test_function}" "${script}" || exit_code="${?}"

	if [[ ${exit_code} -eq 0 ]]
	then
		echo "PASS: [${script_name}] ${description}."

		pass=$((pass + 1))
	else
		echo "FAIL: [${script_name}] ${description}."

		fail=$((fail + 1))
	fi
}

function _test_aborts_with_missing_required_utility {
	_assert_aborts_with "The utility k3d is not installed." "${1}" k3d down
}

function _test_aborts_with_no_arguments {
	_assert_aborts_with "Usage:" "${1}" ""
}

function _test_aborts_with_unknown_command {
	_assert_aborts_with "Usage:" "${1}" "" not-a-command
}

main "${@}"
