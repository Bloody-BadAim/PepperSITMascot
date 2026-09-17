# {SIT} QUEST

Offline Pepper-ervaring voor de HvA Experience. Een bezoeker ontgrendelt Student Mode met een touch-combo en kiest daarna een tech-class met lokale spraakherkenning of tabletknoppen.

## Functies

- Nederlandse Pepper-spraak met een vaste, lokale PhraseSet
- `Head/Touch`, `LHand/Touch` en `RHand/Touch`
- Volledige tablet fallback zonder robotfocus
- `HEAD_ONLY` gaze, korte gebaren en één zichtbare STOP
- Geen netwerk, camera, opslag, accounts of base motion
- Android 6 en hoger, package `nl.svsit.pepperquest`

## Flow

1. Druk `START QUEST`
2. Raak hoofd, linkerhand en rechterhand aan, fysiek of op het tablet
3. Zeg of kies Software, Cyber Security, UX & Design of Games
4. Lees de persoonlijke class-uitkomst en de SIT-feiten
5. Druk `RESET` voor de volgende bezoeker

## Build

Open de map in Android Studio en gebruik de project-JDK. Of bouw vanuit Windows PowerShell:

```powershell
cd C:\Users\matin\Desktop\HvA\SIT\PepperSITMascot
.\gradlew.bat testDebugUnitTest assembleDebug
```

Debug APK:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Projectstructuur

- `QuestEngine.kt` bevat de pure, geteste queststate
- `PepperController.kt` bevat de focus-scoped QiSDK 1.7.5 adapter
- `MainActivity.kt` vertaalt tablet- en sensorevents naar dezelfde engine
- `docs/PEPPER-INSTALL.md` bevat installatie en rollback
- `docs/SHOW-RUNBOOK.md` bevat de eventchecklist en storingspaden

## Veiligheidscontract

Pepper rijdt nooit. Gaze gebruikt alleen `LookAtMovementPolicy.HEAD_ONLY` en stopt binnen vier seconden. Say, Listen, Animate en LookAt delen één seriële lane. Listen is asynchroon, heeft een timeout van negen seconden, levert precies één uitkomst en wordt voor iedere Say geannuleerd. Iedere touchlistener wordt met exact dezelfde listenerreferentie verwijderd.

`STOP` is een vergrendeling, geen pauze:

- de actieve QiSDK-actie wordt geannuleerd
- de quest gaat naar `SAFETY_STOP`, waarin start, touch en class-keuze worden genegeerd
- fysieke sensoren en tabletknoppen kunnen geen nieuwe robotactie starten
- de vergrendeling blijft actief over focusverlies en focusherstel heen
- alleen `RESET` geeft bewust een nieuwe ronde vrij

Een QiSDK-actie die precies tijdens `STOP`, reset of focuswissel start, wordt geannuleerd en met een begrensde wachttijd afgerond voordat nieuw werk in dezelfde lane mag lopen.

## Licentie

MIT, zie [LICENSE](LICENSE). Copyright 2026 Studievereniging ICT (SIT), Hogeschool van Amsterdam.
