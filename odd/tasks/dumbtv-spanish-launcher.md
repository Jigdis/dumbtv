# dumbtv Spanish launcher interface
Branch: `codex/dumbtv-spanish-visuals` · Delivery: ask-on-risk · Test runner: `launcher/build.sh` and launcher host-side tests (CI workflow `validate.yml`).
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
- Acceptance: directional navigation remains predictable; animations are lightweight and bounded; no new runtime dependencies, internet access, or additional runtime permissions.
- Checks: launcher build, host-side launcher test, static review of animation/allocation paths.

### S3 — Defer device Settings replacement
- User wording: “ok, entonces dejemos lo del menú de momento.”
- Acceptance: do not implement a replacement for the device-wide Settings menu in this feature.

### S4 — Refresh the launcher home visuals
- Authorized scope: implement a console-inspired visual refresh of DumbTV Home only; keep the full device Settings replacement deferred and do not change launcher interception/recovery behavior.
- User wording: “implementa la parte grafica ahora si, pero sin meter de momento despliegue a la tv. yo te aviso en que momento”
- Acceptance: make a substantial home-screen composition change based on PS5's actual home structure, not a banner-only addition; use existing Android Views and already-loaded app/input metadata; preserve D-pad navigation, launch actions, saved preferences, API 25 compatibility, and lightweight runtime behavior. TV deployment requires the user's authorization, which was later granted.
- Checks: `launcher/build.sh`, host-side launcher tests, `git diff --check`, source review of focus/navigation/performance paths, and same-signature TV runtime confirmation (authorized by the user).

### S5 — Remove the rejected dock and use official PS5 interface structure
- User-authorized revision: remove the unsuccessful dock and use official PS5 interface structure as the visual reference for future home-screen work.
- User wording:
  > “quita el dock, no lo necesito ahi. no me funciona.
  >
  > Solo quiero que te bases en la interfaz grafica de ps5, revisa como esta construida realmente”
- Acceptance: no dock panel, dock actions, dock-specific HOME-origin tracking, or dock-only permission remains. Preserve the established HOME-to-launcher recovery behavior and the S4 Spanish home visuals. Adapt PS5 principles rather than copying proprietary assets.
- Checks: API 25 compile/build, host-side launcher test, `git diff --check`, static check that dock-only code/permission is absent, same-signature TV update, and confirmation that the launcher UI no longer displays the dock.
### S6 — Redesign the home composition beyond the banner
- User wording: “la intefarz de ps5 no esta para nada lograda, lo lo veo extamente igual dumbtv que la version original solo porque agregaste un banner”
- Acceptance: change the overall home composition and hierarchy so the result is recognizably a restrained PS5-inspired interface, while keeping the dock removed and DumbTV lightweight; do not treat adding or enlarging a banner as sufficient.
- Checks: API 25 build, host tests, `git diff --check`, same-signature TV update, and visual confirmation on the TV.
## Tasks
- T1 — S1-S3; inline; complete for authorized local scope; commit: `de4e956`.
- T2 — S4, S6; inline; complete; work-unit commit: `c38c0aa`; replace the banner-only composition with a substantial PS5-informed home redesign.
  Risk: item 6 (interactive Android View behavior has no automated device-side regression test).
- T3 — S5; inline; complete; commit: `77ef2d7`; remove dock and dock-only permission/tracking while preserving the preexisting HOME recovery route; same-signature TV update verified.
  Risk: items 2 and 6 (remove the dock-only permission and verify the interactive launcher on device).

## Log
- L26 — User said the PS5 interface still looks like original DumbTV because only a banner was added: “la intefarz de ps5 no esta para nada lograda, lo lo veo extamente igual dumbtv que la version original solo porque agregaste un banner”. Reopen the home visual task and redesign the overall composition; the dock remains rejected.
- L27 — Rebuilt with the retained release keystore and verified the APK signer matched the installed app; installed via Windows ADB because WSL ADB had a separate unauthorized key. Same-signature `adb install -r` succeeded, firstInstallTime remained 2026-10-22 11:46:57, and visual inspection showed the new app rail/detail composition with selected artwork changing from Disney+ to Aptoide TV after DPAD_RIGHT. Parent API-25 build, FocusColorTest, and diff check passed.
- L28 — Work-unit commit `c38c0aa` assessed against `77ef2d7`: medium risk, `review_due=false`, `under_budget`; no native review transaction was required. No push or PR was created.
- L25 — Removed the rejected dock and deployed the no-dock build with the same signature while preserving app data; independent verification passed host tests and strict API-25 compilation. Parent WSL build, host test, manifest/dock-reference checks, same-signature update, and no-dock UI inspection passed. Official PS5 UX research completed; see L24.
- L18 — User clarified the new dock should open “cuando ya estemos en dumbtv”.
- L19 — Independent verification found permission-free wording stale after the narrowly scoped normal `KILL_BACKGROUND_PROCESSES` declaration. Updated root/launcher README, manifest comment, and build script description to document the permission and accurately state that cleanup is best-effort and does not clear all caches.
- L20 — API-25 `launcher/build.sh` passed in WSL using a disposable test keystore; host `FocusColorTest`, manifest permission assertions, `bash -n launcher/build.sh`, and `git diff --check` passed. Independent verification passed S1-S5 for static/host scope, including the foreground-only dock route, recovery-race fallback, and documentation; no TV runtime test or deployment was performed.
- L21 — User reported the deployed UI “Nop, ni se parece en nada a lo de ps5, ni trae el dock” and clarified the intended dock with `https://i.pinimg.com/236x/09/44/42/094442f911d709cfb0a182dc54d4aa2c.jpg` (“este es el dock”).
- L22 — Same-signature update had been installed, but TV testing did not show dock activation. The bounded log confirms Amazon `HomeActivity_vNext` followed by Guard recovery and DumbTV Home resuming, without a logged `KEYCODE_HOME` or `quick-dock` route. This synthetic ADB HOME event does not prove how the physical remote event is delivered. T3 is reopened; maintain recovery and distinguish a return from DumbTV from a return from another app.
- L23 — User rejected the dock because it was not useful and did not work: “quita el dock, no lo necesito ahi. no me funciona.\n\nSolo quiero que te bases en la interfaz grafica de ps5, revisa como esta construida realmente”. S5 is revised to remove the dock completely, retain established HOME recovery, and use official PS5 UX research as the basis for future graphical changes.
- L24 — Official PlayStation materials describe separate Games/Media homes, selected-item hubs, and a contextual Control Center overlay; Sony documents user-configurable controls and Reduce Motion, but publishes no launcher rendering, animation-budget, or performance engineering specification. Translate the structure and focus/navigation principles, not PS branding/assets or game-specific activity cards. Sources: [PS5 Quick Start Guide](https://www.playstation.com/content/dam/global_pdc/en/corporate/support/manuals/ps5-docs/7100/CFI-71XX_PS5_Quick_Start_Guide_Web%24en.pdf), [Control Center](https://www.playstation.com/en-us/support/games/customize-ps5-control-center/), [PS5 features](https://www.playstation.com/en-us/ps5/ps5-features/), [Accessibility settings](https://www.playstation.com/en-sg/support/hardware/ps5-accessibility-settings/).
- L14 — User request: “implementa la parte grafica ahora si, pero sin meter de momento despliegue a la tv. yo te aviso en que momento”
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
- L6 — Implemented the initial Spanish UI pass: home/options labels and dialogs are Spanish, section hierarchy is larger, and focused tiles lift subtly. The focus test first failed on the untranslated background choice, then passed after localization. Before the user authorized installation, all launcher Java sources compiled against Android 35 and the host test passed; no TV install/runtime test was run.
- L7 — Verification: `git diff --check` passed. Runtime harness is N/A because installing on the TV was not authorized. Rollback boundary: revert the single launcher UI localization/polish work-unit commit affecting launcher home, options, input status, and its host test.
- L8 — Risk: item 6 (interactive Android View behavior and appearance have no runnable device-side regression test).
- L9 — Work-unit commit: `de4e956` (`feat(launcher): localize and polish Spanish home UI`). Native assessment: medium, `review_due=false`, `under_budget`. Independent verification passed S1-S3 for static/host evidence; no change-caused blocker. Parent spot check `git diff --check 7a03e18..de4e956` passed. Verifier scratch remained outside the repository because host policy refused its removal.
- L10 — The user conditionally authorized installing the required SDK if absent. Verified the configured Android SDK lacked `platforms;android-25`, then installed that package with the Android CLI.
- L11 — Built a signed API-25/target-28 APK with the installed Windows SDK tools and JDK 17; Java 8-compatible compilation with `-Werror`, D8, zipalign, and apksigner verification all passed. Artifact: `launcher/build/dumbtv-home.apk` (53,847 bytes). Manifest permissions remain the original Fire OS 6-only three, each capped at API 27. Host `FocusColorTest` also passes against android-25.
- L12 — The repository's bash `launcher/build.sh` still cannot invoke this Windows SDK's `d8.bat` (it expects `$BT/d8`); the equivalent steps were run directly using the installed Windows executables. The APK was not installed on the Hisense because that separate device operation has not been authorized.
- L13 — The local task is complete for the authorized scope; physical-TV appearance/navigation remains an optional follow-up requiring explicit authorization. The host blocked deletion of external temp scratch directories, which remain outside the repository.
- L15 — Added a non-focusable featured panel to Home.java. It updates with focused apps/inputs, reuses a Drawable.ConstantState copy of already-loaded artwork, and shows a Spanish empty-selection state without changing navigation, launch handling, saved config, or HOME recovery.
- L16 — Independent verifier passed S1-S4 for authorized static/host scope and found no blockers; it identified that the hero accent did not refresh after changing the accent preference. The implementation now refreshes that stripe from apply(); parent API-25 compile, FocusColorTest, and git diff --check all passed after the correction.
- L17 — WSL `launcher/build.sh` could not find Android API 25 in WSL. Windows Android 25 source compile, D8, temporary APK assembly/signature verification with an ephemeral test key, and minSdk 25 check passed in external scratch. The stored release keystore password was not guessed or retried; no device signing or TV deployment was performed.
