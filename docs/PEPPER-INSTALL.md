# Pepper installatiekaart

## Benodigd

- Windows-laptop met Android Platform Tools
- Pepper en laptop op hetzelfde lokale netwerk
- Pepper Developer Mode aan
- Een geslaagde lokale debug-build

De app heeft na de build geen internet nodig. Er worden geen audio, beelden of bezoekersgegevens opgeslagen.

## 1 Build de APK

Open PowerShell:

```powershell
cd C:\Users\matin\Desktop\HvA\SIT\PepperSITMascot
.\gradlew.bat clean testDebugUnitTest assembleDebug
```

Verwacht bestand:

```text
C:\Users\matin\Desktop\HvA\SIT\PepperSITMascot\app\build\outputs\apk\debug\app-debug.apk
```

## 2 Verbind ADB

Lees het IP-adres op Pepper en vervang `PEPPER_IP` hieronder. QiSDK Pepper gebruikt normaal ADB-poort 5555.

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb kill-server
& $adb start-server
& $adb connect PEPPER_IP:5555
& $adb devices
```

Ga alleen verder als `adb devices` één regel met `PEPPER_IP:5555 device` toont. Accepteer een eventuele debugmelding op Pepper.

## 3 Installeer

```powershell
$apk = "C:\Users\matin\Desktop\HvA\SIT\PepperSITMascot\app\build\outputs\apk\debug\app-debug.apk"
& $adb install -r $apk
```

Een schone herinstallatie, alleen als `install -r` faalt:

```powershell
& $adb uninstall nl.svsit.pepperquest
& $adb install $apk
```

## 4 Start

```powershell
& $adb shell am force-stop nl.svsit.pepperquest
& $adb shell am start -n nl.svsit.pepperquest/.MainActivity
```

## 5 Smokecheck

1. Controleer dat `{SIT} QUEST` fullscreen in landscape staat
2. Controleer dat `RESET` en de rode `STOP` bereikbaar zijn
3. De status toont `ROBOT ONLINE`; `PREVIEW MODE` is een geldige tablet fallback
4. Doorloop start, hoofd, linkerhand, rechterhand en één class-chip
5. Controleer de resultaatkaart met `€10 / jaar`, `50+ events` en `door studenten voor studenten`
6. Start opnieuw en controleer fysieke `Head/Touch`, `LHand/Touch` en `RHand/Touch`
7. Start voice en wacht zonder te spreken; na circa negen seconden blijven de class-chips bedienbaar
8. Druk tijdens spraak of animatie op `STOP`; de actie stopt, het scherm toont `ROBOT LOCKED` en touch, stem en chips doen niets meer
9. Druk `RESET`; pas daarna start een nieuwe ronde weer
10. Pepper mag niet rijden of zijn base draaien

## Rollback

Bewaar de laatst werkende APK als `app-debug-last-known-good.apk`. Installeer deze terug met:

```powershell
& $adb install -r "C:\pad\naar\app-debug-last-known-good.apk"
& $adb shell am force-stop nl.svsit.pepperquest
& $adb shell am start -n nl.svsit.pepperquest/.MainActivity
```

Volledige verwijdering:

```powershell
& $adb uninstall nl.svsit.pepperquest
```

Een rollback verandert geen bezoekersdata, want de app schrijft geen data weg.
