# Requirements

## Must haves

### R001 Start en reset
Als bezoeker kan ik de quest met één grote tabletknop starten en na afloop of een fout opnieuw beginnen.

### R002 Touch-missie
De quest gebruikt `Head/Touch`, `LHand/Touch` en `RHand/Touch` in een zichtbare combo. Verkeerde invoer geeft vriendelijke feedback en wist de voortgang niet volledig.

### R003 Voice-missie
Pepper spreekt Nederlands en luistert lokaal met een vaste PhraseSet naar minimaal vier tech-classes. De gekozen class verandert de eindboodschap.

### R004 Gaze en beweging
Pepper zoekt een mens via HumanAwareness, kijkt maximaal kort met `HEAD_ONLY`, speelt een welkomstgebaar en viert een voltooide quest met een animatie.

### R005 SIT-tabletinterface
De interface gebruikt de SIT TERMINAL en GLITCH-designtaal, grote touchdoelen en live missievoortgang op Pepper-resolutie 1280 bij 800.

### R006 Veiligheid en privacy
Geen base motion, cloud, cameraopslag of netwerkverkeer. Robotacties zijn geserialiseerd. Luisteren wordt gestopt vóór Pepper praat. Een noodstop annuleert de actieve actie.

### R007 Degradatie
Zonder robotfocus blijft de tablet-preview bruikbaar. Voice kan via zichtbare keuzechips worden overgeslagen. Touch kan via tabletknoppen worden gesimuleerd voor demo en test.

### R008 Oplevering
Een debug APK, broncode, tests en een installatiekaart met Windows ADB-commando's worden geleverd.

## Should haves
R009 De eindkaart toont relevante SIT-feiten zoals `€10 per jaar`, `50+ events` en `door studenten voor studenten`.

R010 De quest duurt bij normaal gebruik 60 tot 90 seconden.

## Could haves
R011 Een begeleidersmodus toont sensordiagnostiek zonder de hoofdflow te verstoren.

## Won't haves
Geen backend, accounts, scoreopslag, vrije spraakherkenning, generatieve AI, gezichtsherkenning, fysieke LED-aansturing, camera, rijden of externe analytics.

## Kwaliteit
Android 6 API 23 blijft ondersteund. De UI heeft minimaal 56dp touchdoelen, duidelijke contrasten en tekstalternatieven naast kleur. Animaties zijn kort en niet nodig om state te begrijpen. Alle foutpaden komen terug in een bedienbare state.

## Security en privacy
Er is geen gebruikersinput die naar netwerk of opslag gaat. De app vraagt alleen de QiSDK-toegang die Pepper nodig heeft. Er zijn geen secrets. Exporteerbare Android-componenten zijn beperkt tot de launcher activity.

## Foutscenario's
Geen robotfocus toont previewmodus. Geen gedetecteerd mens slaat LookAt over. Niet verstaan toont class-keuzes. Ontbrekende touchsensor meldt dit en laat tabletsimulatie toe. Een geannuleerde animatie keert terug naar een veilige state.

## Succescriteria
1. `assembleDebug` eindigt met exitcode 0
2. Unit tests voor correcte combo, fout touch, voice-keuze, reset en finish zijn groen
3. Alle drie fysieke touchsensornamen zijn in code gebonden
4. De PhraseSet bevat minimaal vier keuzes
5. Statische inspectie vindt geen `GoToBuilder`, camera-builder, OkHttp of externe URL
6. Tablet-preview doorloopt start, drie touchstappen, class-keuze en eindkaart zonder robotfocus

## Clarifications
De app heet `{SIT} QUEST` en package `nl.svsit.pepperquest`. Nederlands is de primaire taal. Engelse techwoorden zijn geldige voice-antwoorden omdat deze goed herkenbaar en bekend zijn bij de doelgroep. De fysieke Pepper-installatie en sensortest gebeuren morgen door Matin en zijn daarom geen voorwaarde voor de lokale APK-build.
