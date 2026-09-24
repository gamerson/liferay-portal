#!/bin/bash

# Packages a built client extension as a versioned Helm chart and pushes it to
# an OCI registry, which is what a CI pipeline would do after building the zip.
#
# package-cx-chart.sh <sample> [version] [oci-repository]
#
# The zip a Liferay Workspace build already produces carries everything the
# deployment needs: *.client-extension-config.json is the configuration payload,
# and LCP.json describes the workload shape. This bakes both into the chart's
# defaults, so what reaches the cluster is the artifact the build produced and
# the only thing left to supply per environment is the binding -- which Liferay,
# which host, which image.
#
# Helm renders before anything runs and cannot fetch or unpack a zip, so the
# artifact has to be materialised at package time. Baking it into values keeps
# the rendering hermetic: Argo CD can still diff and dry run what it will apply.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

CHART_DIR="$(cd "${HACK_DIR}/../../helm/client-extension" && pwd)"

OCI_PLAIN_HTTP="${OCI_PLAIN_HTTP:-}"

OCI_REPOSITORY="${OCI_REPOSITORY:-oci://localhost:5000/charts}"

WORK_DIR="${BUILD_DIR:-/tmp/cx-spike-images}/charts"

function main {
	local sample=${1:?usage: package-cx-chart.sh <sample> [version] [oci-repository]}
	local version=${2:-0.1.0}
	local repository=${3:-${OCI_REPOSITORY}}

	log_step "Packaging ${sample} ${version}"

	local staging="${WORK_DIR}/${sample}"

	rm --force --recursive "${staging}"
	mkdir --parents "${staging}"

	cp --recursive "${CHART_DIR}/." "${staging}/"

	_extract "${sample}" "${staging}/artifact"
	_bake "${sample}" "${staging}"

	python3 -c '
import sys, yaml

path, name, version = sys.argv[1:]
chart = yaml.safe_load(open(path))
chart["name"] = name
chart["version"] = version
yaml.safe_dump(chart, open(path, "w"), default_flow_style=False)
' "${staging}/Chart.yaml" "${sample}" "${version}"

	rm --force --recursive "${staging}/artifact"

	helm package "${staging}" --destination "${WORK_DIR}" > /dev/null

	log "Packaged ${WORK_DIR}/${sample}-${version}.tgz"

	local -a push=(helm push "${WORK_DIR}/${sample}-${version}.tgz" "${repository}")

	if [ -n "${OCI_PLAIN_HTTP}" ]
	then
		push+=(--plain-http)
	fi

	"${push[@]}" 2>&1 | sed 's/^/    /'

	log "Pushed ${repository}/${sample}:${version}"
}

# _bake writes the artifact into the chart's defaults. Everything set here comes
# from the build; nothing here is environment specific.
function _bake {
	local sample=${1}
	local staging=${2}

	python3 -c '
import glob, json, sys, yaml

sample, staging = sys.argv[1:]

lcp = json.load(open(staging + "/artifact/LCP.json"))

configs = [
    open(path).read()
    for path in sorted(glob.glob(staging + "/artifact/*.client-extension-config.json"))
]

values = yaml.safe_load(open(staging + "/values.yaml"))

values["clientExtension"]["configs"] = configs
values["clientExtension"]["serviceId"] = sample
values["fullnameOverride"] = sample

workload = values["workload"]
workload["containerPort"] = lcp.get("loadBalancer", {}).get("targetPort", 80)
workload["kind"] = lcp.get("kind", "Deployment")
workload["replicas"] = lcp.get("scale", 1)

for key in ("livenessProbe", "readinessProbe"):
    if lcp.get(key):
        workload[key] = lcp[key]

if lcp.get("schedule"):
    workload["schedule"] = lcp["schedule"]

if lcp.get("env"):
    workload.setdefault("env", {}).update(
        {k: str(v) for k, v in lcp["env"].items()}
    )

yaml.safe_dump(values, open(staging + "/values.yaml", "w"), default_flow_style=False)

print("    kind=%s replicas=%s port=%s configs=%d" % (
    workload["kind"], workload["replicas"], workload["containerPort"], len(configs)
))
' "${sample}" "${staging}"
}

function _extract {
	local sample=${1}
	local destination=${2}

	mkdir --parents "${destination}"

	unzip -o -q "${SAMPLES_DIR}/${sample}/dist/${sample}.zip" \
		'LCP.json' '*client-extension-config.json' -d "${destination}"
}

main "${@}"
