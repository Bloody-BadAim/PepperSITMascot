# Animaties

## Tablet
Intro glow pulse 1400ms. Missie-overgang fade en slide 260ms. Correcte touch geeft een groene scan 320ms. Foute touch geeft een rode randpuls 220ms. Resultaat toont drie korte confetti-streaks van 600ms. State blijft altijd leesbaar zonder beweging.

## Pepper
Start speelt `welcome.qianim`. Tijdens touch blijft Pepper stil. Correcte complete combo speelt `hyped.qianim`. Resultaat speelt `dance_long.qianim` of een kortere fallback. LookAt duurt maximaal vier seconden en gebruikt `HEAD_ONLY`.

## Reduced motion
Android animator scale nul mag de tabletflow niet blokkeren. Geen logica wacht op een UI-animatie.
