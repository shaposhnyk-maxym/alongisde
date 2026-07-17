# Bundled fonts

Font binaries live in `core/ui/src/commonMain/composeResources/font/` and are
distributed with the app. All three families are licensed under the
SIL Open Font License 1.1 — license texts are kept next to this file.

| Family | Role in the design system | Version / source |
|---|---|---|
| Lora | Serif: display, headlines, titles, brand italic | [cyrealtype/Lora-Cyrillic@master](https://github.com/cyrealtype/Lora-Cyrillic) `fonts/ttf/Lora-{Regular,Medium,SemiBold,Italic,MediumItalic,SemiBoldItalic}.ttf` |
| IBM Plex Mono | Mono: overline labels, diary text, digit/code tiles, meta | [IBM/plex release `@ibm/plex-mono@2.5.0`](https://github.com/IBM/plex/releases/tag/%40ibm%2Fplex-mono%402.5.0) `ibm-plex-mono.zip → fonts/complete/ttf/IBMPlexMono-{Regular,Medium}.ttf` |
| Inter | Sans: body copy, button labels, subtitles | [rsms/inter release v4.1](https://github.com/rsms/inter/releases/tag/v4.1) `Inter-4.1.zip → extras/ttf/Inter-{Regular,Medium,SemiBold}.ttf` (static, not variable) |
