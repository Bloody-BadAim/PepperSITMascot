# Tasks

### T001 Projectidentiteit en merkresources
_Boundary: Gradle config, manifest, strings, fonts, logo en theme_
_Depends: none_
_Requirements: R005,R008_
- Tijd: 20 min
- Files: `settings.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/*`, `app/src/main/res/font/*`, `app/src/main/res/drawable/*`
- AC: app heet `{SIT} QUEST`, package is `nl.svsit.pepperquest`, fonts en SIT-kleuren zijn lokaal
- Status: done

### T002 QuestEngine met unit tests
_Boundary: pure Kotlin domeinstate en tests_
_Depends: T001_
_Requirements: R001,R002,R003,R007,R010_
- Tijd: 30 min
- Files: `app/src/main/java/nl/svsit/pepperquest/QuestEngine.kt`, `app/src/test/**`
- AC: tests bewijzen start, combo, fout touch, voicekeuze, reset en finish
- Status: done

### T003 Veilige QiSDK-adapter
_Boundary: PepperController en lifecycle wiring_
_Depends: T001,T002_
_Requirements: R002,R003,R004,R006,R007,R011_
- Tijd: 30 min
- Files: `app/src/main/java/nl/svsit/pepperquest/PepperController.kt`
- AC: drie sensoren, Nederlands Say, lokale PhraseSet, HEAD_ONLY LookAt, animatie en noodstop aanwezig zonder base motion
- Status: done

### T004 SIT tabletflow
_Boundary: activity, layout en drawables_
_Depends: T002,T003_
_Requirements: R001,R005,R007,R009,R010_
- Tijd: 30 min
- Files: `MainActivity.kt`, `activity_main.xml`, `res/drawable/*`
- AC: volledige flow is via tablet te doorlopen, state en fouten zijn zichtbaar, touchdoelen minimaal 56dp
- Status: done

### T005 Opleverdocumentatie
_Boundary: README en runbook_
_Depends: T004_
_Requirements: R008_
- Tijd: 20 min
- Files: `README.md`, `docs/PEPPER-INSTALL.md`, `docs/SHOW-RUNBOOK.md`
- AC: build, Windows ADB-connect, install, launch, smokecheck, noodstop en rollback staan exact beschreven
- Status: done

### T006 Build en statische veiligheidscheck
_Boundary: alleen bewijs en kleine bewezen fixes_
_Depends: T002,T003,T004,T005_
_Requirements: R006,R008_
- Tijd: 25 min
- Files: `specs/artifacts/*`, alleen benodigde fixes uit failure-output
- AC: unit tests en assembleDebug groen, APK-pad vastgelegd, verboden API-scan groen
- Status: done
