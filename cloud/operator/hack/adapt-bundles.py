"""Adapts a master-built OSGi bundle to a published Liferay image.

A bundle compiled against this branch imports packages at the versions this
branch exports. A published image ships older ones, so an Import-Package floor
can sit above what the image provides and the bundle fails to resolve -- which,
for a bundle in osgi/static, stops the portal from booting.

This lowers each import floor to the version the target image actually exports,
and touches nothing else. It is a deployment step, not a source change: the
repository keeps the versions this branch really builds against.
"""

import argparse
import json
import re
import shutil
import zipfile

_VERSION_PATTERN = re.compile(r'version="\[?([0-9]+(?:\.[0-9]+)*)')


def adapt(jar_path, exports, report):
	manifest = read_manifest(jar_path)

	match = re.search(r"^Import-Package: (.*)$", manifest, re.MULTILINE)

	if not match:
		return False

	changed = False
	adapted_clauses = []

	for clause in split_clauses(match.group(1)):
		package = clause.split(";")[0].strip()
		version_match = _VERSION_PATTERN.search(clause)

		if version_match and package in exports:
			required = tuple(int(part) for part in version_match.group(1).split("."))
			available = tuple(int(part) for part in exports[package].split("."))

			if required > available:
				clause = clause.replace(version_match.group(1), exports[package], 1)
				changed = True

				report.append(
					"  %s %s -> %s"
					% (package, version_match.group(1), exports[package])
				)

		adapted_clauses.append(clause)

	if not changed:
		return False

	manifest = (
		manifest[: match.start()]
		+ "Import-Package: "
		+ ",".join(adapted_clauses)
		+ manifest[match.end() :]
	)

	manifest = "\n".join(fold(line) for line in manifest.split("\n"))

	temporary_path = jar_path + ".adapted"

	with zipfile.ZipFile(jar_path) as source:
		with zipfile.ZipFile(temporary_path, "w", zipfile.ZIP_DEFLATED) as target:
			for item in source.infolist():
				data = source.read(item.filename)

				if item.filename == "META-INF/MANIFEST.MF":
					data = manifest.encode("utf-8")

				target.writestr(item, data)

	shutil.move(temporary_path, jar_path)

	return True


def fold(line):
	folded = line[:72]
	rest = line[72:]

	while rest:
		folded += "\n " + rest[:71]
		rest = rest[71:]

	return folded


def main():
	parser = argparse.ArgumentParser()
	parser.add_argument("--exports", required=True)
	parser.add_argument("jars", nargs="+")

	arguments = parser.parse_args()

	with open(arguments.exports) as exports_file:
		exports = json.load(exports_file)

	for jar_path in arguments.jars:
		report = []

		if adapt(jar_path, exports, report):
			print("adapted %s" % jar_path)

			for line in report:
				print(line)
		else:
			print("unchanged %s" % jar_path)


def read_manifest(jar_path):
	with zipfile.ZipFile(jar_path) as jar:
		raw = jar.read("META-INF/MANIFEST.MF").decode("utf-8")

	return raw.replace("\r\n", "\n").replace("\n ", "")


def split_clauses(header):
	clauses = []
	quoted = False
	current = ""

	for character in header:
		if character == '"':
			quoted = not quoted

		if character == "," and not quoted:
			clauses.append(current)
			current = ""
		else:
			current += character

	if current:
		clauses.append(current)

	return clauses


main()