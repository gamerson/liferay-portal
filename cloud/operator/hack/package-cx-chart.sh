#!/bin/bash

# Publishes a built client extension to an OCI registry as two artifacts, which
# is what a CI pipeline would do after building the zip.
#
# package-cx-chart.sh <sample> [version]
#
# <registry>/cx/<sample> the zip's files, as an image to mount
# <registry>/charts/<sample> a chart that runs them
#
# Nothing is built from the zip's Dockerfile. It is read instead: its FROM names
# the stock base image the workload runs, and its COPY lines say where the files
# are mounted, so the chart can pair an unmodified liferay/caddy or
# liferay/jar-runner with the extension's own files through an image volume.
#
# The chart has everything baked into its defaults -- the configuration payload,
# the workload shape from LCP.json, the base image, and the artifact pinned by
# digest -- so the only values left to supply per environment are the binding:
# which Liferay, which virtual instance, which host.
#
# Helm renders before anything runs and cannot fetch or unpack a zip, so the
# zip has to be materialised at package time. Baking it into values keeps the
# rendering hermetic, which is what lets Argo CD diff and dry run what it will
# apply.

set -o errexit
set -o nounset
set -o pipefail

HACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${HACK_DIR}/lib.sh"

CHART_DIR="$(cd "${HACK_DIR}/../../helm/client-extension" && pwd)"

OCI_PLAIN_HTTP="${OCI_PLAIN_HTTP:-true}"

WORK_DIR="${BUILD_DIR:-/tmp/cx-spike-images}/charts"

function main {
	local sample=${1:?usage: package-cx-chart.sh <sample> [version]}
	local version=${2:-0.1.0}

	local zip="${SAMPLES_DIR}/${sample}/dist/${sample}.zip"
	local stamp="${WORK_DIR}/${sample}-${version}.stamp"

	mkdir --parents "${WORK_DIR}"

	# Rebuilding unchanged inputs would push identical bytes; skip it so a
	# scenario that deploys the same sample into several namespaces publishes
	# it once. The inputs are the zip and everything that turns it into the
	# published chart, so a fix to the chart or to this packaging republishes
	# even when the zip has not changed.
	local zip_digest
	zip_digest=$(
		{
			sha256sum "${zip}" "${HACK_DIR}/cx_artifact.py" "${BASH_SOURCE[0]}"
			find "${CHART_DIR}" -type f -print0 | sort --zero-terminated | xargs --null sha256sum
		} | cut --delimiter " " --fields 1 | sha256sum | cut --delimiter " " --fields 1
	)

	if [ -f "${stamp}" ] && [ "$(cat "${stamp}")" == "${zip_digest}" ]
	then
		log "${sample} ${version} already published"

		return
	fi

	local staging="${WORK_DIR}/${sample}"

	rm --force --recursive "${staging}"
	mkdir --parents "${staging}/artifact" "${staging}/layout"

	python3 "${HACK_DIR}/cx_artifact.py" "${zip}" "${staging}/layout" "${version}" \
		> "${staging}/artifact.json"

	_replay "${staging}" "${version}"

	_push_artifact "${sample}" "${version}" "${staging}"

	cp --recursive "${CHART_DIR}/." "${staging}/chart/"

	unzip -o -q "${zip}" 'LCP.json' '*client-extension-config.json' -d "${staging}/artifact"

	_bake "${sample}" "${version}" "${staging}"

	helm package "${staging}/chart" --destination "${WORK_DIR}" > /dev/null

	local -a push=(helm push "${WORK_DIR}/${sample}-${version}.tgz" "oci://${OCI_PUSH_HOST}/charts")

	if [ "${OCI_PLAIN_HTTP}" == "true" ]
	then
		push+=(--plain-http)
	fi

	"${push[@]}" > /dev/null 2>&1

	echo "${zip_digest}" > "${stamp}"

	log "Published ${sample} ${version}"
}

# _bake writes the artifact into the chart's defaults. Everything set here comes
# from the build; nothing here is environment specific.
function _bake {
	local sample=${1}
	local version=${2}
	local staging=${3}

	python3 -c '
import glob, json, sys, yaml

sample, version, staging, reference = sys.argv[1:]

artifact = json.load(open(staging + "/artifact.json"))
lcp = json.load(open(staging + "/artifact/LCP.json"))
configs = [
	open(path).read()
	for path in sorted(glob.glob(staging + "/artifact/*client-extension-config.json"))
]

values = yaml.safe_load(open(staging + "/chart/values.yaml"))

values["artifact"]["mounts"] = artifact["mounts"]
values["artifact"]["reference"] = reference if artifact["mounts"] else ""

values["clientExtension"]["configs"] = configs
values["clientExtension"]["serviceId"] = sample
values["fullnameOverride"] = sample

repository, _, tag = artifact["image"].rpartition(":")
values["image"]["repository"] = repository
values["image"]["tag"] = tag

workload = values["workload"]
workload["containerPort"] = lcp.get("loadBalancer", {}).get("targetPort", 80)
workload["kind"] = lcp.get("kind", "Deployment")
workload["replicas"] = lcp.get("scale", 1)

for key in ("livenessProbe", "readinessProbe"):
	if lcp.get(key):
		workload[key] = lcp[key]

# LCP.json declares its probes with no initial delay, relying on the platform
# to allow for start up. Kubernetes does not: a liveness probe fires at once and
# kills a JVM before Tomcat is listening. A startup probe on the same endpoint
# holds both off until the process has answered once, allowing five minutes.
handler = workload.get("readinessProbe") or workload.get("livenessProbe")

if handler:
	workload["startupProbe"] = dict(
		{key: value for key, value in handler.items() if key in ("exec", "grpc", "httpGet", "tcpSocket")},
		failureThreshold=60,
		periodSeconds=5,
	)

if lcp.get("schedule"):
	workload["schedule"] = lcp["schedule"]

env = dict(artifact["env"])
env.update({key: str(value) for key, value in (lcp.get("env") or {}).items()})

# The batch runner reads its OAuth2 application from the environment. The
# payload names it: the headless server application a batch client extension
# ships with is the only one it declares.
if repository.endswith("liferay/batch"):
	prefix = "OAuth2ProviderApplicationHeadlessServerConfiguration~"
	for config in configs:
		for pid in json.loads(config):
			if prefix in pid:
				env["LIFERAY_BATCH_OAUTH_APP_ERC"] = pid.split(prefix, 1)[1]

workload["env"] = env

values["service"]["targetPort"] = workload["containerPort"]

yaml.safe_dump(values, open(staging + "/chart/values.yaml", "w"), default_flow_style=False)

chart = yaml.safe_load(open(staging + "/chart/Chart.yaml"))
chart["name"] = sample
chart["version"] = version
yaml.safe_dump(chart, open(staging + "/chart/Chart.yaml", "w"), default_flow_style=False)

for warning in artifact["warnings"]:
	print("    warning: " + warning, file=sys.stderr)
' "${sample}" "${version}" "${staging}" \
		"${OCI_CLUSTER_HOST}/cx/${sample}@$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["manifestDigest"])' "${staging}/artifact.json")"
}

# _replay runs the Dockerfile's RUN steps, when there are any, inside the zip's
# own base image against the unpacked zip, then rebuilds the layer from the
# result. It is the one thing an image build did that mounting cannot: the node
# sample's zip carries a single one of its six dependencies, and "npm install"
# is where the rest come from. Running it here, in CI, keeps the cluster free of
# any build step, and running it in the base image means it installs for the
# exact runtime the pod will use.
function _replay {
	local staging=${1}
	local version=${2}

	local replay
	replay=$(python3 -c '
import json, sys
replay = json.load(open(sys.argv[1]))["replay"]
if replay:
	print(replay["root"])
	print(" && ".join(replay["commands"]))
' "${staging}/artifact.json")

	if [ -z "${replay}" ]
	then
		return
	fi

	local root=${replay%%$'\n'*}
	local commands=${replay#*$'\n'}

	local image
	image=$(python3 -c 'import json, sys; print(json.load(open(sys.argv[1]))["image"])' "${staging}/artifact.json")

	log "Replaying \"${commands}\" in ${image}"

	rm --force --recursive "${staging}/tree" "${staging}/layout"
	mkdir --parents "${staging}/tree" "${staging}/layout"

	unzip -q "$(_zip_of "${staging}")" -d "${staging}/tree"

	docker run \
		--entrypoint sh \
		--env HOME=/tmp \
		--rm \
		--user "$(id -u):$(id -g)" \
		--volume "${staging}/tree:${root}" \
		--workdir "${root}" \
		"${image}" -c "${commands}" > "${staging}/replay.log" 2>&1 || {
			cat "${staging}/replay.log" >&2

			return 1
		}

	(cd "${staging}/tree" && zip -q -r -y "${staging}/replayed.zip" .)

	python3 "${HACK_DIR}/cx_artifact.py" "${staging}/replayed.zip" "${staging}/layout" \
		"${version}" --replayed > "${staging}/artifact.json"
}

function _zip_of {
	echo "${SAMPLES_DIR}/$(basename "${1}")/dist/$(basename "${1}").zip"
}

# _push_artifact publishes the OCI layout cx_artifact.py wrote. The chart pins
# it by digest, so a republished tag can never change what a release mounts.
function _push_artifact {
	local sample=${1}
	local version=${2}
	local staging=${3}

	local -a copy=(skopeo copy --quiet)

	if [ "${OCI_PLAIN_HTTP}" == "true" ]
	then
		copy+=(--dest-tls-verify=false)
	fi

	"${copy[@]}" "oci:${staging}/layout:${version}" \
		"docker://${OCI_PUSH_HOST}/cx/${sample}:${version}"
}

main "${@}"