#!/usr/bin/env bash
#
# Maven Central publishing script (macOS) — project-agnostic.
#
# Stores Maven Central credentials and the GPG signing key in the macOS Keychain
# (encrypted, per-user) and feeds them to Gradle as ORG_GRADLE_PROJECT_* environment
# variables at publish time. Secrets never touch gradle.properties or the repo.
#
# Credentials are shared across all projects on this machine — set up once, then drop
# this script into any Gradle project that uses the vanniktech maven-publish plugin
# (or any build reading mavenCentralUsername / signingInMemoryKey properties) and has
# VERSION_NAME in its gradle.properties.
#
# Usage:
#   ./scripts/publish.sh setup      # interactive one-time credential setup (per machine)
#   ./scripts/publish.sh            # publish this project to Maven Central
#   ./scripts/publish.sh local      # dry run: publish to ~/.m2 (no credentials needed)
#   ./scripts/publish.sh status     # show which credentials are stored
#   ./scripts/publish.sh reset      # remove stored credentials from the Keychain
#
set -euo pipefail

ACCOUNT="maven-central-publish"
ITEM_USER="maven-central.username"
ITEM_PASS="maven-central.password"
ITEM_KEY="maven-central.signingKeyBase64"
ITEM_KEYPASS="maven-central.signingKeyPassword"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

red()   { printf '\033[31m%s\033[0m\n' "$*"; }
green() { printf '\033[32m%s\033[0m\n' "$*"; }
bold()  { printf '\033[1m%s\033[0m\n' "$*"; }

require_macos() {
  [[ "$(uname)" == "Darwin" ]] || { red "This script uses the macOS Keychain and only runs on macOS."; exit 1; }
}

keychain_get() { security find-generic-password -a "$ACCOUNT" -s "$1" -w 2>/dev/null; }
keychain_set() { security add-generic-password -U -a "$ACCOUNT" -s "$1" -w "$2"; }
keychain_del() { security delete-generic-password -a "$ACCOUNT" -s "$1" >/dev/null 2>&1 || true; }
keychain_has() { security find-generic-password -a "$ACCOUNT" -s "$1" >/dev/null 2>&1; }

# One-time migration from the original kova-specific Keychain names.
migrate_legacy() {
  local pairs=(
    "kova.mavenCentralUsername:$ITEM_USER"
    "kova.mavenCentralPassword:$ITEM_PASS"
    "kova.signingKeyBase64:$ITEM_KEY"
    "kova.signingKeyPassword:$ITEM_KEYPASS"
  )
  for pair in "${pairs[@]}"; do
    local old="${pair%%:*}" new="${pair##*:}"
    if ! keychain_has "$new" \
        && security find-generic-password -a "kova-publish" -s "$old" >/dev/null 2>&1; then
      keychain_set "$new" "$(security find-generic-password -a "kova-publish" -s "$old" -w)"
      security delete-generic-password -a "kova-publish" -s "$old" >/dev/null 2>&1 || true
    fi
  done
}

gradle_prop() { grep "^$1=" "$ROOT/gradle.properties" | head -1 | cut -d= -f2-; }

project_name() {
  local name
  name="$(gradle_prop POM_NAME)"
  [[ -n "$name" ]] && echo "$name" || basename "$ROOT"
}

find_java_home() {
  local jbr="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  if [[ -x "$jbr/bin/java" ]]; then
    echo "$jbr"
  elif /usr/libexec/java_home -v 17+ >/dev/null 2>&1; then
    /usr/libexec/java_home -v 17+
  else
    red "No JDK 17+ found. Install one, or Android Studio (its bundled JDK works)."
    exit 1
  fi
}

setup() {
  bold "Maven Central publishing setup — credentials go to your macOS Keychain."
  bold "This is per-machine, shared by every project that uses this script."
  echo
  echo "You need a Central Portal user token (central.sonatype.com → account → Generate User Token)"
  echo "and a GPG signing key exported with:"
  echo "  gpg --export-secret-keys --armor <KEY_ID> > ~/signing-key.asc"
  echo

  read -r -p "Maven Central token username: " mc_user
  [[ -n "$mc_user" ]] || { red "Username must not be empty."; exit 1; }

  read -r -s -p "Maven Central token password: " mc_pass; echo
  [[ -n "$mc_pass" ]] || { red "Password must not be empty."; exit 1; }

  read -r -p "Path to armored GPG secret key file (e.g. ~/signing-key.asc): " key_path
  key_path="${key_path/#\~/$HOME}"
  [[ -f "$key_path" ]] || { red "File not found: $key_path"; exit 1; }
  grep -q "BEGIN PGP PRIVATE KEY BLOCK" "$key_path" \
    || { red "That file doesn't look like an armored GPG secret key."; exit 1; }

  read -r -s -p "GPG key passphrase (empty if none): " key_pass; echo

  keychain_set "$ITEM_USER" "$mc_user"
  keychain_set "$ITEM_PASS" "$mc_pass"
  keychain_set "$ITEM_KEY" "$(base64 < "$key_path")"
  keychain_set "$ITEM_KEYPASS" "$key_pass"

  echo
  green "Stored in Keychain. The key file on disk is no longer needed — consider deleting it:"
  echo "  rm '$key_path'"
}

status() {
  migrate_legacy
  for item in "$ITEM_USER" "$ITEM_PASS" "$ITEM_KEY" "$ITEM_KEYPASS"; do
    if keychain_has "$item"; then green "  ✓ $item"; else red "  ✗ $item (missing)"; fi
  done
}

reset() {
  for item in "$ITEM_USER" "$ITEM_PASS" "$ITEM_KEY" "$ITEM_KEYPASS"; do keychain_del "$item"; done
  green "Maven Central publishing credentials removed from the Keychain."
}

publish_local() {
  export JAVA_HOME="$(find_java_home)"
  bold "Publishing to ~/.m2 (mavenLocal) — no signing, no credentials."
  "$ROOT/gradlew" -p "$ROOT" publishToMavenLocal
  local group_path
  group_path="$(gradle_prop GROUP | tr . /)"
  green "Done. Artifacts in ~/.m2/repository/${group_path:-<group>}/"
}

publish_central() {
  migrate_legacy
  keychain_has "$ITEM_USER" && keychain_has "$ITEM_PASS" && keychain_has "$ITEM_KEY" || {
    bold "No stored credentials found — running setup first."
    echo
    setup
    echo
  }

  export JAVA_HOME="$(find_java_home)"
  export ORG_GRADLE_PROJECT_mavenCentralUsername="$(keychain_get "$ITEM_USER")"
  export ORG_GRADLE_PROJECT_mavenCentralPassword="$(keychain_get "$ITEM_PASS")"
  export ORG_GRADLE_PROJECT_signingInMemoryKey="$(keychain_get "$ITEM_KEY" | base64 -d)"
  export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword="$(keychain_get "$ITEM_KEYPASS")"

  local version name
  version="$(gradle_prop VERSION_NAME)"
  [[ -n "$version" ]] || { red "VERSION_NAME not found in $ROOT/gradle.properties."; exit 1; }
  name="$(project_name)"

  bold "About to publish $name $version to Maven Central as $(keychain_get "$ITEM_USER")."
  red  "Maven Central releases are permanent — they cannot be unpublished."
  read -r -p "Type the version ($version) to confirm: " confirm
  [[ "$confirm" == "$version" ]] || { red "Aborted."; exit 1; }

  "$ROOT/gradlew" -p "$ROOT" publishToMavenCentral --no-configuration-cache

  echo
  green "Upload complete. Final step: central.sonatype.com → Deployments → Publish."
}

require_macos
case "${1:-publish}" in
  setup)   setup ;;
  status)  status ;;
  reset)   reset ;;
  local)   publish_local ;;
  publish) publish_central ;;
  *)       echo "Usage: $0 [setup|status|reset|local|publish]"; exit 1 ;;
esac
