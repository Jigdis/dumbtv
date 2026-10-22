# dumbtv Spanish launcher interface
Branch: `codex/dumbtv-spanish-launcher` · Delivery: ask-on-risk · Test runner: `launcher/build.sh` and launcher host-side tests (CI workflow `validate.yml`).
Objective: redesign dumbtv home as a Spanish-language, PS5-inspired launcher while keeping the app lightweight and preserving existing launcher behavior. Forecast: approximately 300 authored changed lines; one coherent work unit.

## Specs

### S1 — Spanish launcher UI
- Authorized scope: redesign/localize the dumbtv launcher interface; do not build the deferred device-wide Settings replacement.
- User wording: “rediseñar la interfaz, el idioma a español del launcher.”
- Acceptance: user-facing launcher and existing Options UI copy is professional Spanish; existing app/input discovery, navigation, actions, and stored config remain compatible.
- Checks: launcher build, host-side launcher test, UI-string/code inspection.

### S2 — PS5-inspired visual direction with dumbtv performance
- Authorized scope: apply a restrained console-inspired layout, focus treatment, and motion without copying proprietary assets or compromising responsiveness.
- User wording: “una interfaz como la de ps5 me encantaría” and “asegurandose que se mantenga optimizado al 100% como ya es dumbtv.”
- Acceptance: directional navigation remains predictable; animations are lightweight and bounded; no new runtime dependencies or internet/permission requirements.
- Checks: launcher build, host-side launcher test, static review of animation/allocation paths.

### S3 — Defer device Settings replacement
- User wording: “ok, entonces dejemos lo del menú de momento.”
- Acceptance: do not implement a replacement for the device-wide Settings menu in this feature.

## Tasks
- T1 — S1-S3; inline; partial pending full APK/device proof; commit: pending.

## Log
- L1 — User request: “hagamos un fork del repo de dumbtv para poder modificarlo a nuestro antojo, ya sabes buenas practicas y todo eso.

El punto es rediseñar la interfaz, el idioma a español del launcher.
El menú se settings, es lo que le ha faltado a los otros. porque va acorde a algo mas generico, pero aqui quiero rehacer todo el menu de opciones que tiene mi dispositivo al 100%, porque es algo personalizado a mi dispositivo.

asi que toca explorar todas las opciones que da el menu de configuración del dispositivo.

una interfaz como la de ps5 me encantaría. se puede?
asi que consulta como se construyo eso, algun concepto similar, animaciónes, sonidos, etc. Pero asegunrandose que se mantenga optimizado al 100% como ya es dumbtv.

Como ves? lo podemos lograr?”
- L2 — User narrowed scope: “ok, entonces dejemos lo del menú de momento.” Device-wide Settings replacement is deferred.
- L3 — GitHub CLI authentication succeeded for `Jigdis`; fork `https://github.com/Jigdis/dumbtv` is cloned locally with `origin` on the fork and `upstream` on the official repository.
- L4 — Source map: home and Options UI are programmatically rendered with Android Views; Tile is a custom Canvas-drawn view. Keep the existing no-Gradle/no-dependency build and Android API 25 compatibility.
- L5 — CodeGraph initialized and synced for this clone. RDD mode is enabled globally; assess the candidate after the work-unit commit.
- L6 — Implemented the initial Spanish UI pass: home/options labels and dialogs are Spanish, section hierarchy is larger, and focused tiles lift subtly. The focus test first failed on the untranslated background choice, then passed after localization. All launcher Java sources compile against the installed Android 35 jar and `FocusColorTest` passes; four existing deprecation warnings appear with that newer SDK. The normal launcher build remains unverified: Android 25 is not installed, and `MIN_SDK=35` trips existing `-Werror` deprecation warnings. No TV install/runtime test was run.
- L7 — Verification: `git diff --check` passed. Runtime harness is N/A because installing on the TV was not authorized. Rollback boundary: revert the single launcher UI localization/polish work-unit commit affecting launcher home, options, input status, and its host test.
- L8 — Risk: item 6 (interactive Android View behavior and appearance have no runnable device-side regression test).
