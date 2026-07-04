# Privacy Policy

**SSTVAF**
Last updated: July 4, 2026

## Overview

SSTVAF is an open-source amateur radio application for sending and receiving
SSTV (slow-scan television) images. Your privacy is important to us. This
policy explains what data the app collects, how it is used, and what choices
you have.

## Data Collection

SSTVAF does **not** collect, transmit, or sell personal information to third
parties for advertising or analytics purposes. The app contains no ads, no
tracking SDKs, and no telemetry.

### Data You Provide

- **Operator identity**: Your callsign and Maidenhead grid square, entered in
  Settings. This information is stored locally on your device and is only
  transmitted over the air as part of normal amateur radio operation (for
  example, as a caption on an SSTV image you transmit).
- **QSO logs**: Contact records are stored in a local database on your device.
- **Images**: Photos you choose to transmit are read only when you pick them.
  Received SSTV images are stored locally in the app's gallery; if you enable
  the option, they are also saved to your device's Photos gallery.

### Microphone

SSTVAF uses the microphone / audio input to receive SSTV transmissions from
your radio and decode them into images. Audio is processed on-device in real
time for decoding only — it is never recorded to a file for any other purpose
and never leaves your device.

### Location Data

If you enable "Auto-update Grid (GPS)" in Settings, the app accesses your
device's GPS to calculate your Maidenhead grid square. This location data is
not transmitted to any server — it is only used locally to set your grid and
calculate distances to other stations.

### Data Shared with Third-Party Services

If you choose to configure a logging integration, the app will transmit QSO
data to the service you enable:

- **Cloudlog / Wavelog / Nextlog** — self-hosted or cloud logging platforms

These transmissions only occur when you explicitly configure and enable them.
The data sent is limited to standard amateur radio contact information
(callsigns, grids, signal reports, frequency, mode, and time). Each service is
governed by its own privacy policy.

## Data Storage

All data is stored locally on your device — in the app's private storage, and
(for received images, when enabled) your device's Photos gallery. No data is
stored on external servers operated by SSTVAF.

## Data Sharing

SSTVAF does not share your data with any party except the logging service
listed above, and only when you explicitly configure it. Anything you transmit
over amateur radio is, by the nature of the radio service, public.

## Children's Privacy

SSTVAF does not knowingly collect information from children under 13. The app
is intended for licensed amateur radio operators.

## Open Source

SSTVAF is open-source software licensed under the MIT License. You can review
the complete source code at
[github.com/patrickrb/sstvaf](https://github.com/patrickrb/sstvaf).

## Changes to This Policy

If this policy is updated, the revised version will be posted to this page
with an updated date.

## Contact

If you have questions about this privacy policy, please open an issue on the
[GitHub repository](https://github.com/patrickrb/sstvaf/issues).
