# HentaiVNReal for Mihon

Unofficial Vietnamese-language Mihon source for https://hentaivnreal.com/.

## Build status

The repository contains an initial Kotlin source implementation and a GitHub Actions workflow. The workflow checks out the current Keiyoushi extension build system, overlays this module, and attempts to build a release APK.

**This extension is experimental.** The site's listing page has been inspected, but the manga detail and chapter-reader HTML could not be reliably inspected through the available public page fetch. The selectors in the parser therefore still require real-device validation and may need adjustment. Do not treat an uploaded build artifact as proof that all reading functions work.

## Build

Open the **Actions** tab and run **Build HentaiVNReal extension**, or push a change under `src/`. A successful run uploads an APK artifact named `hentaivnreal-mihon-apk`.

## Mihon repository

No Mihon repository index is published yet. An index must be generated from a successfully built APK before there is a valid URL to add in Mihon.

Unofficial project; not affiliated with HentaiVNReal or Mihon.
