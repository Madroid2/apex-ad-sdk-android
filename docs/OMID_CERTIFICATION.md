# Apex Ad SDK OMID Certification

## Eligibility decision

IAB Tech Lab's OM SDK Integration Validation Compliance (IVC) program accepts
Ad SDK providers. IAB Tech Lab membership is not required. The applicant must,
however, register in the Tools Portal with a company-domain email address;
free-mail domains are not accepted. The generated OM SDK build and Partner
namespace are tied to that domain.

For Apex, the practical applicant can be a company, a registered sole
proprietorship, or another legal trading entity that owns a domain. The public
certificate can identify the product as **Apex Ad SDK**, while the service
order, billing, Tools Portal account, and Partner namespace use the applicant's
legal/domain identity.

## Current readiness

| Requirement | Apex status |
| --- | --- |
| Android SDK controls its own display containers | Ready |
| HTML display render lifecycle wired to a measurement delegate | Ready |
| OMID signaling honesty-gated (`api=7`, `omidpn`, `omidpv`) | Ready |
| `omidpv` follows the submitted Apex SDK release version | Ready |
| IAB company-domain Tools Portal account | External action required |
| IAB-issued OM SDK 1.6 Android AAR and Partner namespace | External action required |
| Native OM SDK activation, sessions, events, and friendly obstructions | Blocked on IAB AAR |
| Scrollable certification sample app and verification test creatives | Pending after integration |
| IVC intake, service order, payment, and IAB testing | External action required |

Do not advertise OMID support or set `OMID_ACTIVE` to `true` until the portal
AAR is integrated and the certification build passes device tests.

## Tools Portal and intake sequence

1. Establish the applicant legal/trading name, owned domain, domain email, and
   billing contact.
2. Register at <https://tools.iabtechlab.com/> using that domain email.
3. Request **OM SDK** access and generate the Android OM SDK 1.6 build for the
   assigned namespace.
4. Place the issued AAR in `sdk-measurement/libs` and configure builds with:

   ```bash
   ./gradlew verifyOmidCertificationReadiness \
     -PAPEX_VERSION=<release-version> \
     -PAPEX_OMID_PARTNER_NAME=<iab-partner-namespace>
   ```

5. Complete activation, Partner creation, HTML/native/video sessions, loaded
   and impression events, verification resources, friendly obstructions, and
   the IAB kill switch.
6. Build a production-equivalent sample app where each ad can be moved from 0%
   to 100% viewability. Include HTML display, native display, and video only if
   all three are ready to certify in the same SDK release.
7. Submit the OM SDK intake form and coordinate validation with
   `compliance@iabtechlab.com`.

## Prepared IVC questionnaire

Use these answers after replacing bracketed business details and confirming the
release dates.

- **Applicant/product:** `[LEGAL APPLICANT NAME]`, Apex Ad SDK for Android.
- **Platform:** Android mobile in-app.
- **Expected integration date:** After IAB Tools Portal access and receipt of
  the OM SDK 1.6 Android build; target `[DATE]`.
- **Release constraints:** Certification build must precede public enablement
  of OpenRTB OMID capability signaling. Apex will retain a runtime kill switch
  and will not signal `api=7` before measurement is active.
- **Ready to begin certification:** `[DATE]`, after the IAB verification-tag
  sample app passes the published validation cases.
- **Framework supports multiple apps:** Yes. Apex is an Ad SDK intended for
  integration by multiple publisher applications.
- **Display containers:** Apex creates and manages its own banner, MRECT,
  interstitial, and app-open ad containers.
- **Display rendering:** HTML display is rendered in an SDK-owned WebView.
- **Third-party ad framework/SSP:** N/A for Apex-rendered demand; OpenRTB demand
  is supplied by Apex and configured partners.
- **Native containers:** Apex manages the ad request and measurement lifecycle;
  the host app renders native assets using the SDK contract. Certification must
  exercise the exact supported native rendering path.
- **Native video:** Not in the first certification scope unless the native
  video OMID lifecycle is complete before intake.
- **Video player:** Android Media3/ExoPlayer for VAST video.
- **Video rendering:** Native player, not WebView video.
- **Ad stitching:** No.

## Commercial and maintenance obligations

The public 2026 non-member price for OM SDK Mobile App compliance is $12,000
initially and $6,800 at annual renewal. Confirm scope and any multi-SDK pricing
with IAB Tech Lab before signing. Certification is global, is published by
Partner name and SDK version, and requires annual recertification plus retest
when formats, placements, or rendering behavior materially change.

Official references:

- <https://iabtechlab.com/wp-content/uploads/2025/02/IVC_guide.pdf>
- <https://iabtechlab.com/wp-content/uploads/2025/02/integration_onboard.pdf>
- <https://iabtechlab.com/compliance-programs/techlab-compliance-pricing/>
- <https://iabtechlab.com/standards/open-measurement-sdk/>
