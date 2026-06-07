# MVVM Source Structure

The SDK source tree is organized by module and layer while preserving the public
Java/Kotlin package names for consumer compatibility.

## Layer Folders

Use these folders consistently inside each module:

| Folder | Purpose |
|---|---|
| `app/` | SDK/demo bootstrap and application-level entry points |
| `presentation/api/` | Public facades and listener APIs exposed to publishers |
| `presentation/view/` | Android views, activities, and render-layer classes |
| `presentation/viewmodel/` | Format-specific ViewModels and UI orchestration |
| `presentation/mvvm/` | Shared MVVM primitives in `sdk-core` |
| `domain/model/` | Business/data models and value objects |
| `domain/policy/` | Business rules such as frequency caps |
| `domain/repository/` | Repository contracts |
| `data/repository/` | Repository implementations |
| `data/network/` | Network clients, serializers, parsers, exchanges |
| `data/parser/` | Format-specific creative parsers |
| `data/cache/` | Local cache implementations |
| `data/consent/` | Consent and privacy-state access |
| `data/device/` | Device/app signal collection |
| `data/tracking/` | Impression/click/tracking transport |
| `data/crashreporter/` | Crash-report formatting and delivery |
| `di/` | Service location and dependency wiring |
| `utils/` | Small cross-layer utilities |

## Compatibility Rule

Do not rename public package declarations just to match the folder path. For
example, `sdk-banner` keeps publisher imports such as:

```java
import com.apexads.sdk.banner.BannerAdView;
```

even though the source file lives under `presentation/view/`. This keeps Maven
artifacts binary/source-compatible while still making the repository navigable by
MVVM layer.

## Test Layout

Unit and instrumentation tests mirror the production layer they exercise:

- parser tests under `data/parser`
- network tests under `data/network`
- model tests under `domain/model`
- ViewModel/observable tests under `presentation/mvvm` or `presentation/viewmodel`
- instrumentation tests under the demo app's `instrumentation` folder
