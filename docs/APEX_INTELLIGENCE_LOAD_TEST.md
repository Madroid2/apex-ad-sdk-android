# Apex Intelligence SDK load test

`ApexIntelligenceLoadInstrumentationTest` validates the SDK → Ad Server →
Intelligence transport using explicitly synthetic auction opportunities.

It does not render ads and does not call win, bill, impression, click, or
conversion trackers. Test mode suppresses advertising/user IDs and declares
LMT/DNT. Intelligence stores this traffic as `traffic_kind=synthetic`, which is
never eligible for model training or readiness gates.

With an emulator running and a test Ad Server listening on the host:

```bash
./gradlew :demo-app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.apexads.demo.ApexIntelligenceLoadInstrumentationTest \
  -Pandroid.testInstrumentationRunnerArguments.apexLoadCount=1000 \
  -Pandroid.testInstrumentationRunnerArguments.apexBatchSize=25 \
  -Pandroid.testInstrumentationRunnerArguments.apexAdServerUrl=http://10.0.2.2:18080/openrtb/v1/auction
```

Keep the test endpoint isolated from advertiser reporting. A successful run
proves request construction, auction transport, queue behavior, schema
validation, and observation persistence; it does not prove CTR lift or model
maturity.

Set `apexBatchSize=1` when validating functional bidder behavior independently
of burst capacity. Increase it deliberately for concurrency and circuit-breaker
testing.
