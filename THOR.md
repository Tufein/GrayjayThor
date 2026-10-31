# Grayjay Thor — onofficiële dual-screenversie

Deze aangepaste Android-app is gebouwd op de echte Grayjay-broncode van
<https://gitlab.futo.org/videostreaming/grayjay>, commit
`c3ac3017382e9829b25916378d951463e5a93a36` (upstreamversie 396).
Dit is een onofficiële aanpassing voor persoonlijk gebruik, geen release van FUTO.
De oorspronkelijke [Source First License 1.1](LICENSE.md), auteursvermeldingen en
betaalfunctionaliteit blijven van toepassing.

## Gebruik op de AYN Thor

1. Zet beide schermen aan in de Thor-instellingen en gebruik Android als
   besturingssysteem; voor deze app zijn geen rootrechten nodig.
2. Installeer de **arm64-v8a**-APK. Android kan vragen om installatie van apps uit
   deze bron toe te staan.
3. Open **Grayjay Thor (unofficial)** via het app-pictogram. De launcher opent de
   volledige Grayjay-interface op het kleinere tweede scherm. Het hoofdscherm
   toont de video zodra je een video kiest.
4. Configureer videobronnen via de gewone Grayjay-bronnenpagina. De Thor-variant
   bundelt dezelfde bronplugins als de stable-variant; de beschikbaarheid van
   diensten blijft afhankelijk van de betreffende plugin en dienst.
5. Onder **Instellingen → Thor: twee schermen** kun je beide schermen aan- of
   uitzetten, de schermen omwisselen en controllerbediening uitschakelen.
   Open de app opnieuw via het app-pictogram na het omwisselen.

De video en ondertiteling staan op het videoscherm. Zoeken, afspeellijsten,
reacties, bronkeuze en de bestaande Grayjay-afspeelbediening staan op het
bedieningsscherm. Beide gebruiken **dezelfde ExoPlayer**. Er wordt geen tweede
stream gestart en geen beeld via het netwerk gecast.
De afzonderlijke Shorts-speler en casting blijven hun bestaande Grayjay-gedrag
gebruiken; de schermscheiding is toegevoegd aan de normale videospeler.

De normale A-knop en D-pad blijven beschikbaar voor navigatie. Tijdens het
afspelen werken deze extra knoppen op beide schermen:

| Knop | Actie |
| --- | --- |
| START | Afspelen / pauzeren |
| L1 | 10 seconden terug, als de stream zoeken ondersteunt |
| R1 | 10 seconden vooruit, als de stream zoeken ondersteunt |
| Terug op het videoscherm | Bedieningsscherm naar voren brengen |

Met één beschikbaar scherm, of wanneer Android starten op een tweede scherm
weigert, werkt de bestaande Grayjay-speler op één scherm. Als het videoscherm
verdwijnt, wordt het beeld teruggegeven aan de normale speler.

Het pakket heet `com.futo.platformplayer.thor` en staat naast gewone Grayjay.
Instellingen en accounts worden niet automatisch uit de gewone app overgenomen;
gebruik desgewenst de bestaande Grayjay-export/importfunctie.
App-updates worden voor deze variant handmatig geïnstalleerd, zodat de officiële
updater deze aanpassing niet probeert te vervangen. Pluginupdates blijven actief.
Bewaar voor vervolgbouw dezelfde ondertekeningssleutel als je updates wilt
installeren zonder de app eerst te verwijderen. De meegeleverde debug-APK is
bedoeld als eerste testversie.

## Zelf bouwen

Haal deze repository op:

```sh
git clone https://github.com/Tufein/GrayjayThor.git
cd GrayjayThor
```

De submodules verwijzen rechtstreeks naar GitLab. De Thor-versiecode is
vastgelegd op 396, zodat bronplugins dezelfde API-versie zien, onafhankelijk
van de tags en geschiedenis van deze GitHub-repository.

Als alternatief kun je de losse patch op de oorspronkelijke bouwbron toepassen:

```sh
git clone https://gitlab.futo.org/videostreaming/grayjay.git grayjay-thor
cd grayjay-thor
git checkout c3ac3017382e9829b25916378d951463e5a93a36
git apply /pad/naar/grayjay-thor-c3ac301.patch
```

Benodigd: JDK 17 of hoger, Git LFS, Android SDK-platform 36, Android build-tools en
`ANDROID_HOME` ingesteld op de SDK-map. De wrapper gebruikt Gradle 8.13.

```sh
export ANDROID_HOME=/pad/naar/android-sdk
./scripts/build-thor.sh
```

Het script initialiseert de vastgepinde GitLab-submodules, bouwt de APK en draait
de Thor-unit-tests. De APK's staan onder `app/build/outputs/apk/thor/debug/`.
Gebruik de `arm64-v8a`-APK voor de Thor. De `x86_64`-APK is voor een emulator.
Een releasebuild vereist je eigen ondertekeningsconfiguratie zoals bij upstream.

Handmatig installeren via USB:

```sh
adb install -r app/build/outputs/apk/thor/debug/app-thor-arm64-v8a-debug.apk
```

De Android-instrumentatietests controleren ook het verplaatsen en herstellen van
het videobeeld met een echte Media3-speler, met behoud van afspeelpositie, volume
en de callbacks van Grayjay:

```sh
./gradlew :app:connectedThorDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.futo.platformplayer.thor.ThorPlayerOutputTest
```

## Testen van twee schermen

Schermselectie wordt getest met niet-opeenvolgende scherm-id's, één scherm,
ontbrekende of ongeldige schermen, omwisselen, extra monitoren en grote
schermresoluties. De scherm-id van het onderste scherm wordt nergens als `1`
vastgelegd.

Voor een Android-emulator kun je een openbaar tweede scherm simuleren:

```sh
adb shell settings put global overlay_display_devices '1080x1240/320'
adb shell am start -n com.futo.platformplayer.thor/com.futo.platformplayer.thor.ThorLauncherActivity
adb shell dumpsys activity activities
```

Controleer vóór dagelijks gebruik op een echte Thor: schermindeling, afspelen en
zoeken, audio en ondertiteling, controllerknoppen, schermen omwisselen, sluiten en
opnieuw openen en gedrag bij het uitzetten van een scherm. Een emulator bewijst
geen compatibiliteit met de Thor-firmware.
