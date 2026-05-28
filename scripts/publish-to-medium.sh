#!/usr/bin/env bash
#
# Copyright (c) 2026.
#
# This file is part of Imposter.
#
# "Commons Clause" License Condition v1.0
#
# The Software is provided to you by the Licensor under the License, as
# defined below, subject to the following condition.
#
# Without limiting other conditions in the License, the grant of rights
# under the License will not include, and the License does not grant to
# you, the right to Sell the Software.
#
# For purposes of the foregoing, "Sell" means practicing any or all of
# the rights granted to you under the License to provide to third parties,
# for a fee or other consideration (including without limitation fees for
# hosting or consulting/support services related to the Software), a
# product or service whose value derives, entirely or substantially, from
# the functionality of the Software. Any license notice or attribution
# required by the License must also include this Commons Clause License
# Condition notice.
#
# Software: Imposter
#
# License: GNU Lesser General Public License version 3
#
# Licensor: Peter Cornish
#
# Imposter is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Imposter is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with Imposter.  If not, see <https://www.gnu.org/licenses/>.
#

# Publishes a markdown file to Medium as a draft (or public post) using the
# Medium REST API. Preserves markdown formatting via contentFormat=markdown.
#
# Requires:
#   - MEDIUM_TOKEN env var (integration token from Medium Settings > Security)
#   - jq, curl

set -euo pipefail

readonly MEDIUM_API="https://api.medium.com/v1"

#######################################
# Writes an error message to stderr.
# Arguments:
#   Message tokens to print.
# Outputs:
#   Writes the message to stderr, prefixed with "ERROR:".
#######################################
err() {
  echo "ERROR: $*" >&2
}

#######################################
# Writes an error message and exits non-zero.
# Arguments:
#   Message tokens to print.
#######################################
die() {
  err "$*"
  exit 1
}

#######################################
# Prints usage information and exits non-zero.
#######################################
usage() {
  cat >&2 <<'EOF'
Usage:
  scripts/publish-to-medium.sh <markdown-file> [options]

Options:
  --title <title>      Override post title (default: first H1 in the file).
  --tags <a,b,c>       Comma-separated tags (max 5).
  --canonical <url>    Canonical URL to set on the Medium post.
  --status <s>         Publish status: draft|public|unlisted (default: draft).
  --notify-followers   Notify followers when publishing (default: false).
  --strip-header       Strip the leading H1 and "*By ... — date*" byline
                       from the body (Medium adds its own). Default: on.
  --no-strip-header    Keep the H1 and byline in the body.

Environment:
  MEDIUM_TOKEN         Required. Integration token from Medium Settings.

Example:
  export MEDIUM_TOKEN=...
  scripts/publish-to-medium.sh docs/blog/mocking-grpc.md \
    --tags "api,mock,grpc,testing" \
    --canonical "https://docs.imposter.sh/blog/mocking-grpc/"
EOF
  exit 1
}

#######################################
# Aborts if the named command is not on PATH.
# Arguments:
#   Command name.
#######################################
require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "required command not found: $1"
}

#######################################
# Extracts the first H1 heading from a markdown file.
# Arguments:
#   Path to the markdown file.
# Outputs:
#   Writes the title (without the leading "# ") to stdout.
#######################################
extract_title() {
  local file=$1
  local title
  title=$(grep -m1 '^# ' "${file}" | sed 's/^# //') || true
  [[ -n "${title}" ]] || die "no H1 found in ${file} — supply --title"
  printf '%s' "${title}"
}

#######################################
# Strips the leading H1 and an immediately-following italic byline
# (e.g. "*By Pete Cornish — 27 May 2026*") from a markdown file, so
# Medium's auto-rendered title and byline do not duplicate the body.
# Arguments:
#   Path to the markdown file.
# Outputs:
#   Writes the stripped content to stdout.
#######################################
strip_header() {
  local file=$1
  awk '
    BEGIN { dropped_h1 = 0; dropped_byline = 0 }
    !dropped_h1 && /^# / { dropped_h1 = 1; next }
    dropped_h1 && !dropped_byline && /^$/ { next }
    dropped_h1 && !dropped_byline && /^\*By .*\*$/ {
      dropped_byline = 1; next
    }
    { dropped_byline = 1; print }
  ' "${file}"
}

#######################################
# Parses CLI args, builds the API payload and posts it to Medium.
# Globals:
#   MEDIUM_TOKEN
#   MEDIUM_API
# Arguments:
#   The script's positional arguments ($@).
# Outputs:
#   Writes progress and the resulting post URL to stdout.
#######################################
main() {
  require_cmd jq
  require_cmd curl

  [[ $# -ge 1 ]] || usage
  local file=$1
  shift
  [[ -f "${file}" ]] || die "file not found: ${file}"
  [[ -n "${MEDIUM_TOKEN:-}" ]] || die "MEDIUM_TOKEN env var is not set"

  local title=""
  local tags_csv=""
  local canonical=""
  local status="draft"
  local notify="false"
  local strip="true"

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --title)
        title=$2
        shift 2
        ;;
      --tags)
        tags_csv=$2
        shift 2
        ;;
      --canonical)
        canonical=$2
        shift 2
        ;;
      --status)
        status=$2
        shift 2
        ;;
      --notify-followers)
        notify="true"
        shift
        ;;
      --strip-header)
        strip="true"
        shift
        ;;
      --no-strip-header)
        strip="false"
        shift
        ;;
      -h|--help)
        usage
        ;;
      *)
        die "unknown argument: $1"
        ;;
    esac
  done

  case "${status}" in
    draft|public|unlisted)
      ;;
    *)
      die "invalid --status: ${status} (expected draft|public|unlisted)"
      ;;
  esac

  [[ -n "${title}" ]] || title=$(extract_title "${file}")

  local content
  if [[ "${strip}" == "true" ]]; then
    content=$(strip_header "${file}")
  else
    content=$(cat "${file}")
  fi

  local tags_json="[]"
  if [[ -n "${tags_csv}" ]]; then
    tags_json=$(printf '%s' "${tags_csv}" \
      | jq -Rc 'split(",") | map(. | gsub("^\\s+|\\s+$"; ""))')
  fi

  echo "→ fetching Medium user id..."
  local me_response
  me_response=$(curl -sfS \
    -H "Authorization: Bearer ${MEDIUM_TOKEN}" \
    -H "Accept: application/json" \
    "${MEDIUM_API}/me") \
    || die "failed to fetch /me — check MEDIUM_TOKEN"

  local user_id
  user_id=$(printf '%s' "${me_response}" | jq -r '.data.id')
  if [[ -z "${user_id}" || "${user_id}" == "null" ]]; then
    die "could not parse user id from response"
  fi

  echo "→ posting '${title}' as ${status} for user ${user_id}..."

  local payload
  payload=$(jq -n \
    --arg title "${title}" \
    --arg content "${content}" \
    --arg status "${status}" \
    --arg canonical "${canonical}" \
    --argjson tags "${tags_json}" \
    --argjson notify "${notify}" \
    '{
       title: $title,
       contentFormat: "markdown",
       content: $content,
       tags: $tags,
       publishStatus: $status,
       notifyFollowers: $notify
     }
     + (if $canonical == ""
        then {}
        else { canonicalUrl: $canonical }
        end)')

  local response
  response=$(curl -sfS -X POST \
    -H "Authorization: Bearer ${MEDIUM_TOKEN}" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -d "${payload}" \
    "${MEDIUM_API}/users/${user_id}/posts") \
    || die "Medium API request failed"

  local url
  local id
  url=$(printf '%s' "${response}" | jq -r '.data.url // empty')
  id=$(printf '%s' "${response}" | jq -r '.data.id // empty')

  if [[ -n "${url}" ]]; then
    echo "✓ posted: ${url}"
    [[ -n "${id}" ]] && echo "  id: ${id}"
  else
    err "unexpected response:"
    printf '%s\n' "${response}" >&2
    exit 1
  fi
}

main "$@"
