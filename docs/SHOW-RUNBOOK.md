# HvA Experience show-runbook

## Voor opening

- Zet Pepper op de vloer met de rem- en veiligheidsprocedure van de locatie
- Houd looppaden en de ruimte rond de armen vrij
- Sluit Pepper en tablet aan op voeding
- Start `{SIT} QUEST`
- Controleer `ROBOT ONLINE`; bij `PREVIEW MODE` blijft de volledige tabletflow beschikbaar
- Doorloop één volledige testronde
- Test alle drie sensoren en de vier class-chips
- Test `STOP` tijdens Listen en tijdens een gebaar
- Druk `RESET` en laat het scherm op standby staan

## Bezoekersflow

1. Begeleider zegt: “Druk op start en volg het scherm”
2. Bezoeker drukt `START QUEST`
3. Bezoeker volgt hoofd, linkerhand, rechterhand
4. Bezoeker zegt of tikt een tech-class
5. Bezoeker leest de uitslag
6. Begeleider drukt `RESET` als de bezoeker dit niet zelf doet

Doeltijd is 60 tot 90 seconden. Sla robotstappen over als een capability niet beschikbaar is. De tabletknoppen zijn de officiële fallback, niet een foutmodus.

## STOP

Druk direct op de rode `STOP` bij:

- onverwachte arm- of hoofdbeweging
- een bezoeker die te dichtbij komt
- spraak of een animatie die niet eindigt
- verlies van toezicht

`STOP` annuleert de actieve QiSDK-actie en vergrendelt daarna alle invoer. Het scherm toont `ROBOT LOCKED`. Touch, stem en tabletknoppen doen niets meer, ook niet na focusverlies of het opnieuw vinden van de robot. Alleen `RESET` geeft een nieuwe ronde vrij. Als Pepper niet rustig is, sluit de app en volg de fysieke veiligheidsprocedure van Pepper en de locatie.

## Snelle storingspaden

### Status blijft PREVIEW MODE

1. Laat bezoekers de tablet fallback gebruiken
2. Controleer of de app op Pepper zelf draait
3. Sluit de app en start hem opnieuw
4. Controleer QiSDK-focus
5. Herinstalleer alleen buiten de bezoekersrij

### Eén touchsensor reageert niet

1. Lees de sensorstatus bovenin
2. Gebruik de gelijknamige tabletknop
3. Druk `RESET`
4. Herstart de app bij een rustig moment
5. Noteer welke exacte sensor faalde

### Voice verstaat niets

1. Wacht op de timeout van circa negen seconden
2. Gebruik een zichtbare class-chip
3. Zet achtergrondgeluid lager als dat kan
4. Voice is nooit vereist om de ronde af te maken

### Scherm reageert niet

1. Druk `STOP`
2. Sluit de app via ADB of Android
3. Start `nl.svsit.pepperquest/.MainActivity` opnieuw
4. Gebruik de laatst werkende APK als herstart niet helpt

## Wissel en afsluiting

- Druk tussen bezoekers op `RESET`
- Laat nooit een Listen-actie onbeheerd actief
- Druk bij pauze eerst `STOP`
- Sluit de app aan het einde van de show
- Er is geen export of dataverwijdering nodig; de app bewaart niets
