#!/usr/bin/env bash
# Unpack the decompiled Minecraft + NeoForge sources that ModDevGradle generated
# into .mc-sources/ so Claude Code, ripgrep and VS Code can search them.
# The output is Mojang's copyrighted code: it is gitignored and must never be committed.
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
out_dir="${repo_root}/.mc-sources"
neo_version="$(grep -E '^neo_version=' "${repo_root}/gradle.properties" | cut -d= -f2)"

# ModDevGradle keeps its generated artifacts under <module>/build/moddev. Look there first,
# then fall back to the Gradle caches. Prefer a jar whose name contains the NeoForge version.
search_roots=("${repo_root}/neoforge/build/moddev" "${repo_root}/build/moddev" "${HOME}/.gradle/caches")
jar=""
for root in "${search_roots[@]}"; do
  [[ -d "${root}" ]] || continue
  jar="$(find "${root}" -type f -name "*${neo_version}*sources*.jar" 2>/dev/null | head -n 1 || true)"
  [[ -n "${jar}" ]] && break
done

if [[ -z "${jar}" ]]; then
  echo "No sources jar for NeoForge ${neo_version} found." >&2
  echo "Run ./gradlew build (or sync the project in your IDE) first, then re-run this script." >&2
  echo "If it still fails, list candidates with:" >&2
  echo "  find neoforge/build/moddev ~/.gradle/caches -name '*sources*.jar' | grep -i neoforge" >&2
  exit 1
fi

echo "Using ${jar}"
rm -rf "${out_dir}"
mkdir -p "${out_dir}"
(cd "${out_dir}" && unzip -q "${jar}")
echo "${neo_version}" > "${out_dir}/.neo_version"

if ! grep -qxF '.mc-sources/' "${repo_root}/.gitignore" 2>/dev/null; then
  echo '.mc-sources/' >> "${repo_root}/.gitignore"
  echo "Added .mc-sources/ to .gitignore"
fi

count="$(find "${out_dir}" -name '*.java' | wc -l)"
echo "Extracted ${count} .java files into .mc-sources/"
