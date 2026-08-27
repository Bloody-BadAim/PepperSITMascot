# Testrapport

Datum: 2026-08-27 07:59
Buildroute: `tools/windows-gradle.cmd` met Android Studio JBR 21 en Gradle 9.3.1
Commando: `clean lintDebug testDebugUnitTest assembleDebug`

## Geautomatiseerd bewijs

| Check | Resultaat | Bewijs |
|---|---|---|
| Lint | 0 errors, 0 warnings | `app/build/reports/lint-results-debug.xml` bevat 0 issue-elementen |
| Types en compilatie | PASS | `BUILD SUCCESSFUL`, Kotlin compileert tegen QiSDK 1.7.5 |
| Unit tests | 8 tests, 0 failures, 0 errors | `app/build/test-results/testDebugUnitTest/TEST-nl.svsit.pepperquest.QuestEngineTest.xml` |
| Build en packaging | PASS | `app/build/outputs/apk/debug/app-debug.apk` |
| APK grootte | 79.455.959 bytes | universele debug-APK met QiSDK native libs |
| APK SHA-256 | `a39db3ba24d92d42b6708bbbbff4aea63f55f2df4722f0ccf827ccdce920d8cd` | verse hash na de laatste clean build |
| Verboden API-scan | 0 treffers | geen GoTo, camera, ApproachHuman, HTTP, opslag, ruwe threads of brede listener-cleanup in `app/src` |
| Permissies in de echte APK | alleen `com.aldebaran.permission.ROBOT` en de Android dynamic-receiver permissie | `aapt dump badging` op de gebouwde APK |
| Netwerkrecht | afwezig | `android.permission.INTERNET` uit de QiSDK-merge verwijderd, niet aanwezig in samengevoegd manifest of APK |
| Exporteerbare componenten | 1 | alleen `nl.svsit.pepperquest.MainActivity` |
| Android 6 compatibiliteit | PASS | `sdkVersion:'23'` in de gebouwde APK |
| Backup en data-extractie | uitgeschakeld | `allowBackup=false` plus expliciete exclude-regels |

## Testdekking van de veiligheidslogica

| Testnaam | Wat het bewijst |
|---|---|
| `startOpensTouchMissionAtHeadStep` | quest start bij de hoofd-stap |
| `correctComboAdvancesFromHeadToLeftHandToRightHand` | volledige combo opent het voice-kanaal |
| `wrongTouchReportsExpectedZoneWithoutErasingProgress` | fout aanraken wist geen voortgang |
| `voiceChoiceSelectsTheRequestedTechClass` | class-keuze wordt correct opgeslagen |
| `finishReturnsResultForTheChosenClass` | resultaat hoort bij de gekozen class |
| `resetReturnsToIdleAndClearsQuestProgress` | reset wist state en voortgang |
| `emergencyStopLocksEveryInputUntilExplicitReset` | STOP negeert start, touch en class-keuze |
| `resetReleasesSafetyStopButDoesNotStartRobotWork` | alleen RESET geeft een nieuwe ronde vrij |

## Herstelde reviewbevindingen

1. STOP is nu een vergrendeling in zowel `QuestEngine` als `PepperController`, en blijft actief over focusverlies heen
2. Een afgewezen QiSDK Future wordt geannuleerd en met een begrensde wachttijd afgerond voordat de seriële lane verder gaat
3. De Listen-timeout gebruikt `AtomicBoolean` en levert precies één uitkomst
4. Listen gebruikt dezelfde Nederlandse locale als Say, met body language uit
5. Een tweede class-tik kan de lopende resultaat-spraak niet meer annuleren
6. Statusupdates uit robot-lifecycle callbacks lopen altijd via de main thread
7. Het samengevoegde manifest bevat geen INTERNET en geen extra exporteerbare receiver

## Niet uitgevoerd in deze omgeving

- Fysieke robotcheck van de drie touchsensoren, Nederlandse TTS, lokale spraakherkenning, annuleertiming en bewegingsruimte
- Visuele en end-to-end verificatie op het echte 1280x800 Pepper-tablet
- Er is geen Android-emulator image en geen aangesloten toestel op deze machine

Deze punten staan als expliciete blocker in `specs/pipeline.json` en als smokecheck in `docs/PEPPER-INSTALL.md`.
