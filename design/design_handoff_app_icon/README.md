# Handoff: Alongside App Icon

## Overview
App icon for **Alongside** (KMP — Compose Multiplatform, Android + iOS): a couples long-distance trip app. Icon concept: two overlapping circles (blue + yellow, the couple) inside a thin cream emblem ring, on a warm terracotta ground.

## About the Design Files
The `Alongside App Icon.dc.html` file is an **HTML design reference** showing the icon composition and exact colors/proportions — not production code. The PNGs in `assets/` are the actual exported rasters, ready to drop into the native projects. Fidelity: **high-fidelity** — colors and geometry in the PNGs are final.

## Assets (all 1024×1024 PNG, @4x from a 256px source)
- `assets/ios-icon-1024.png` — flat iOS icon: terracotta background baked in, **no alpha channel**, no rounded corners (iOS masks it automatically). Drop straight into `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/` as the single 1024×1024 entry (Xcode 14+ supports one universal size in `Contents.json`; otherwise resize down for the legacy per-size slots).
- `assets/android-foreground-1024.png` — Android adaptive icon **foreground layer**, transparent background (verified alpha channel), just the ring+circles emblem. Save as `androidApp/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png` (or equivalent per-density, downscaled).
- `assets/android-background-1024.png` — Android adaptive icon **background layer**, solid terracotta with a subtle dot texture, no transparency. Save as `androidApp/src/main/res/drawable-xxxhdpi/ic_launcher_background.png`.

The emblem is sized to sit inside Android's ~66%-diameter safe zone, so it survives circle, squircle, and rounded-square OEM masks without clipping.

### Wiring the Android adaptive icon
Reference both layers from `androidApp/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` (and `ic_launcher_round.xml`):
```xml
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
```
Also generate flattened legacy `mipmap-*/ic_launcher.png` fallbacks (pre-API 26) — run the two layers through Android Studio's Image Asset Studio, or any adaptive-icon flattening tool, to get per-density legacy PNGs.

### Wiring the iOS icon
Single 1024×1024, RGB (no alpha), no corner rounding — Apple's platform applies the mask. Set as the "App Store iOS 1024pt" (or universal) slot in the asset catalog's `Contents.json`.

## Design Tokens
- Background (terracotta): `oklch(0.68 0.15 45)`
- Background dot texture: `oklch(0.63 0.15 45)`, 16px grid, 1.5px dots
- Emblem ring (cream): `oklch(0.97 0.01 85)`, 7px stroke on the 256px source (scales to ~28px at 1024)
- Circle 1 (blue): `oklch(0.55 0.16 250)`
- Circle 2 (yellow): `oklch(0.88 0.19 95)`

These are the exact CSS values used to render the PNGs. If a tool needs hex, sample directly from the PNGs at 100% zoom rather than approximating — they're the source of truth.

## Files
- `Alongside App Icon.dc.html` — live HTML design reference (open in a browser to see the full composition, including the adaptive-mask preview).
- `assets/` — the three exported PNGs described above.
