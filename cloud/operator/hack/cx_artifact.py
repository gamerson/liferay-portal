"""Turns a client extension zip into an OCI image to mount, without building one.

The zip a Liferay Workspace build produces carries a Dockerfile that is never
more than a FROM and a COPY: a stock Liferay base image, plus the extension's
files copied to where that image expects them. Rather than run that build, this
reads it. The files go into a single layer that holds the whole zip, the base
image and the COPY destinations come back as data, and the chart mounts the
layer into the base image as an image volume. Nothing is built between the zip
and the cluster, and no container daemon is needed to publish it.

An image volume can only mount a directory, never a single file. A COPY whose
destination is a file -- jar-runner's "COPY *.jar /opt/liferay/jar-runner.jar"
-- therefore also gets a directory of its own inside the layer, holding the file
under its destination name as a hard link, so the directory can be mounted over
the destination's parent without the jar being stored twice.

A RUN step cannot be skipped the same way: it produced files the zip does not
carry -- the node sample's "RUN npm install" is where its dependencies come
from. When the zip is copied whole to one destination, the step is reported as
"replay" instead, for the packaging step to run inside the base image against
the unpacked zip before the layer is built. Pass --replayed once it has.

    cx_artifact.py <zip> <oci-layout-dir> <tag> [--replayed]

prints {"env", "image", "manifestDigest", "mounts", "replay", "warnings"} as
JSON.
"""

import fnmatch
import gzip
import hashlib
import io
import json
import os
import posixpath
import shlex
import sys
import tarfile
import zipfile

MOUNTS_DIR = ".cx-mounts"


def _blob(layout, data, media_type):
	digest = hashlib.sha256(data).hexdigest()

	directory = os.path.join(layout, "blobs", "sha256")

	os.makedirs(directory, exist_ok=True)

	with open(os.path.join(directory, digest), "wb") as out:
		out.write(data)

	return {"digest": "sha256:" + digest, "mediaType": media_type, "size": len(data)}


def _layer(archive, entries, links):
	"""Builds the tar: every zip entry at its own path, plus the hard links that
    give each file destination a mountable directory. Timestamps are fixed so the
    same zip always produces the same digest."""

	directories = set()

	for name in list(entries) + [link for link, _ in links]:
		parent = posixpath.dirname(name)

		while parent:
			directories.add(parent)
			parent = posixpath.dirname(parent)

	directories.update(name for name, entry in entries.items() if entry.is_dir())

	raw = io.BytesIO()

	with tarfile.open(fileobj=raw, mode="w", format=tarfile.PAX_FORMAT) as tar:
		for directory in sorted(directories):
			info = tarfile.TarInfo(directory)
			info.mode = 0o755
			info.mtime = 0
			info.type = tarfile.DIRTYPE
			tar.addfile(info)

		for name, entry in sorted(entries.items()):
			if entry.is_dir():
				continue

			data = archive.read(entry)

			info = tarfile.TarInfo(name)
			info.mode = ((entry.external_attr >> 16) & 0o777) or 0o644
			info.mtime = 0
			info.size = len(data)
			tar.addfile(info, io.BytesIO(data))

		for link, target in links:
			info = tarfile.TarInfo(link)
			info.linkname = target
			info.mode = 0o644
			info.mtime = 0
			info.type = tarfile.LNKTYPE
			tar.addfile(info)

	return raw.getvalue()


def _parse(dockerfile):
	image = None
	copies = []
	env = {}
	runs = []
	warnings = []

	for line in dockerfile.splitlines():
		line = line.strip()

		if not line or line.startswith("#"):
			continue

		instruction, _, rest = line.partition(" ")
		instruction = instruction.upper()
		arguments = [
			argument for argument in shlex.split(rest) if not argument.startswith("--")
		]

		if instruction == "FROM":
			image = arguments[0]

			if ":" not in image.rsplit("/", 1)[-1]:
				image += ":latest"
		elif instruction == "COPY":
			*sources, destination = arguments
			copies.append((sources, destination))
		elif instruction == "ENV":
			if "=" in arguments[0]:
				for argument in arguments:
					key, _, value = argument.partition("=")
					env[key] = value
			else:
				env[arguments[0]] = " ".join(arguments[1:])
		elif instruction == "RUN":
			runs.append(rest.strip())
		else:
			# Anything else ran at image build time, which no longer happens.
			# The mount is read only, so it cannot be replayed at runtime
			# either; whoever relied on it needs to know.
			warnings.append(
				"%s is not applied: the artifact is mounted, not built" % line
			)

	if image is None:
		raise SystemExit("the Dockerfile names no base image")

	return image, copies, env, runs, warnings


def _plan(copies, files, entries):
	mounts = []
	links = []
	file_destinations = {}

	for sources, destination in copies:
		for source in sources:
			source = source.strip("/") if source not in (".", "./") else ""

			is_directory = (source == "") or any(
				name.startswith(source + "/") for name in entries
			)

			if is_directory:
				mounts.append(
					{"mountPath": destination.rstrip("/") or "/", "subPath": source}
				)

				continue

			matches = [name for name in files if fnmatch.fnmatchcase(name, source)]

			if not matches:
				raise SystemExit("COPY %s matches nothing in the zip" % source)

			for match in matches:
				if destination.endswith("/"):
					target = posixpath.join(destination, posixpath.basename(match))
				else:
					target = destination

				file_destinations.setdefault(posixpath.dirname(target), []).append(
					(posixpath.basename(target), match)
				)

	for index, (directory, members) in enumerate(sorted(file_destinations.items())):
		mount_dir = "%s/%d" % (MOUNTS_DIR, index)

		for name, target in members:
			links.append((mount_dir + "/" + name, target))

		mounts.append({"mountPath": directory or "/", "subPath": mount_dir})

	return mounts, links


def _write_layout(layout, layer, tag):
	compressed = gzip.compress(layer, mtime=0)

	config = json.dumps(
		{
			"architecture": "amd64",
			"config": {},
			"os": "linux",
			"rootfs": {
				"diff_ids": ["sha256:" + hashlib.sha256(layer).hexdigest()],
				"type": "layers",
			},
		},
		sort_keys=True,
	).encode()

	manifest = json.dumps(
		{
			"config": _blob(layout, config, "application/vnd.oci.image.config.v1+json"),
			"layers": [
				_blob(layout, compressed, "application/vnd.oci.image.layer.v1.tar+gzip")
			],
			"mediaType": "application/vnd.oci.image.manifest.v1+json",
			"schemaVersion": 2,
		},
		sort_keys=True,
	).encode()

	descriptor = _blob(layout, manifest, "application/vnd.oci.image.manifest.v1+json")
	descriptor["annotations"] = {"org.opencontainers.image.ref.name": tag}

	with open(os.path.join(layout, "index.json"), "w") as out:
		json.dump({"manifests": [descriptor], "schemaVersion": 2}, out)

	with open(os.path.join(layout, "oci-layout"), "w") as out:
		json.dump({"imageLayoutVersion": "1.0.0"}, out)

	return descriptor["digest"]


def main():
	zip_path, layout, tag = sys.argv[1:4]
	replayed = "--replayed" in sys.argv[4:]

	with zipfile.ZipFile(zip_path) as archive:
		entries = {
			entry.filename.rstrip("/"): entry for entry in archive.infolist()
		}

		if "Dockerfile" not in entries:
			raise SystemExit("%s carries no Dockerfile" % zip_path)

		dockerfile = archive.read("Dockerfile").decode()

		image, copies, env, runs, warnings = _parse(dockerfile)

		files = sorted(name for name, entry in entries.items() if not entry.is_dir())

		mounts, links = _plan(copies, files, entries)

		layer = _layer(archive, entries, links)

	digest = _write_layout(layout, layer, tag)

	# A RUN is only replayable when the zip lands whole in one place, so the
	# step sees exactly the tree the Dockerfile's build would have.
	roots = [destination for sources, destination in copies if sources in (["."], ["./"])]

	replay = None

	if runs and not replayed:
		if len(roots) == 1 and len(copies) == 1:
			replay = {"commands": runs, "root": roots[0].rstrip("/") or "/"}
		else:
			warnings.extend(
				"RUN %s is not applied: the artifact is mounted, not built" % run
				for run in runs
			)

	json.dump(
		{
			"env": env,
			"image": image,
			"manifestDigest": digest,
			"mounts": mounts,
			"replay": replay,
			"warnings": warnings,
		},
		sys.stdout,
		indent=2,
		sort_keys=True,
	)


if __name__ == "__main__":
	main()