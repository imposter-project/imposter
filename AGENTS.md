# Agent guide

This repository holds the user-facing documentation for Imposter — published to
[docs.imposter.sh](https://docs.imposter.sh/) via MkDocs — along with examples and
supporting tooling.

## Documentation is the source of truth here

User-facing product documentation for the whole Imposter project lives in this
repo. Prefer adding or updating user docs here rather than in the individual tool
repositories (such as `imposter-cli`), which should link to docs.imposter.sh
instead of duplicating content. Maintainer- and developer-only material (build
steps, internal design, contributing notes) belongs in the relevant tool's own
repo.

## Adding or changing a docs page

Pages live under `docs/` as Markdown. When you add a new page, wire it into both:

- `mkdocs.yml` — add an entry under the appropriate `nav:` section
- `docs/index.md` — add a link under the matching heading

Match the surrounding pages: task-oriented and user-focused, with `> **Note**`
callouts and a `## What's next` footer where it helps. Cross-link related pages
with relative links (e.g. `./scaffold.md`).
