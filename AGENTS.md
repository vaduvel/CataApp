# AGENTS.md — instrucțiuni pentru agentul care lucrează în acest repo

Sursa de adevăr pentru **ce** se construiește este [`SPEC.md`](SPEC.md). Citește-l înainte de orice modificare. Acest fișier spune **cum** se lucrează.

## Comenzi

```bash
./gradlew :app:assembleDebug          # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest      # logica pură (bani, text factură, reguli)
./gradlew :app:connectedDebugAndroidTest   # doar cu telefon sau emulator conectat (DbTest)
./gradlew :app:lintDebug
bash tools/check-no-internet.sh       # obligatoriu după build
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Cerințe: **JDK 17 sau 21**, **Android SDK 35**.

Nu schimba versiunea de JDK a mașinii ca să „respecți specul”. Bytecode-ul rămâne 17 în ambele cazuri, fixat prin `compileOptions` și `jvmTarget` din `app/build.gradle.kts`. Build-ul e verificat pe JDK 21. Nu adăuga `jvmToolchain(17)`: ar impune instalarea unui JDK 17 fără niciun câștig.

Wrapper-ul Gradle 8.9 **este comis**. Folosește `./gradlew`, niciodată `gradle` direct. Dacă lipsește după un clone incomplet, regenerează-l o singură dată cu `gradle wrapper --gradle-version 8.9`.

## Reguli care nu se încalcă niciodată

1. **Fără rețea.** Nu adăuga permisiuni de rețea (INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE). Dacă o funcție are nevoie de internet, nu intră în aplicație. Verifică cu `tools/check-no-internet.sh`.
2. **Banii se țin în cenți**, ca `Long`. Niciodată `Double`/`Float` pentru sume. Cantitățile de măsurători sunt `Double`.
3. **Aplicația nu emite facturi.** Produce evidența și *textul* care ajunge pe factură. Zero XML, zero SdI, zero TVA, zero date fiscale ale clientului.
4. **„Facturat” și „încasat” sunt două cifre separate.** Nu presupune niciodată că sunt egale (vezi SPEC §5.2).
5. **Limbi:** cod și identificatori în engleză; textele din interfață în română, **doar** în `strings.xml`, niciodată scrise direct în composable-uri; textele generate pentru client sau contabil în italiană, doar în `domain/Descrizione.kt` și `domain/Dictionary.kt`.
	- Ghilimelele românești „…” se scriu doar în `strings.xml`. Într-un literal Kotlin, un `"` ASCII folosit ca ghilimea de închidere termină string-ul mai devreme și produce erori derutante de tip `Unresolved reference`. Diacriticele nu sunt problema, ghilimelele sunt. Dacă ai nevoie de ghilimele în cod, folosește `\"`.
	- Excepție: numele șabloanelor și ale etapelor din `domain/Templates.kt` sunt **date**, nu texte de interfață. Ele sunt și cheile din dicționarul RO → IT, deci rămân în cod și se scriu exact ca în SPEC §14.
6. **Logica de business stă în `domain/`**, pură, testabilă fără Android. Zero logică de calcul în composable-uri.
7. Fără `!!`, fără `GlobalScope`, fără operații de bază de date pe main thread.
8. Nicio dependență nouă fără justificare. Dependențele se declară **doar** în `gradle/libs.versions.toml`.

## Baza de date

- Room cu `exportSchema = true`. Schema se scrie în `app/schemas/` **abia după un build reușit** și trebuie comisă: e singurul mod în care migrările viitoare pot fi verificate.
- Fără `fallbackToDestructiveMigration`. Datele lui nu se șterg niciodată automat; la schimbarea schemei se scrie migrare.
- Enum-ul numit `Unit` în SPEC §4 se numește `MeasureUnit` în cod. Motivul e tehnic: un tip propriu numit `Unit` umbrește `kotlin.Unit` în orice fișier care îl importă, iar atunci orice lambda scrisă ca `() -> Unit` nu mai compilează. Restul modelului urmează specul litera cu literă.
- Datele demo (`domain/Seed.kt`) se scriu o singură dată, doar dacă tabela `clients` e goală. Cifrele sunt cele din scenariul de referință SPEC §10 și sunt folosite de testul golden din M6, deci nu se schimbă fără să se schimbe și specul.

## Definition of done pentru orice milestone

`./gradlew :app:testDebugUnitTest :app:assembleDebug` trece, `bash tools/check-no-internet.sh` trece, și criteriile din SPEC §10 pentru milestone-ul respectiv sunt bifate.

Commit-uri: Conventional Commits, ex. `feat(money): rest de incasat pe lucrare`.

## Stadiu

- **M0 — gata, build verificat local:** schelet Gradle, temă, navigare cu 5 ecrane, script de verificare offline.
- **M1 — gata:** modelul Room complet din SPEC §4 (12 entități, convertoare, DAO-uri), `domain/Seed.kt` cu lucrarea demo, `domain/Templates.kt` cu șabloanele din SPEC §14, ecranele Lucrări (listă, căutare după stradă, filtre, lucrare nouă în 4 câmpuri), Detaliu lucrare (status, etape, sunat, hartă, ștergere) și Clienți. `DbTest` acoperă seed-ul, căutarea și ștergerea în cascadă.
- **Urmează M2:** etape bifabile cu șabloane, „Am lucrat azi aici” în 2 apăsări, bară de avansare, estimat vs. real.
