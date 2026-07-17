# Repository Agent Instructions

Before changing deployment configuration or declaring the Apex trust layer production-
ready, read and complete `docs/PRODUCTION_TRUST_ACTIVATION.md`.

- Do not invent or commit Google credentials, publisher tokens, lease secrets, or raw
  Play Integrity tokens.
- Do not claim T2/T3 activation from unit tests or mocked verdicts; the runbook requires a
  live token from a Google Play-installed release and cross-service acceptance evidence.
- Keep the trust lease secret and Play service-account credentials out of the Android app.
