# Documentation Index

> Korean version: [ko/INDEX.md](ko/INDEX.md)

## Purpose

This is the navigation hub for PhonePad engineering, design, testing, release, and collaboration docs.

## Current State

PhonePad is in a documentation and Phase 0 spike stage. The docs are organized so implementation can begin without committing private planning files.

## Current Rules

- `README.md` and `CLAUDE.md` live at the repo root.
- Product, architecture, test, and release docs live under `docs/`.
- Reference templates and AI prompts live under `docs/ref/`.
- PRD drafts and chat exports stay local and ignored by git.

## Core Docs

| Doc | Use When |
|---|---|
| [QUICK_REF.md](QUICK_REF.md) | You need the short product contract. |
| [ARCHITECTURE.md](ARCHITECTURE.md) | You are changing app layers, lifecycle, or native integration. |
| [HID.md](HID.md) | You are working on Bluetooth HID, descriptors, pairing, or permissions. |
| [DRAG_MODE.md](DRAG_MODE.md) | You are touching drag behavior or lifecycle safety release. |
| [DB.md](DB.md) | You are adding local settings, host records, or gesture mappings. |
| [API.md](API.md) | You are wiring Flutter to Android native or dongle packets. |
| [DESIGN.md](DESIGN.md) | You are building screens, components, states, or copy. |
| [MOBILE_APP.md](MOBILE_APP.md) | You are changing Android build, permissions, services, or store release. |
| [SECURITY.md](SECURITY.md) | You are adding permissions, logging, telemetry, or data handling. |
| [TEST.md](TEST.md) | You are verifying compatibility, drag, reconnection, or release gates. |
| [PHASE0_TEST_REPORT_2026-05-27.md](PHASE0_TEST_REPORT_2026-05-27.md) | You need the latest recorded ADB Phase 0 verification evidence. |
| [ROADMAP.md](ROADMAP.md) | You need phase order and scope boundaries. |
| [DEPLOY.md](DEPLOY.md) | You are preparing GitHub Releases or Play Store. |
| [CONTRIBUTING.md](CONTRIBUTING.md) | You are collaborating through GitHub issues, PRs, or spikes. |
| [BRIDGE_DONGLE.md](BRIDGE_DONGLE.md) | You are validating the hardware Plan B. |

## Related Docs

- [../README.md](../README.md)
- [../CLAUDE.md](../CLAUDE.md)
- [ref/README.md](ref/README.md)
