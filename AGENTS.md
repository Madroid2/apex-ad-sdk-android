# Repository Agent Instructions

This repository is for the open-source Android SDK. Website source and hosting
are maintained separately in a private repository. Do not add a `site/` directory
or a GitHub Pages deployment workflow here. Run `node scripts/check-source-boundary.mjs`
before committing. After a history rewrite, do not merge an old local branch back
into this repository. Rebase or re-clone to avoid restoring removed website history.

Before changing deployment configuration or declaring the Apex trust layer production-
ready, read and complete `docs/PRODUCTION_TRUST_ACTIVATION.md`.

- Do not invent or commit Google credentials, publisher tokens, lease secrets, or raw
  Play Integrity tokens.
- Do not claim T2/T3 activation from unit tests or mocked verdicts; the runbook requires a
  live token from a Google Play-installed release and cross-service acceptance evidence.
- Keep the trust lease secret and Play service-account credentials out of the Android app.
